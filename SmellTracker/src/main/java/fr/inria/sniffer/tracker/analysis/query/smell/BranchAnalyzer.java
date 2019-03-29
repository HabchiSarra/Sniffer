/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.inria.sniffer.tracker.analysis.query.smell;

import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.queries.SmellQueries;
import fr.inria.sniffer.tracker.analysis.query.PersistenceAnalyzer;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import fr.inria.sniffer.tracker.analysis.query.smell.gap.SingleBranchGapHandler;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.SmellCategory;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.query.smell.duplication.SmellDuplicationChecker;
import fr.inria.sniffer.tracker.analysis.query.smell.gap.CommitGapHandler;
import fr.inria.sniffer.tracker.analysis.query.smell.gap.CommitNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Analyze the smell introduction/presence/refactoring for a given branch.
 */
class BranchAnalyzer extends PersistenceAnalyzer implements BranchAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(BranchAnalyzer.class.getName());

    // Analyzer Configuration
    private final CommitGapHandler gapHandler;

    // Analyzer data source
    private final SmellQueries smellQueries;
    private final SmellDuplicationChecker duplicationChecker;

    // Those attributes are the class state.
    private Commit previous;
    private Commit underAnalysis;
    private int lostCommitOrdinal;

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                   CommitQueries commitQueries, SmellQueries smellQueries) {
        this(projectId, persistence, duplicationChecker, commitQueries, smellQueries, new SingleBranchGapHandler(projectId, persistence, commitQueries));
    }

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                   CommitQueries commitQueries, SmellQueries smellQueries,
                   CommitGapHandler gapHandler) {
        this(projectId, persistence, duplicationChecker, commitQueries, smellQueries, gapHandler, null);
    }

    BranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                   CommitQueries commitQueries, SmellQueries smellQueries,
                   CommitGapHandler gapHandler, String parentCommitSha) {
        super(logger, projectId, persistence, commitQueries);
        this.duplicationChecker = duplicationChecker;
        this.smellQueries = smellQueries;
        this.gapHandler = gapHandler;

        previous = Commit.empty();
        if (parentCommitSha != null) {
            underAnalysis = new Commit(parentCommitSha, -1);
        } else {
            underAnalysis = Commit.empty();
        }
        this.resetLostCommit();
    }

    /**
     * Tells if the given commit is a dummy placeholder commit.
     *
     * @param commit The commit to test.
     * @return True if it is a placeholder, false otherwise.
     */
    private boolean isEmptyCommit(Commit commit) {
        return commit.ordinal == -1;
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
            if (!isEmptyCommit(underAnalysis)) {
                handleCommitChanges(underAnalysis);
            }
            // Compare the two commits ordinal to find a gap.
            if (gapHandler.hasGap(underAnalysis, commit) && !isEmptyCommit(underAnalysis)) {
                handleCommitGap();
            }
            updateCommitTracking(commit);
            logger.debug("[" + projectId + "] => Now analysing commit: " + underAnalysis);
        }
    }

    @Override
    public void notifySmell(Smell smell) {
        smell = fetchIdentifiedSmell(smell);

        if (smell.id == -1) {
            // If smell did not exists in previous commit, we try to
            // update the smell with parent instance, if any
            handleSmellRename(smell, underAnalysis);
        }

        // Check if we already inserted smell previously to avoid having too much insert statements.
        // This could be removed and still checked by our unicity constraint.
        if (isNew(smell)) {
            smell.id = insertSmellInstance(smell);
        }
        assert smell.id != -1;

        insertSmellInCategory(smell, underAnalysis, SmellCategory.PRESENCE);

        // We keep track of the smells present in our commit.
        underAnalysis.addSmell(smell);
    }

    /**
     * Retrieve the same smell previously present on his parents
     * with identifier and parents
     *
     * @param temporary The smell to retrieve.
     * @return The original, identified smell.
     */
    private Smell fetchIdentifiedSmell(Smell temporary) {
        Smell strippedTemporary = Smell.copyWithoutParent(temporary);
        Smell original = previous.getPreviousInstance(strippedTemporary);
        if (original.id == -1) {
            original = underAnalysis.getMergedInstance(strippedTemporary);
        }
        return original.id > -1 ? original : temporary;
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
        return smell.id == -1;
    }

    @Override
    public void notifyEnd() throws QueryException {
        notifyEnd(fetchLastProjectCommitSha());
    }

    @Override
    public void notifyEnd(String lastCommitSha1) {
        if (isEmptyCommit(underAnalysis)) {
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
            // The ordinal is unused here, so we can safely increment value
            try {
                updateCommitTracking(gapHandler.fetchNoSmellCommit(underAnalysis));
            } catch (CommitNotFoundException e) {
                logger.warn("[" + projectId + "] Unable to fetch last branch commit: " + e.getMessage());
            }
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
        try {
            Commit emptyCommit = gapHandler.fetchNoSmellCommit(underAnalysis);
            // If we found the gap commit, we insert it as any other before continuing
            updateCommitTracking(emptyCommit);
            persistCommitChanges(emptyCommit);
        } catch (CommitNotFoundException e) {
            logger.warn("An error occurred while treating gap, inserting in lost smells: " + e.getMessage());
            setLostCommit(e.getOrdinal());
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
        Smell parent = duplicationChecker.original(smell, commit, previous);

        // We found a file renamed, hence a potential renamed smell's parent
        if (parent != null) {
            logger.info("[" + projectId + "] => Guessed rename for smell: " + smell);
            // We try to find the original smell from this parent.
            Smell originalParent = fetchIdentifiedSmell(parent);
            if (originalParent.id > -1) {
                logger.info("[" + projectId + "]   => Found parent smell: " + originalParent);
                smell.parent = originalParent;
                commit.setRenamedSmell(smell.parent, smell);
            } else {
                logger.warn("[" + projectId + "]   => Could not find original smell for parent: " + parent);
            }
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
        if (!isEmptyCommit(underAnalysis)) {
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
        logger.debug("[" + projectId + "] ==> Handling lost commit from " + lostCommitOrdinal + " to " + commit.getOrdinal());
        insertLostSmellIntroductions(lostCommitOrdinal, commit.getOrdinal(), previous, commit);
        insertLostSmellRefactorings(lostCommitOrdinal, commit.getOrdinal(), previous, commit);
    }

    private void updateCommitTracking(Commit commit) {
        previous = underAnalysis;
        underAnalysis = commit;
    }

    private int insertSmellInstance(Smell smell) {
        int insertResult = persistence.execute(smellQueries.smellInsertionStatement(projectId, smell));
        List<Map<String, Object>> result;
        if (insertResult == 1) {
            result = persistence.query(smellQueries.lastSmellIdQuery(projectId));
        } else {
            result = persistence.query(smellQueries.smellIdQuery(projectId, smell));
        }
        return (int) result.get(0).get("id");
    }

    /**
     * Helper method adding Smell- -Presence, -Introduction, or -Refactor statement.
     *
     * @param smell    The smell to insert.
     * @param commit   The commit to insert into.
     * @param category The table category, either SmellPresence, SmellIntroduction, or SmellRefactor
     */
    private void insertSmellInCategory(Smell smell, Commit commit, SmellCategory category) {
        persistence.addStatements(smellQueries.smellCategoryInsertionStatement(projectId, commit.sha, smell, category));
    }

    /**
     * Insert into persistence all smells introductions that has been lost in a commit gap.
     *
     * @param since    The lower ordinal of the interval in which the smell it lost.
     * @param until    The upper ordinal of the interval in which the smell it lost.
     * @param previous The previous {@link Commit}.
     * @param current  The current {@link Commit}.
     */
    private void insertLostSmellIntroductions(int since, int until, Commit previous, Commit current) {
        for (Smell smell : current.getIntroduced(previous)) {
            insertLostSmellInCategory(smell, SmellCategory.INTRODUCTION, since, until);
        }
    }

    /**
     * Insert into persistence all smells refactorings that has been lost in a commit gap.
     *
     * @param since    The lower ordinal of the interval in which the smell it lost.
     * @param until    The upper ordinal of the interval in which the smell it lost.
     * @param previous The previous {@link Commit}.
     * @param current  The current {@link Commit}.
     */
    private void insertLostSmellRefactorings(int since, int until, Commit previous, Commit current) {
        for (Smell smell : current.getRefactored(previous)) {
            insertLostSmellInCategory(smell, SmellCategory.REFACTOR, since, until);
        }
    }

    /**
     * Insert into persistence all smells introductions that happened between two commits.
     *
     * @param previous The previous {@link Commit}.
     * @param current  The current {@link Commit}.
     */
    private void insertSmellIntroductions(Commit previous, Commit current) {
        for (Smell smell : current.getIntroduced(previous)) {
            insertSmellInCategory(smell, current, SmellCategory.INTRODUCTION);
        }
    }

    /**
     * Insert into persistence all smells refactorings that happened between two commits.
     *
     * @param previous The previous {@link Commit}.
     * @param current  The current {@link Commit}.
     */
    private void insertSmellRefactorings(Commit previous, Commit current) {
        for (Smell smell : current.getRefactored(previous)) {
            insertSmellInCategory(smell, current, SmellCategory.REFACTOR);
        }
    }

    /**
     * Helper method adding Smell-, -Introduction, or -Refactor statement for lost commits.
     *
     * @param smell    The smell to insert.
     * @param since    The lower ordinal of the interval in which the smell it lost.
     * @param until    The upper ordinal of the interval in which the smell it lost.
     * @param category The table category, either SmellPresence, SmellIntroduction, or SmellRefactor
     */
    private void insertLostSmellInCategory(Smell smell, SmellCategory category, int since, int until) {
        persistence.addStatements(
                smellQueries.lostSmellCategoryInsertionStatement(projectId, smell, category, since, until)
        );
    }
}
