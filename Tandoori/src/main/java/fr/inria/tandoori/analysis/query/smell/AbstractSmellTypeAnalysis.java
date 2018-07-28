package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.AbstractQuery;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract query defining common behavior of
 * {@link fr.inria.tandoori.analysis.model.Smell} type analysis.
 */
abstract class AbstractSmellTypeAnalysis extends AbstractQuery {
    AbstractSmellTypeAnalysis(Logger logger, int projectId, Persistence persistence) {
        super(logger, projectId, persistence);
    }

    void insertSmellInstance(Smell smell) {
        persistence.addStatements(persistence.smellInsertionStatement(projectId, smell));
    }

    /**
     * Create a commit in case of a gap in the Paprika result set.
     * This means that all smells of the current type has been refactored.
     *
     * @param ordinal The missing commit ordinal.
     * @throws CommitNotFoundException if no commit exists for the given ordinal and project.
     */
    Commit createNoSmellCommit(int ordinal) throws CommitNotFoundException {
        List<Map<String, Object>> result = persistence.query(persistence.commitSha1QueryStatement(projectId, ordinal));
        if (result.isEmpty()) {
            throw new CommitNotFoundException(projectId, ordinal);
        }
        return new Commit(String.valueOf(result.get(0).get("sha1")), ordinal);
    }

    /**
     * Helper method adding Smell- -Presence, -Introduction, or -Refactor statement.
     *
     * @param smell    The smell to insert.
     * @param commit   The commit to insert into.
     * @param category The table category, either SmellPresence, SmellIntroduction, or SmellRefactor
     */
    void insertSmellInCategory(Smell smell, Commit commit, SmellCategory category) {
        persistence.addStatements(persistence.smellCategoryInsertionStatement(projectId, commit.sha, smell, category));
    }

    /**
     * Insert into persistence all smells introductions that has been lost in a commit gap.
     *
     * @param since                     The lower ordinal of the interval in which the smell it lost.
     * @param until                     The upper ordinal of the interval in which the smell it lost.
     * @param previousSmells            The list of smell present in last commit before gap.
     * @param currentSmells             The list of smell present in first commit after gap.
     * @param renamedSmellsNewInstances The list of *new* instances of {@link Smell}s identified as renamed.
     */
    void insertLostSmellIntroductions(int since, int until,
                                      List<Smell> previousSmells,
                                      List<Smell> currentSmells,
                                      List<Smell> renamedSmellsNewInstances) {
        for (Smell smell : getIntroduced(previousSmells, currentSmells, renamedSmellsNewInstances)) {
            insertLostSmellInCategory(smell, SmellCategory.INTRODUCTION, since, until);
        }
    }

    /**
     * Insert into persistence all smells refactorings that has been lost in a commit gap.
     *
     * @param since                          The lower ordinal of the interval in which the smell it lost.
     * @param until                          The upper ordinal of the interval in which the smell it lost.
     * @param previousSmells                 The list of smell present in last commit before gap.
     * @param currentSmells                  The list of smell present in first commit after gap.
     * @param renamedSmellsOriginalInstances The list of *old* instances of {@link Smell}s identified as renamed.
     */
    void insertLostSmellRefactorings(int since, int until,
                                     List<Smell> previousSmells,
                                     List<Smell> currentSmells,
                                     List<Smell> renamedSmellsOriginalInstances) {
        for (Smell smell : getRefactored(previousSmells, currentSmells, renamedSmellsOriginalInstances)) {
            insertLostSmellInCategory(smell, SmellCategory.REFACTOR, since, until);
        }
    }

    /**
     * Insert into persistence all smells introductions that happened between two commits.
     *
     * @param commit                    The new commit to bing those introductions onto.
     * @param previousSmells            The list of smells present in the previous commit.
     * @param currentSmells             The list of smells present in the current commit.
     * @param renamedSmellsNewInstances The list of *new* instances of {@link Smell}s identified as renamed.
     */
    void insertSmellIntroductions(Commit commit,
                                  List<Smell> previousSmells,
                                  List<Smell> currentSmells,
                                  List<Smell> renamedSmellsNewInstances) {
        for (Smell smell : getIntroduced(previousSmells, currentSmells, renamedSmellsNewInstances)) {
            insertSmellInCategory(smell, commit, SmellCategory.INTRODUCTION);
        }
    }

    /**
     * Insert into persistence all smells refactorings that happened between two commits.
     *
     * @param commit                         The new commit to bing those introductions onto.
     * @param previousSmells                 The list of smells present in the previous commit.
     * @param currentSmells                  The list of smells present in the current commit.
     * @param renamedSmellsOriginalInstances The list of *old* instances of {@link Smell}s identified as renamed.
     */
    void insertSmellRefactorings(Commit commit,
                                 List<Smell> previousSmells,
                                 List<Smell> currentSmells,
                                 List<Smell> renamedSmellsOriginalInstances) {
        for (Smell smell : getRefactored(previousSmells, currentSmells, renamedSmellsOriginalInstances)) {
            insertSmellInCategory(smell, commit, SmellCategory.REFACTOR);
        }
    }

    /**
     * Retrieve the list of introced commits from the list of smell presence of the previous and current commit.
     *
     * @param previous                  The list of smells present in the previous commit.
     * @param current                   The list of smells present in the current commit.
     * @param renamedSmellsNewInstances The list of *new* instances of {@link Smell}s identified as renamed.
     * @return The list of {@link Smell} introduced in the current commit.
     */
    private List<Smell> getIntroduced(List<Smell> previous, List<Smell> current, List<Smell> renamedSmellsNewInstances) {
        List<Smell> introduction = new ArrayList<>(current);
        introduction.removeAll(previous);
        introduction.removeAll(renamedSmellsNewInstances);
        return introduction;
    }

    /**
     * Retrieve the list of refactored commits from the list of smell presence of the previous and current commit.
     *
     * @param previous                       The list of smells present in the previous commit.
     * @param current                        The list of smells present in the current commit.
     * @param renamedSmellsOriginalInstances The list of *old* instances of {@link Smell}s identified as renamed.
     * @return The list of {@link Smell} refactored in the current commit.
     */
    private List<Smell> getRefactored(List<Smell> previous, List<Smell> current, List<Smell> renamedSmellsOriginalInstances) {
        List<Smell> refactoring = new ArrayList<>(previous);
        refactoring.removeAll(current);
        refactoring.removeAll(renamedSmellsOriginalInstances);
        return refactoring;
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
                persistence.lostSmellCategoryInsertionStatement(projectId, smell, category, since, until)
        );
    }
}