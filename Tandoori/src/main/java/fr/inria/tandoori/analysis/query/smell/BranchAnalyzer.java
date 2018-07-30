package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class BranchAnalyzer extends AbstractSmellTypeAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(BranchAnalyzer.class.getName());

    private final SmellDuplicationChecker duplicationChecker;
    private final boolean handleGap;

    // Those attributes are the class state.
    private final Set<Smell> previousCommitSmells;
    private final Set<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;
    private Commit underAnalysis;
    private int lostCommitOrdinal;

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker) {
        this(projectId, persistence, duplicationChecker, true);
    }

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker, boolean handleGap) {
        super(logger, projectId, persistence);
        this.duplicationChecker = duplicationChecker;
        this.handleGap = handleGap;

        previousCommitSmells = new HashSet<>();
        currentCommitSmells = new HashSet<>();
        currentCommitOriginal = new ArrayList<>();
        currentCommitRenamed = new ArrayList<>();
        underAnalysis = Commit.EMPTY;
        this.resetLostCommit();
    }

    /**
     * Add the {@link List} of {@link Smell} as already existing in the current branch.
     * This means that the smells will be considered as already introduced, and existing in the smell table.
     * For this we set them in both the previousCommitSmells and currentCommitSmells.
     *
     * @param smells The smells to add.
     */
    public void addExistingSmells(List<Smell> smells) {
        previousCommitSmells.addAll(smells);
        currentCommitSmells.addAll(smells);
    }

    /**
     * Specifically add smells to the previous ones.
     * This method is used for adding all smells before a merge commit occurs.
     *
     * @param smells The smells to add.
     */
    public void addPreviousSmells(List<Smell> smells) {
        previousCommitSmells.addAll(smells);
    }

    void addSmellCommit(Smell smell, Commit commit) {
        // We handle the commit change in our result dataset.
        // This dataset MUST be ordered by commit_number to have right results.
        if (!underAnalysis.equals(commit)) {
            handleCommitChanges(underAnalysis);
            // Compare the two commits ordinal to find a gap.
            if (handleGap && underAnalysis.hasGap(commit) && !underAnalysis.equals(Commit.EMPTY)) {
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

    void finalizeAnalysis() throws QueryException {
        finalizeAnalysis(fetchLastProjectCommitSha());
    }

    void finalizeAnalysis(String lastCommitSha1) {
        if (underAnalysis.equals(Commit.EMPTY)) {
            logger.info("[" + projectId + "] No smell found");
            return;
        }

        // We persist the introduction and refactoring of the last commit.
        handleCommitChanges(underAnalysis);

        // If we didn't reach the last project, it means we have refactored our smells
        // In a commit prior to it.
        if (!underAnalysis.sha.equals(lastCommitSha1)) {
            logger.info("[" + projectId + "] Last analyzed commit is not last present commit: "
                    + underAnalysis.sha + " / " + lastCommitSha1);
            // The ordinal is unused here, so we can safely put current + 1
            handleCommitChanges(new Commit(lastCommitSha1, underAnalysis.ordinal + 1));
        } else {
            logger.info("[" + projectId + "] Last analysed commit is last project commit: " + underAnalysis.sha);
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
        int nextOrdinal = commit.ordinal + 1;
        try {
            commit = createNoSmellCommit(nextOrdinal);
        } catch (CommitNotFoundException e) {
            logger.warn("An error occurred while treating gap, inserting in lost smells: " + e.getMessage());
            setLostCommit(nextOrdinal);
            return;
        }
        // If we found the gap commit, we insert it as any other before continuing
        persistCommitChanges(commit);
        updateCommitTrackingCounters();
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
        if (isLostCommit()) {
            persistLostChanges(current);
            resetLostCommit();
        } else {
            persistCommitChanges(current);
        }
        updateCommitTrackingCounters();
    }

    private boolean isLostCommit() {
        return lostCommitOrdinal > -1;
    }

    private void setLostCommit(int ordinal) {
        this.lostCommitOrdinal = ordinal;
    }

    private void resetLostCommit() {
        this.lostCommitOrdinal = -1;
    }

    /**
     * Persist all the introduction and refactoring, binding them to the given commit
     *
     * @param commit The new commit.
     */
    private void persistCommitChanges(Commit commit) {
        if (!commit.equals(Commit.EMPTY)) {
            logger.debug("[" + projectId + "] ==> Persisting smells for commit: " + commit);
            insertSmellIntroductions(commit, previousCommitSmells, currentCommitSmells, currentCommitRenamed);
            insertSmellRefactorings(commit, previousCommitSmells, currentCommitSmells, currentCommitOriginal);
        }
    }

    /**
     * Persist all the introduction and refactoring, binding them to the table of Lost smells.
     *
     * @param commit The new commit.
     */
    private void persistLostChanges(Commit commit) {
        logger.debug("[" + projectId + "] ==> Handling lost commit from " + lostCommitOrdinal + " to " + commit.ordinal);
        insertLostSmellIntroductions(lostCommitOrdinal, commit.ordinal,
                previousCommitSmells, currentCommitSmells, currentCommitRenamed);
        insertLostSmellRefactorings(lostCommitOrdinal, commit.ordinal,
                previousCommitSmells, currentCommitSmells, currentCommitOriginal);
    }

    private void updateCommitTrackingCounters() {
        previousCommitSmells.clear();
        previousCommitSmells.addAll(currentCommitSmells);
        currentCommitSmells.clear();
    }
}
