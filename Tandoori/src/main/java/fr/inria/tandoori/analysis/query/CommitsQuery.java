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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch all commits and developers for a project.
 */
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
        int authorId = insertAuthor(commit.getAuthorIdent().getEmailAddress());
        //TODO: How to retrieve the diff?!
        StringBuilder statement =
                new StringBuilder("INSERT INTO")
                        .append("Commit ")
                        .append("(projectId, developerId, sha1, ordinal, date)") // TODO: size
                        .append("VALUES ")
                        .append("(")
                        .append(projectId).append(authorId)
                        .append(commit.name()).append(count).append(commit.getCommitTime())
                       // .append(commit.getTree()/*TODO: commit size (addition, deletion)*/)
                        .append(")");
        return statement.toString();
    }

    private int insertAuthor(String emailAddress) {
        String developerQuery = "SELECT FROM Developer where username = " + emailAddress;

        // Try to insert the developer if not exist
        String authorInsert = "INSERT INTO Developer (username) VALUES (" + emailAddress + ");";
        persistence.addStatements(authorInsert);
        persistence.commit();

        // Try to insert the developer/project mapping if not exist
        String authorProjectInsert = "INSERT INTO ProjectDeveloper (developerId, projectId) VALUES ("
                + developerQuery + ", " + projectId + ");";
        persistence.addStatements(authorProjectInsert);
        persistence.commit();

        // Retrieve developer ID for further usage
        ResultSet result = persistence.query(developerQuery);
        try {
            return result.getInt("id");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve developer id (" + emailAddress + ")", e);
        }
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
