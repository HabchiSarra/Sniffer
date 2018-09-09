package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.PersistenceAnalyzer;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Abstract query defining common behavior of
 * {@link fr.inria.tandoori.analysis.model.Smell} type analysis.
 */
abstract class AbstractSmellTypeAnalysis extends PersistenceAnalyzer {
    protected final SmellQueries smellQueries;

    AbstractSmellTypeAnalysis(Logger logger, int projectId, Persistence persistence,
                              CommitQueries commitQueries, SmellQueries smellQueries) {
        super(logger, projectId, persistence, commitQueries);
        this.smellQueries = smellQueries;
    }

    int insertSmellInstance(Smell smell) {
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
     * Create a commit in case of a gap in the Paprika result set.
     * This means that all smells of the current type has been refactored.
     *
     * @param ordinal The missing commit ordinal.
     * @throws CommitNotFoundException if no commit exists for the given ordinal and project.
     */
    Commit createNoSmellCommit(int ordinal) throws CommitNotFoundException {
        List<Map<String, Object>> result = persistence.query(commitQueries.shaFromOrdinalQuery(projectId, ordinal));
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
    void insertLostSmellIntroductions(int since, int until, Commit previous, Commit current) {
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
    void insertLostSmellRefactorings(int since, int until, Commit previous, Commit current) {
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
    void insertSmellIntroductions(Commit previous, Commit current) {
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
    void insertSmellRefactorings(Commit previous, Commit current) {
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