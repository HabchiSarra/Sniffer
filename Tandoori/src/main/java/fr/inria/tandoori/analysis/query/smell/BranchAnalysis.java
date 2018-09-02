package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.query.QueryException;

import java.util.List;

interface BranchAnalysis {
    /**
     * Add the {@link List} of {@link Smell} as already existing in the current branch.
     * This means that the smells will be considered as already introduced, and existing in the smell table.
     * For this we set them in both the previousCommitSmells and currentCommitSmells.
     *
     * @param smells The smells to add.
     */
    void addExistingSmells(List<Smell> smells);

    /**
     * Specifically add smells to the second branch of a merge commit.
     * This method is used for adding all smells before a merge commit occurs.
     *
     * @param smells The smells to add.
     */
    void addMergedSmells(List<Smell> smells);

    /**
     * Notify the current analyzed commit instance.
     *
     * @param commit The under analysis {@link Commit}, may be the same as before.
     */
    void notifyCommit(Commit commit);

    /**
     * Notify a new smell on the currently analyzed commit.
     *
     * @param smell The {@link Smell} instance.
     */
    void notifySmell(Smell smell);

    /**
     * Notify the end of smell analysis, the Branch analyzer should check for the last
     * commit sha and finalize the analysis.
     *
     * @throws QueryException If anything goes wrong during last commit retrieval.
     */
    void notifyEnd() throws QueryException;

    /**
     * Notify the end of smell analysis, using a specific commit sha.
     *
     * @param lastCommitSha1 The specific sha to use.
     */
    void notifyEnd(String lastCommitSha1);
}
