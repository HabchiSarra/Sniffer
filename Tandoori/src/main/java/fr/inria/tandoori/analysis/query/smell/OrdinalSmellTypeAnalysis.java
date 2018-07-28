package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Analyze a smell type only considering the commits ordinal.
 * This method may create too much {@link Smell} insertions and refactoring since we may have 2 branches
 * with commits mixed up in the ordinal (time based) commits ordering.
 *
 * @see <a href="https://git.evilantrules.xyz/antoine/test-git-log">https://git.evilantrules.xyz/antoine/test-git-log</a>
 */
public class OrdinalSmellTypeAnalysis extends AbstractSmellTypeAnalysis {
    private final Iterator<Map<String, Object>> smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    // Those attributes are the class state.
    private final List<Smell> previousCommitSmells;
    private final List<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;
    private int lostCommitOrdinal;

    public OrdinalSmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
                                    String smellType, SmellDuplicationChecker duplicationChecker) {
        super(LoggerFactory.getLogger(OrdinalSmellTypeAnalysis.class.getName()), projectId, persistence);
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;
        this.resetLostCommit();

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
        if (!commit.sha.equals(lastProjectSha1)) {
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
        logger.debug("[" + projectId + "] ==> Handling commit: " + commit);
        insertSmellIntroductions(commit, previousCommitSmells, currentCommitSmells, currentCommitRenamed);
        insertSmellRefactorings(commit, previousCommitSmells, currentCommitSmells, currentCommitOriginal);
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
