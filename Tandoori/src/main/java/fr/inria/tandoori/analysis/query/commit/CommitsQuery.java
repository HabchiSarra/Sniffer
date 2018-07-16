package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.smell.Commit;
import neo4j.QueryEngine;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch all commits and developers for a project.
 */
public class CommitsQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    public static final int BATCH_SIZE = 1000;
    private final int projectId;
    private final String paprikaDB;
    private final Repository repository;
    private final Persistence persistence;

    public CommitsQuery(int projectId, String paprikaDB, String repository, Persistence persistence) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.repository = new Repository(repository);
        this.persistence = persistence;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Commits insertion");
        Git gitRepository = initializeJGitRepository();

        QueryEngine engine = new QueryEngine(paprikaDB);
        Result commits = getCommits(engine);

        List<String> commitStatements = new ArrayList<>();
        List<String> authorStatements = new ArrayList<>();
        List<String> renameStatements = new ArrayList<>();

        int commitCount = 0;
        CommitDetails details;
        RevCommit commit;
        while (commits.hasNext()) {
            commit = findJGitCommit(gitRepository, Commit.fromInstance(commits.next()));

            logger.debug("[" + projectId + "] => Analyzing commit: " + commit.name());
            details = CommitDetails.fetch(repository.getRepoDir().toString(), commit.name());

            authorStatements.addAll(authorStatements(commit.getAuthorIdent().getEmailAddress()));
            commitStatements.add(commitStatement(commit, commitCount++, details));
            renameStatements.addAll(fileRenameStatements(commit, details));

            if (commitCount % BATCH_SIZE == 0) {
                logger.info("[" + projectId + "] Persist commit batch of size: " + BATCH_SIZE);
                persistBatch(commitStatements, authorStatements, renameStatements);
                authorStatements.clear();
                commitStatements.clear();
                renameStatements.clear();
            }
        }
        persistBatch(commitStatements, authorStatements, renameStatements);

        engine.shutDown();
        repository.finalizeRepository();
    }

    /**
     * We use the {@link RevCommit} to easily access Author and Commit message.
     *
     * @param repo   The {@link Git} repository.
     * @param commit The {@link Commit} definition.
     * @return The found {@link RevCommit}
     */
    private RevCommit findJGitCommit(Git repo, Commit commit) throws QueryException {
        try (RevWalk revWalk = new RevWalk(repo.getRepository())) {
            ObjectId commitId = ObjectId.fromString(commit.sha);
            return revWalk.parseCommit(commitId);
        } catch (IOException e) {
            throw new QueryException(logger.getName(),
                    "Unable to retrieve commit " + commit.sha + " in git repository " + repo);
        }
    }

    private Git initializeJGitRepository() throws QueryException {
        try {
            return repository.initializeRepository();
        } catch (Repository.RepositoryException e) {
            throw new QueryException(logger.getName(), e);
        }
    }

    /**
     * Persist the current commit state.
     * We add everything in a bulk insert since we must have a coherent state.
     * Warning, we have to insert authors, then commits, then renaming!
     *
     * @param commitStatements CommitEntry to persists.
     * @param authorStatements Developer to persist.
     * @param renameStatements FileRename to persist.
     */
    private void persistBatch(List<String> commitStatements, List<String> authorStatements, List<String> renameStatements) {
        persistence.addStatements(authorStatements.toArray(new String[0]));
        persistence.addStatements(commitStatements.toArray(new String[0]));
        persistence.addStatements(renameStatements.toArray(new String[0]));
        persistence.commit();
    }

    private static Result getCommits(QueryEngine engine) throws QueryException {
        return new neo4j.CommitsQuery(engine).streamResult(true, true);
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
        String authorProjectInsert = "INSERT INTO ProjectDeveloper (developerId, projectId) VALUES (" +
                "(" + developerQuery + "), " + projectId + ") ON CONFLICT DO NOTHING;";
        statements.add(authorProjectInsert);

        return statements;
    }

    private String commitStatement(RevCommit commit, int count, CommitDetails details) {
        String authorEmail = commit.getAuthorIdent().getEmailAddress();
        String developerQuery = persistence.developerQueryStatement(projectId, authorEmail);
        GitDiff diff = details.diff;

        DateTime commitDate = new DateTime(((long) commit.getCommitTime()) * 1000);
        logger.trace("[" + projectId + "] Commit time is: " + commit.getCommitTime() + "(datetime: " + commitDate + ")");

        return "INSERT INTO CommitEntry (projectId, developerId, sha1, ordinal, date, additions, deletions, filesChanged, message) VALUES ('" +
                projectId + "', (" + developerQuery + "), '" + commit.name() + "', " + count + ", '" + commitDate.toString() +
                "', " + diff.getAddition() + ", " + diff.getDeletion() + ", " + diff.getChangedFiles() + ", $$" + commit.getFullMessage() + "$$) ON CONFLICT DO NOTHING";
    }

    private List<String> fileRenameStatements(RevCommit commit, CommitDetails details) {
        String commitSelect = persistence.commitQueryStatement(projectId, commit.name());
        List<String> result = new ArrayList<>();

        for (GitRename rename : details.renames) {
            if (!(rename.oldFile.endsWith(".java") && rename.newFile.endsWith(".java"))) {
                continue;
            }

            logger.debug("[" + projectId + "]  => Found .java renamed: " + rename.oldFile);
            logger.trace("[" + projectId + "]    => new file: " + rename.newFile);
            logger.trace("[" + projectId + "]    => Similarity: " + rename.similarity);

            result.add(renameInsertStatement(commitSelect, rename));
        }
        return result;
    }

    /**
     * Generate an insertion line for the given rename statement.
     *
     * @param commitSelect Query to select the right commit id.
     * @param rename       Rename info to use.
     */
    private String renameInsertStatement(String commitSelect, GitRename rename) {
        return "INSERT INTO FileRename (projectId, commitId, oldFile, newFile, similarity) VALUES ('" +
                projectId + "',(" + commitSelect + "), '" + rename.oldFile + "', '" +
                rename.newFile + "', " + rename.similarity + ") ON CONFLICT DO NOTHING";
    }
}
