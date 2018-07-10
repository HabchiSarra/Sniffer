package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.FilesUtils;
import fr.inria.tandoori.analysis.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch all commits and developers for a project.
 */
public class CommitsQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    private int projectId;
    private final Repository repository;
    private Persistence persistence;

    private static final int SIMILARITY_THRESHOLD = 50;

    public CommitsQuery(int projectId, String repository, Persistence persistence) {
        this.projectId = projectId;
        this.repository = new Repository(repository);
        this.persistence = persistence;
    }

    @Override
    public void query() throws QueryException {
        logger.info("### Starting Commits insertion ###");
        Git gitRepo = null;
        try {
            gitRepo = repository.initializeRepository();
        } catch (Repository.RepositoryException e) {
            throw new QueryException(logger.getName(), e);
        }
        Iterable<RevCommit> commits = getCommits(gitRepo);
        List<RevCommit> commitsList = new ArrayList<>();
        // Reverse our commit list.
        for (RevCommit commit : commits) {
            commitsList.add(0, commit);
        }

        String[] commitStatements = new String[commitsList.size()];
        List<String> authorStatements = new ArrayList<>();
        List<String> renameStatements = new ArrayList<>();
        int commitCount = 0;
        for (RevCommit commit : commitsList) {
            logger.debug("=> Analyzing commit: " + commit.name());
            authorStatements.addAll(authorStatements(commit.getAuthorIdent().getEmailAddress()));
            commitStatements[commitCount] = commitStatement(commit, commitCount++);
            renameStatements.addAll(fileRenameStatements(commit));
        }

        // We add everything in a bulk insert since we must have a coherent state.
        // Warning, we have to insert authors, then commits, then renaming!
        persistence.addStatements(authorStatements.toArray(new String[0]));
        persistence.addStatements(commitStatements);
        persistence.addStatements(renameStatements.toArray(new String[0]));
        persistence.commit();

        repository.finalizeRepository();
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

    private List<String> authorStatements(String emailAddress) {
        String developerQuery = persistence.developerQueryStatement(projectId, emailAddress);
        List<String> statements = new ArrayList<>();

        // Try to insert the developer if not exist
        String authorInsert = "INSERT INTO Developer (username) VALUES ('" + emailAddress + "') ON CONFLICT DO NOTHING;";
        statements.add(authorInsert);

        // Try to insert the developer/project mapping if not exist
        String projectDevQuery = persistence.projectDevQueryStatement(projectId, emailAddress);
        String authorProjectInsert = "INSERT INTO ProjectDeveloper (developerId, projectId) VALUES (" +
                "(" + developerQuery + "), " + projectId + ") ON CONFLICT DO NOTHING;";
        statements.add(authorProjectInsert);

        return statements;
    }

    private String commitStatement(RevCommit commit, int count) {
        String authorEmail = commit.getAuthorIdent().getEmailAddress();
        String developerQuery = persistence.developerQueryStatement(projectId, authorEmail);

        GitDiffResult diff = GitDiffResult.fetch(repository.getRepoDir().toString(), commit.name());
        logger.trace("Commit diff is +: " + diff.getAddition() + ", -: " + diff.getDeletion() + ", file: " + diff.getChangedFiles());

        DateTime commitDate = new DateTime(((long) commit.getCommitTime()) * 1000);
        logger.trace("Commit time is: " + commit.getCommitTime() + "(datetime: " + commitDate + ")");

        return "INSERT INTO CommitEntry (projectId, developerId, sha1, ordinal, date, additions, deletions, filesChanged) VALUES ('" +
                projectId + "', (" + developerQuery + "), '" + commit.name() + "', " + count + ", '" + commitDate.toString() +
                "', " + diff.getAddition() + ", " + diff.getDeletion() + ", " + diff.getChangedFiles() + ") ON CONFLICT DO NOTHING";
    }

    private List<String> fileRenameStatements(RevCommit commit) {
        // TODO: See if we can use Jgit instead of a raw call to git
        List<String> result = new ArrayList<>();
        try {
            String command = "git -C " + repository.getRepoDir().toString() + " show " + commit.name() + " -M50% --summary --format=";
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String commitSelect = persistence.commitQueryStatement(projectId, commit.name());

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String errLine;
            while ((errLine = stdError.readLine()) != null) {
                logger.error(errLine);
            }

            String line = reader.readLine();
            String rename;
            while (line != null) {
                try {
                    rename = generateRenameStatement(commitSelect, line);
                    if (rename != null) {
                        result.add(rename);
                    }
                } catch (Exception e) {
                    logger.warn("Unable to parse file rename: ", e);
                } finally {
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            logger.error("Unable to execute git command", e);
        }
        return result;
    }

    /**
     * Parse and add insertion statement in case of a renamed entry in the git diff.
     *
     * @param commitSelect Query to select the right commit id.
     * @param line         Line to parse for rename.
     */
    private String generateRenameStatement(String commitSelect, String line) throws Exception {
        if (!line.trim().startsWith("rename")) {
            return null;
        }

        GitRenameParser.RenameParsingResult result = GitRenameParser.parseRenamed(line.trim());
        if (!(result.oldFile.endsWith(".java") && result.newFile.endsWith(".java"))) {
            return null;
        }

        logger.debug("  => Found .java renamed: " + result.oldFile);
        logger.trace("    => new file: " + result.newFile);
        logger.trace("    => Similarity: " + result.similarity);
        return "INSERT INTO FileRename (projectId, commitId, oldFile, newFile, similarity) VALUES ('" +
                projectId + "',(" + commitSelect + "), '" + result.oldFile + "', '" +
                result.newFile + "', " + result.similarity + ") ON CONFLICT DO NOTHING";
    }
}
