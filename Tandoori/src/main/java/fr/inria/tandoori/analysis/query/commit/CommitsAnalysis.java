package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.CommitDetails;
import fr.inria.tandoori.analysis.model.GitRename;
import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Actual analysis and persisting of commits and authors for a project.
 */
class CommitsAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsAnalysis.class.getName());
    public static final int BATCH_SIZE = 1000;

    private final int projectId;
    private final Repository repository;
    private final Map<String, Commit> paprikaCommits;
    private final CommitDetailsChecker detailsChecker;

    private final Persistence persistence;
    private final DeveloperQueries developerQueries;
    private final CommitQueries commitQueries;
    private final boolean paprikaOnly;

    CommitsAnalysis(int projectId, Persistence persistence, Repository repository,
                    Iterator<Map<String, Object>> commits,
                    CommitDetailsChecker detailsChecker,
                    DeveloperQueries developerQueries, CommitQueries commitQueries) {
        this(projectId, persistence, repository, commits, detailsChecker, developerQueries, commitQueries, false);
    }

    CommitsAnalysis(int projectId, Persistence persistence, Repository repository,
                    Iterator<Map<String, Object>> commits,
                    CommitDetailsChecker detailsChecker,
                    DeveloperQueries developerQueries, CommitQueries commitQueries,
                    boolean paprikaOnly) {
        this.projectId = projectId;
        this.persistence = persistence;
        this.repository = repository;
        this.paprikaCommits = mapPaprikaCommits(commits);
        this.detailsChecker = detailsChecker;
        this.developerQueries = developerQueries;
        this.commitQueries = commitQueries;
        this.paprikaOnly = paprikaOnly;
    }

    private static Map<String, Commit> mapPaprikaCommits(Iterator<Map<String, Object>> commits) {
        Map<String, Commit> mapping = new HashMap<>();
        Commit paprikaCommit;
        while (commits.hasNext()) {
            paprikaCommit = Commit.fromInstance(commits.next());
            mapping.put(paprikaCommit.sha, paprikaCommit);
        }
        return mapping;
    }

    @Override
    public void query() throws QueryException {
        List<String> commitStatements = new ArrayList<>();
        List<String> authorStatements = new ArrayList<>();
        List<String> renameStatements = new ArrayList<>();

        int commitCount = 0;
        CommitDetails details;
        Commit currentCommit;
        for (String commit : choseCommitsSource()) {
            currentCommit = fillCommit(commit);

            logger.debug("[" + projectId + "] => Analyzing commit: " + currentCommit.sha);
            details = detailsChecker.fetch(currentCommit.sha);

            authorStatements.addAll(authorStatements(currentCommit.authorEmail));
            // GitCommit will not contain the right ordinal.
            commitStatements.add(commitStatement(currentCommit, details));
            renameStatements.addAll(fileRenameStatements(currentCommit, details));

            if (++commitCount % BATCH_SIZE == 0) {
                logger.info("[" + projectId + "] Persist commit batch of size: " + BATCH_SIZE);
                persistBatch(commitStatements, authorStatements, renameStatements);
                authorStatements.clear();
                commitStatements.clear();
                renameStatements.clear();
            }
        }
        persistBatch(commitStatements, authorStatements, renameStatements);
    }

    /**
     * Chose the commits source depending on if we want to query only Paprika
     * commits or not.
     *
     * @return The list of sha to add to the project.
     * @throws QueryException If anything goes wrong.
     */
    private List<String> choseCommitsSource() throws QueryException {
        if (paprikaOnly) {
            List<String> shas = new ArrayList<>();
            for (Commit commit : paprikaCommits.values()) {
                shas.add(commit.sha);
            }
            return shas;
        } else {
            List<String> list = fetchGitLog();
            Collections.reverse(list);
            return list;
        }
    }

    /**
     * Retrieve the git repository's log.
     *
     * @return An iterable of SHA1s.
     * @throws QueryException If anything goes wrong.
     */
    private List<String> fetchGitLog() throws QueryException {
        try {
            return repository.getLog();
        } catch (IOException e) {
            throw new QueryException(logger.getName(), e.getMessage());
        }
    }

    /**
     * Retrieve all details for a commit, currently;
     * - Author,
     * - Message,
     * - Date,
     * - If it exists in Paprika.
     * - Ordinal (from Paprika)
     *
     * @param sha1 The commit's sha1.
     * @return A new, filled in {@link Commit}.
     * @throws QueryException If anything goes wrong.
     */
    private Commit fillCommit(String sha1) throws QueryException {
        Commit result;
        Commit paprikaCommit = paprikaCommits.getOrDefault(sha1, null);
        try {
            result = repository.getCommitWithDetails(sha1);

            // Add parents to the gitCommit.
            result.setParents(repository.getCommitWithParents(sha1).parents);

            // Set paprika data, i.e. the ordinal
            if (paprikaCommit != null) {
                // We chose to use Paprika order in commit insertion
                result.setOrdinal(paprikaCommit.ordinal);
                result.setInPaprika(true);
            } else {
                result.setInPaprika(false);
            }
        } catch (IOException e) {
            throw new QueryException(logger.getName(),
                    "Unable to retrieve commit " + paprikaCommit.sha + " in git repository " + repository);
        }
        return result;
    }

    /**
     * Create Developer and project_developer insertion statements.
     *
     * @param emailAddress The developer mail.
     * @return The generated statements.
     */
    private List<String> authorStatements(String emailAddress) {
        List<String> statements = new ArrayList<>();

        // Try to insert the developer if not exist
        statements.add(developerQueries.developerInsertStatement(emailAddress));

        // Try to insert the developer/project mapping if not exist
        statements.add(developerQueries.projectDeveloperInsertStatement(projectId, emailAddress));

        return statements;
    }

    /**
     * Creates the {@link Commit} insertion statement.
     *
     * @param commit  Commit from Git, containing main data (message, author, ...)
     * @param details Commit details containing file_rename and {@link fr.inria.tandoori.analysis.model.GitDiff} info.
     * @return The generated persistence statement.
     */
    private String commitStatement(Commit commit, CommitDetails details) {
        return commitQueries.commitInsertionStatement(projectId, commit, details.diff);
    }

    /**
     * Creates the file_rename insertion statements.
     *
     * @param commit  The commit to generate renames onto.
     * @param details The commit details containing FileRename.
     * @return The generated statements.
     */
    private List<String> fileRenameStatements(Commit commit, CommitDetails details) {
        List<String> result = new ArrayList<>();

        for (GitRename rename : details.renames) {
            if (!(rename.oldFile.endsWith(".java") && rename.newFile.endsWith(".java"))) {
                continue;
            }

            logger.debug("[" + projectId + "]  => Found .java renamed: " + rename.oldFile);
            logger.trace("[" + projectId + "]    => new file: " + rename.newFile);
            logger.trace("[" + projectId + "]    => Similarity: " + rename.similarity);

            result.add(commitQueries.fileRenameInsertionStatement(projectId, commit.sha, rename));
        }
        return result;
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
}
