package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.FilesUtils;
import fr.inria.tandoori.analysis.persistence.Persistence;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommitsQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    private int projectId;
    private String repository;
    private Persistence persistence;
    private final Path cloneDir;

    public CommitsQuery(int projectId, String repository, Persistence persistence) {
        this.projectId = projectId;
        this.repository = repository;
        this.persistence = persistence;
        try {
            this.cloneDir = Files.createTempDirectory("tandoori");
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary directory", e);
        }
    }

    @Override
    public void query() throws QueryException {
        Git gitRepo = initializeRepository();
        Iterable<RevCommit> commits = getCommits(gitRepo);

        List<String> statements = new ArrayList<>();
        int commitCount = 0;
        for (RevCommit commit : commits) {
            statements.add(persistStatement(commit, commitCount++));
        }
        // This is better to add them together as it creates a batch insert.
        persistence.addStatements(statements.toArray(new String[statements.size()]));

        finalizeRepository();
    }

    private static Iterable<RevCommit> getCommits(Git gitRepo) throws QueryException {
        Iterable<RevCommit> commits;
        try {
            commits = gitRepo.log().call();
        } catch (GitAPIException e) {
            throw new QueryException(logger.getName(), e);
        }
        return commits;
    }

    private String persistStatement(RevCommit commit, int count) {
        // TODO: Query is too raw over there, should we provide an orm?
        int authorId = getAuthorId(commit.getAuthorIdent().getEmailAddress());
        //TODO: How to retrieve the diff?!
        StringBuilder statement =
                new StringBuilder("INSERT INTO")
                        .append("Commit ")
                        .append("(projectId, developerId, sha1, ordinal, date, size)")
                        .append("VALUES ")
                        .append("(")
                        .append(projectId).append(authorId)//.append(commit.getFullMessage())
                        .append(commit.name()).append(count).append(commit.getCommitTime())
                        .append(commit.getTree()/*TODO*/)
                        .append(")");
        return statement.toString();
    }

    private static int getAuthorId(String emailAddress) {
        // FIXME: Query database to find developer ID, insert if not exists
        return 11;
    }

    private Git initializeRepository() throws QueryException {
        Git gitRepo;
        try {
            gitRepo = Git.cloneRepository()
                    .setDirectory(cloneDir.toFile())
                    .setURI(this.repository)
                    .call();
        } catch (GitAPIException e) {
            logger.error("Unable to clone repository: " + this.repository, e);
            throw new QueryException(logger.getName(), e);
        }
        return gitRepo;
    }

    private void finalizeRepository() {
        FilesUtils.recursiveDeletion(cloneDir);
    }
}
