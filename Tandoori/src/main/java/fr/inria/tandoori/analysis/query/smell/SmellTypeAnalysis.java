package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmellTypeAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellTypeAnalysis.class.getName());

    private final int projectId;
    private final Persistence persistence;
    private final Result smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    // Those attributes are the class state.
    private final List<Smell> previousCommitSmells;
    private final List<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;


    public SmellTypeAnalysis(int projectId, Persistence persistence, Result smells,
                             String smellType, SmellDuplicationChecker duplicationChecker) {
        this.projectId = projectId;
        this.persistence = persistence;
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;

        previousCommitSmells = new ArrayList<>();
        currentCommitSmells = new ArrayList<>();
        currentCommitOriginal = new ArrayList<>();
        currentCommitRenamed = new ArrayList<>();
    }


    @Override
    public void query() throws QueryException {
        Smell smell;
        // Analyzed commit
        Commit underAnalysis = Commit.EMPTY;
        // Current smell commit, most of the time equals to 'underAnalysis'
        Commit commit = Commit.EMPTY;

        Map<String, Object> instance;
        while (smells.hasNext()) {
            instance = smells.next();
            smell = Smell.fromInstance(instance, smellType);
            commit = Commit.fromInstance(instance);

            // We handle the commit change in our result dataset.
            // This dataset MUST be ordered by commit_number to have right results.
            if (!underAnalysis.equals(commit)) {
                underAnalysis = updateCurrentCommit(underAnalysis, commit);
            }

            // We keep track of the smells present in our commit.
            currentCommitSmells.add(smell);

            handleSmellRename(smell, underAnalysis);

            // Check if we already inserted smell previously to avoid having too much insert statements.
            // This could be removed and still checked by our unicity constraint.
            if (!previousCommitSmells.contains(smell)) {
                insertSmellInstance(smell);
            }
            insertSmellPresence(smell, commit);
        }

        handleAllRefactoredBeforeLastCommit(commit);
    }

    /**
     * Handle the fact that we could refactor all our smells before the last commit, and not
     *
     * @param commit The last commit referencing smells.
     */
    private void handleAllRefactoredBeforeLastCommit(Commit commit) {
        List<Map<String, Object>> result = persistence.query(persistence.lastProjectCommitSha1QueryStatement(projectId));
        String lastProjectSha1 = (String) result.get(0).get("sha1");
        if (result.isEmpty()) {
            logger.warn("Unable to fetch last commit for project: " + projectId);
        } else {
            // If we didn't reach the last project, it means we have refactored our smells
            // In a commit prior to it.
            // TODO: What if PaprikaDB and Postgresql commits are not synchronized?! --> Wrong
            if (!commit.sha.equals(lastProjectSha1)) {
                try {
                    commit = createNoSmellCommit(commit.ordinal + 1);
                    updateCommitTrackingCounters();
                } catch (Exception e) {
                    logger.warn("An error occurred while treating gap, skipping", e);
                }
                persistCommitChanges(commit);
            }
        }
    }

    /**
     * In this test we have a smell with its file directing to a newly renamed file.
     * We will have to guess the previous smell instance by rewriting its instance id with the file before being renamed.
     * This instance ID will then be compared with the list of instances from the previous smell to find a match.
     *
     * @param smell  The smell to guess if it has been renamed from a previous smell.
     * @param commit The currently analyzed commit.
     */
    private void handleSmellRename(Smell smell, Commit commit) {
        Smell original = duplicationChecker.original(smell, commit);

        // If we correctly guessed the smell identifier, we will find it in the previous commit smells
        if (original != null && previousCommitSmells.contains(original)) {
            logger.debug("[" + projectId + "] => Guessed rename for smell: " + smell);
            logger.trace("[" + projectId + "]   => potential parent: " + original);
            currentCommitOriginal.add(original);
            currentCommitRenamed.add(smell);
            smell.parentInstance = original.instance;
        }
    }

    private Commit updateCurrentCommit(Commit underAnalysis, Commit commit) {
        // If we found a gap, it means that we have to smell of this type in the next commit.
        // Thus we consider that every smells has been refactored.
        if (underAnalysis.hasGap(commit)) {
            try {
                commit = createNoSmellCommit(commit.ordinal + 1);
                // We consider a new commit with no smell in it.
                updateCommitTrackingCounters();
            } catch (Exception e) {
                logger.warn("An error occurred while treating gap, skipping", e);
            }
        }

        persistCommitChanges(commit);
        updateCommitTrackingCounters();
        return commit;
    }

    /**
     * Create a commit in case of a gap in the Paprika result set.
     * This means that all smells of the current type has been refactored.
     *
     * @param ordinal The missing commit ordinal.
     */
    private Commit createNoSmellCommit(int ordinal) throws Exception {
        List<Map<String, Object>> result = persistence.query("SELECT sha1 FROM CommitEntry " +
                "WHERE ordinal = '" + ordinal + "' AND projectid = '" + projectId + "'");
        if (result.isEmpty()) {
            throw new Exception("[" + projectId + "] ==> Unable to fetch commit n°: " + ordinal);
        }
        return new Commit(String.valueOf(result.get(0).get("sha1")), ordinal);
    }

    /**
     * Transfer all smells from current commit to the previous one, and change the current sha.
     *
     * @param commit The new commit.
     */
    private void persistCommitChanges(Commit commit) {
        logger.debug("[" + projectId + "] ==> Handling commit: " + commit);
        if (logger.isTraceEnabled()) {
            traceCommitIdentifier(commit.sha);
        }
        insertSmellIntroductions(commit);
        insertSmellRefactoring(commit);
    }

    private void updateCommitTrackingCounters() {
        currentCommitOriginal.clear();
        currentCommitRenamed.clear();
        previousCommitSmells.clear();
        previousCommitSmells.addAll(currentCommitSmells);
        currentCommitSmells.clear();
    }

    private void insertSmellInstance(Smell smell) {
        // We know that the parent smell is the last inserted one.
        String parentSmellQuery = persistence.smellQueryStatement(projectId, smell.parentInstance, smell.type, true);
        String parentQuery = smell.parentInstance != null ? "(" + parentSmellQuery + ")" : null;
        String smellInsert = "INSERT INTO Smell (projectId, instance, type, file, renamedFrom) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '" + smell.file + "', " + parentQuery + ") ON CONFLICT DO NOTHING;";
        persistence.addStatements(smellInsert);
    }

    /**
     * Trace the identifier of the currently analyzed commit.
     *
     * @param commitSha The sha to print ID for.
     */
    private void traceCommitIdentifier(String commitSha) {
        String commitQuery = persistence.commitQueryStatement(this.projectId, commitSha);
        List<Map<String, Object>> result = persistence.query(commitQuery);
        if (!result.isEmpty()) {
            logger.trace("[" + projectId + "]  => commit id: " + String.valueOf(result.get(0).get("id")));
        } else {
            logger.trace("[" + projectId + "] NO FOUND COMMIT!");
        }
    }

    private void insertSmellPresence(Smell smell, Commit commit) {
        insertSmellInCategory(smell, commit, "SmellPresence");
    }

    private void insertSmellIntroductions(Commit commit) {
        List<Smell> introduction = new ArrayList<>(currentCommitSmells);
        introduction.removeAll(previousCommitSmells);

        for (Smell smell : introduction) {
            if (!currentCommitRenamed.contains(smell)) {
                insertSmellInCategory(smell, commit, "SmellIntroduction");
            }
        }
    }

    private void insertSmellRefactoring(Commit commit) {
        List<Smell> refactoring = new ArrayList<>(previousCommitSmells);
        refactoring.removeAll(currentCommitSmells);

        for (Smell smell : refactoring) {
            if (!currentCommitOriginal.contains(smell)) {
                insertSmellInCategory(smell, commit, "SmellRefactor");
            }
        }
    }

    /**
     * Helper method adding Smell- -Presence, -Introduction, or -Refactor statement.
     *
     * @param smell    The smell to insert.
     * @param commit   The commit to insert into.
     * @param category The table category, either SmellPresence, SmellIntroduction, or SmellRefactor
     */
    private void insertSmellInCategory(Smell smell, Commit commit, String category) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        String smellQuery = persistence.smellQueryStatement(projectId, smell.instance, smell.type, true);
        String commitQuery = persistence.commitQueryStatement(this.projectId, commit.sha);
        String smellPresenceInsert = "INSERT INTO " + category + " (smellId, commitId) VALUES " +
                "((" + smellQuery + "), (" + commitQuery + "));";
        persistence.addStatements(smellPresenceInsert);
    }
}
