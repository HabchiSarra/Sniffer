package fr.inria.tandoori.analysis.tools;

import fr.inria.tandoori.analysis.FilesUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract a repository concept to choose between cloning or using local path.
 */
public class Repository {
    private static final Logger logger = LoggerFactory.getLogger(Repository.class.getName());
    private String repository;
    private Path cloneDir;
    private boolean isRemote = false;

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
     * Initialize the repository, either by opening the local path or by cloning the remote repository from github.
     *
     * @return The created repository.
     * @throws RepositoryException If anything goes wrong
     */
    public Git initializeRepository() throws RepositoryException {
        if (cloneDir != null) {
            logger.warn("Repository already initialized, doing nothing (" + repository + ")");
        }
        if (Files.exists(Paths.get(this.repository))) {
            return initializeLocalRepository();
        }
        return initializeRemoteRepository();
    }

    /**
     * Remove all useless data from this repository.
     * i.e. clean the cloned directory for a remote repository.
     */
    public void finalizeRepository() {
        if (cloneDir == null) {
            logger.warn("Repository not initialized, doing nothing (" + repository + ")");
        }
        // We don't delete files if the repository was there before us.
        // We won't delete anything by default.
        if (isRemote) {
            FilesUtils.recursiveDeletion(cloneDir);
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
            this.cloneDir = Files.createTempDirectory("tandoori");
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
}
