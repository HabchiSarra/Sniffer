package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class BranchAnalyzer extends AbstractSmellTypeAnalysis implements BranchAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(BranchAnalyzer.class.getName());

    private final SmellDuplicationChecker duplicationChecker;
    private final boolean handleGap;

    // Those attributes are the class state.
    private Commit previous;
    private Commit underAnalysis;
    private int lostCommitOrdinal;

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                   CommitQueries commitQueries, SmellQueries smellQueries) {
        this(projectId, persistence, duplicationChecker, commitQueries, smellQueries, true);
    }

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                   CommitQueries commitQueries, SmellQueries smellQueries,
                   boolean handleGap) {
        super(logger, projectId, persistence, commitQueries, smellQueries);
        this.duplicationChecker = duplicationChecker;
        this.handleGap = handleGap;

        previous = Commit.empty();
        underAnalysis = Commit.empty();
        this.resetLostCommit();
    }

    @Override
    public void addExistingSmells(List<Smell> smells) {
        underAnalysis.addSmells(smells);
    }

    @Override
    public void addMergedSmells(List<Smell> smells) {
        underAnalysis.addMergedSmells(smells);
    }

    @Override
    public void notifyCommit(Commit commit) {
        // We handle the commit change in our result dataset.
        // This dataset MUST be ordered by commit_number to have right results.
        if (!underAnalysis.equals(commit)) {
            if (!underAnalysis.equals(Commit.empty())) {
                handleCommitChanges(underAnalysis);
            }
            // Compare the two commits ordinal to find a gap.
            if (handleGap && underAnalysis.hasGap(commit) && !underAnalysis.equals(Commit.empty())) {
                handleCommitGap();
            }
            updateCommitTracking(commit);
            logger.debug("[" + projectId + "] => Now analysing commit: " + underAnalysis);
        }
    }

    @Override
    public void notifySmell(Smell smell) {
        // We keep track of the smells present in our commit.
        underAnalysis.addSmell(smell);

        // Update smell with parent instance, if any
        handleSmellRename(smell, underAnalysis);

        // Check if we already inserted smell previously to avoid having too much insert statements.
        // This could be removed and still checked by our unicity constraint.
        if (isNew(smell)) {
            insertSmellInstance(smell);
            persistence.commit();
        }

        insertSmellInCategory(smell, underAnalysis, SmellCategory.PRESENCE);
    }

    /**
     * Check if the smell has been seen before.
     * <p>
     * A {@link Smell} is new if it isn't one of the previous {@link Commit}'s {@link Smell}s
     * of one of the underAnalysis {@link Commit}'s merged smells if any.
     *
     * @param smell The smell to check.
     * @return true if it is a brand new smell, false if it is already in the repository.
     */
    private boolean isNew(Smell smell) {
        return !previous.getSmells().contains(smell) && !underAnalysis.getMergedSmells().contains(smell);
    }

    @Override
    public void notifyEnd() throws QueryException {
        notifyEnd(fetchLastProjectCommitSha());
    }

    @Override
    public void notifyEnd(String lastCommitSha1) {
        if (underAnalysis.equals(Commit.empty())) {
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
            updateCommitTracking(new Commit(lastCommitSha1, underAnalysis.ordinal + 1));
            handleCommitChanges(underAnalysis);
        } else {
            logger.info("[" + projectId + "] Last analysed commit is last project commit: " + underAnalysis.sha);
        }
    }

    /**
     * If we found a gap, it means that we have to smell of this type in the next commit.
     * Thus we consider that every smells has been refactored.
     */
    private void handleCommitGap() {
        logger.info("[" + projectId + "] ==> Handling gap after commit: " + underAnalysis);
        int nextOrdinal = underAnalysis.ordinal + 1;
        try {
            Commit emptyCommit = createNoSmellCommit(nextOrdinal);
            // If we found the gap commit, we insert it as any other before continuing
            updateCommitTracking(emptyCommit);
            persistCommitChanges(emptyCommit);
        } catch (CommitNotFoundException e) {
            logger.warn("An error occurred while treating gap, inserting in lost smells: " + e.getMessage());
            setLostCommit(nextOrdinal);
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

        // TODO: Is this condition useful?
        if (original != null && previous.getSmells().contains(original)) {
            logger.debug("[" + projectId + "] => Guessed rename for smell: " + smell);
            logger.trace("[" + projectId + "]   => potential parent: " + original);
            commit.setRenamedSmell(original, smell);
            smell.parent = original;
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
        if (!underAnalysis.equals(Commit.empty())) {
            logger.debug("[" + projectId + "] ==> Persisting smells for commit: " + commit);
            insertSmellIntroductions(previous, commit);
            insertSmellRefactorings(previous, commit);
        }
    }

    /**
     * Persist all the introduction and refactoring, binding them to the table of Lost smells.
     *
     * @param commit The new commit.
     */
    private void persistLostChanges(Commit commit) {
        logger.debug("[" + projectId + "] ==> Handling lost commit from " + lostCommitOrdinal + " to " + commit.ordinal);
        insertLostSmellIntroductions(lostCommitOrdinal, commit.ordinal, previous, commit);
        insertLostSmellRefactorings(lostCommitOrdinal, commit.ordinal, previous, commit);
    }

    private void updateCommitTracking(Commit commit) {
        previous = underAnalysis;
        underAnalysis = commit;
    }
}
