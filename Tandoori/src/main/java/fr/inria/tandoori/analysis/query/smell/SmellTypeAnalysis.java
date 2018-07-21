package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SmellTypeAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellTypeAnalysis.class.getName());

    private final int projectId;
    private final Persistence persistence;
    private final Iterator<Map<String, Object>> smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    // Those attributes are the class state.
    private final List<Smell> previousCommitSmells;
    private final List<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;


    public SmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
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
                handleCommitChanges(underAnalysis);
                // Compare the two commits ordinal to find a gap.
                if (underAnalysis.hasGap(commit) && !underAnalysis.equals(Commit.EMPTY)) {
                    handleCommitGap(underAnalysis);
                }
                underAnalysis = commit;
                logger.debug("[" + projectId + "] => Now analysing commit: " + underAnalysis);
            }

            // We keep track of the smells present in our commit.
            currentCommitSmells.add(smell);

            // Update smell with parent instance, if any
            handleSmellRename(smell, underAnalysis);

            // Check if we already inserted smell previously to avoid having too much insert statements.
            // This could be removed and still checked by our unicity constraint.
            if (!previousCommitSmells.contains(smell)) {
                insertSmellInstance(smell);
            }

            insertSmellInCategory(smell, commit, SmellCategory.PRESENCE);
        }

        if (commit.equals(Commit.EMPTY)) {
            logger.info("[" + projectId + "] No smell found for type: " + smellType);
            return;
        }

        // We persist the introduction and refactoring of the last commit.
        handleCommitChanges(commit);

        // If we didn't reach the last project, it means we have refactored our smells
        // In a commit prior to it.
        String lastProjectSha1 = fetchLastProjectCommitSha();
        if (lastProjectSha1 == null) {
            logger.error("[" + projectId + "] ==> Could not find last commit sha1!");
        } else if (!commit.sha.equals(lastProjectSha1)) {
            logger.info("[" + projectId + "] Last analyzed commit is not last present commit: "
                    + commit.sha + " / " + lastProjectSha1);
            // The ordinal is unused here, so we can safely put current + 1
            handleCommitChanges(new Commit(lastProjectSha1, commit.ordinal + 1));
        } else {
            logger.info("[" + projectId + "] Last analysed commit is last project commit: " + commit.sha);
        }
    }

    /**
     * If we found a gap, it means that we have to smell of this type in the next commit.
     * Thus we consider that every smells has been refactored.
     *
     * @param commit The currently analyzed commit that has no direct child with smells.
     */
    private void handleCommitGap(Commit commit) {
        logger.info("[" + projectId + "] ==> Handling gap after commit: " + commit);
        try {
            commit = createNoSmellCommit(commit.ordinal + 1);
        } catch (Exception e) {
            logger.warn("An error occurred while treating gap, skipping", e);
            return;
        }
        // If we found the gap commit, we insert it as any other before continuing
        persistCommitChanges(commit);
        updateCommitTrackingCounters();
    }

    private String fetchLastProjectCommitSha() {
        List<Map<String, Object>> result = persistence.query(persistence.lastProjectCommitSha1QueryStatement(projectId));
        if (result.isEmpty()) {
            logger.warn("Unable to fetch last commit for project: " + projectId);
            return null;
        }
        return (String) result.get(0).get("sha1");
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

    /**
     * Trigger all actions linked to a change of commit.
     * I.e. persist the commit changes (Insertion, Refactor) and reset all tracking to diff the current commit
     * with the next one.
     *
     * @param current The current commit to persist and set as previous.
     */
    private void handleCommitChanges(Commit current) {
        persistCommitChanges(current);
        updateCommitTrackingCounters();
    }

    /**
     * Create a commit in case of a gap in the Paprika result set.
     * This means that all smells of the current type has been refactored.
     *
     * @param ordinal The missing commit ordinal.
     */
    private Commit createNoSmellCommit(int ordinal) throws Exception {
        List<Map<String, Object>> result = persistence.query(persistence.commitSha1QueryStatement(projectId, ordinal));
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
        previousCommitSmells.clear();
        previousCommitSmells.addAll(currentCommitSmells);
        currentCommitSmells.clear();
    }

    private void insertSmellInstance(Smell smell) {
        persistence.addStatements(persistence.smellInsertionStatement(projectId, smell));
    }

    /**
     * Trace the identifier of the currently analyzed commit.
     *
     * @param commitSha The sha to print ID for.
     */
    private void traceCommitIdentifier(String commitSha) {
        String commitQuery = persistence.commitIdQueryStatement(this.projectId, commitSha);
        List<Map<String, Object>> result = persistence.query(commitQuery);
        if (!result.isEmpty()) {
            logger.trace("[" + projectId + "]  => commit id: " + String.valueOf(result.get(0).get("id")));
        } else {
            logger.trace("[" + projectId + "] NO FOUND COMMIT!");
        }
    }

    private void insertSmellIntroductions(Commit commit) {
        List<Smell> introduction = new ArrayList<>(currentCommitSmells);
        introduction.removeAll(previousCommitSmells);

        for (Smell smell : introduction) {
            if (!currentCommitRenamed.contains(smell)) {
                insertSmellInCategory(smell, commit, SmellCategory.INTRODUCTION);
            }
        }
    }

    private void insertSmellRefactoring(Commit commit) {
        List<Smell> refactoring = new ArrayList<>(previousCommitSmells);
        refactoring.removeAll(currentCommitSmells);

        for (Smell smell : refactoring) {
            if (!currentCommitOriginal.contains(smell)) {
                insertSmellInCategory(smell, commit, SmellCategory.REFACTOR);
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
    private void insertSmellInCategory(Smell smell, Commit commit, SmellCategory category) {
        persistence.addStatements(persistence.smellCategoryInsertionStatement(projectId, commit.sha, smell, category));
    }
}
