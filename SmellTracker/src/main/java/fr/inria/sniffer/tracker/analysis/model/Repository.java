package fr.inria.sniffer.tracker.analysis.model;

import fr.inria.sniffer.tracker.analysis.FilesUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract a repository concept to choose between cloning or using local path.
 */
public class Repository {
    private static final Logger logger = LoggerFactory.getLogger(Repository.class.getName());
    private String repository;
    private Path cloneDir;
    private boolean isRemote = false;
    private Git git;

    /**
     * Initialize a new repository.
     * If the given path is a directory on the filesystem, we will use it.
     * If not, we will clone the repo from github in a temporary repository.
     * <p>
     * param repository Path to the local repository or Github identifier '$user/$project'
     */
    public Repository(String repository) {
        this.repository = repository;
    }


    /**
     * Return the current Git repository.
     *
     * @return The git repository.
     */
    public Git getGitRepository() {
        if (this.git == null) {
            logger.warn("You must call Repository#initializeRepository before calling this method");
            return null;
        }
        return this.git;
    }


    /**
     * Initialize the repository, either by opening the local path or by cloning the remote repository from github.
     *
     * @return The created repository.
     * @throws RepositoryException If anything goes wrong
     */
    public Git initializeRepository() throws RepositoryException {
        if (logger.isDebugEnabled() && cloneDir != null) {
            logger.debug("Repository already initialized, doing nothing (" + repository + ")");
        }
        if (Files.exists(Paths.get(this.repository))) {
            this.git = initializeLocalRepository();
        } else {
            this.git = initializeRemoteRepository();
        }
        return this.git;
    }

    /**
     * Remove all useless data from this repository.
     * i.e. clean the cloned directory for a remote repository.
     */
    public void finalizeRepository() {
        if (logger.isDebugEnabled() && cloneDir == null) {
            logger.debug("Repository not initialized, doing nothing (" + repository + ")");
        }
        // We don't delete files if the repository was there before us.
        // We won't delete anything by default.
        if (isRemote) {
            FilesUtils.recursiveDeletion(cloneDir);
        }
    }

    /**
     * Retrieve the commit identified by 'sha' on the {@link org.eclipse.jgit.api.Git} repository.
     * This {@link Commit} will be filled with its parents, but not its details (message, author, date).
     *
     * @param sha identifier of the commit to retrieve, might be a sha as well as 'HEAD'.
     * @return The retrieved {@link Commit}.
     * @throws IOException If anything goes wrong while parsing Git repository.
     */
    public Commit getCommitWithParents(String sha) throws IOException {
        ObjectId commitId = ObjectId.fromString(sha);
        return Commit.commitWithParents(getRevCommit(commitId));
    }

    /**
     * Retrieve the commit identified by 'sha' on the {@link org.eclipse.jgit.api.Git} repository.
     * This {@link Commit} will be filled with its details (message, author, date) but not with its parents.
     *
     * @param sha identifier of the commit to retrieve, might be a sha as well as 'HEAD'.
     * @return The retrieved {@link Commit}.
     * @throws IOException If anything goes wrong while parsing Git repository.
     */
    public Commit getCommitWithDetails(String sha) throws IOException {
        ObjectId commitId = ObjectId.fromString(sha);
        return Commit.commitWithDetails(getRevCommit(commitId));
    }

    /**
     * Retrieve the commit identified by 'sha' on the {@link org.eclipse.jgit.api.Git} repository.
     * We use the method {@link Commit#commitWithParents(RevCommit)} since this method is only used to parse the
     * commit tree.
     *
     * @return {@link Commit} of the repository HEAD.
     * @throws IOException If anything goes wrong while parsing Git repository.
     */
    public Commit getHead() throws IOException {
        org.eclipse.jgit.lib.Repository gitRepo = getGitRepository().getRepository();
        Ref head = gitRepo.findRef("HEAD");
        return Commit.commitWithParents(getRevCommit(head.getObjectId()));
    }

    /**
     * Returns the underlying repository's log.
     *
     * @return An iterable of commit SHA1s representing the git log.
     * @throws IOException If an exception occurred while retrieving git log.
     */
    public List<String> getLog() throws IOException {
        List<String> shas = new ArrayList<>();
        try {
            for (RevCommit commit : getGitRepository().log().call()) {
                shas.add(commit.name());
            }
            return shas;
        } catch (GitAPIException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the {@link RevCommit} from any object reference in the repository.
     *
     * @param commitId Identifier of the {@link RevCommit} to retrieve.
     * @return The {@link RevCommit}.
     * @throws IOException If anything goes wrong while parsing Git repository.
     */
    private RevCommit getRevCommit(ObjectId commitId) throws IOException {
        org.eclipse.jgit.lib.Repository gitRepo = getGitRepository().getRepository();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk walk = new RevWalk(gitRepo)) {
            RevCommit commit = walk.parseCommit(commitId);
            walk.dispose();
            return commit;
        }
    }

    /**
     * Return the path to the repository location on filesystem.
     * <p>
     * This will be null before calling {@link Repository#initializeRepository()}.
     *
     * @return java.nio.file.Path to the local repository directory.
     */
    public Path getRepoDir() {
        return this.cloneDir;
    }

    /**
     * Clone the repository from a remote location (github.com)
     *
     * @return The {@link Git} repository.
     * @throws RepositoryException If anything wrong occurs.
     */
    private Git initializeRemoteRepository() throws RepositoryException {
        isRemote = true;
        try {
            this.cloneDir = Files.createTempDirectory("tracker");
        } catch (IOException e) {
            throw new RepositoryException("Unable to create temporary directory", e);
        }
        try {
            return Git.cloneRepository()
                    .setDirectory(cloneDir.toFile())
                    .setURI("https://github.com/" + repository)
                    .call();
        } catch (GitAPIException e) {
            throw new RepositoryException("Unable to clone repository: " + repository, e);
        }
    }

    /**
     * Initialize a repository from the given filesystem path.
     *
     * @return The {@link Git} repository.
     * @throws RepositoryException If anything wrong occurs.
     */
    private Git initializeLocalRepository() throws RepositoryException {
        this.cloneDir = Paths.get(this.repository);
        try {
            return Git.open(cloneDir.toFile());
        } catch (IOException e) {
            throw new RepositoryException("Unable to open repository: " + repository, e);
        }
    }

    /**
     * Any {@link Exception} linked to the {@link Repository} class.
     */
    public static class RepositoryException extends Exception {
        private RepositoryException(String message, Exception exception) {
            super(message, exception);
        }
    }

    @Override
    public String toString() {
        return "Repository{" +
                "repository='" + repository + '\'' +
                '}';
    }
}
