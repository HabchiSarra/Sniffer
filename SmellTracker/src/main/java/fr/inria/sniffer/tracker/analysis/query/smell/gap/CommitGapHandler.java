package fr.inria.sniffer.tracker.analysis.query.smell.gap;

import fr.inria.sniffer.tracker.analysis.model.Commit;

/**
 * Manage the gaps between analyzed commits.
 */
public interface CommitGapHandler {

    /**
     * Tells if the commit is not consecutive with the other commit.
     *
     * @param first  The first commit to test.
     * @param second The commit to test against the first.
     * @return True if the two commits ordinal are separated by more than 1, False otherwise.
     */
    boolean hasGap(Commit first, Commit second);

    /**
     * Try to retrieve the next commit in our database in case of a {@link Commit}
     * with no smell at all in Paprika.
     * <p>
     * If the commit is found, this means that all smells of the current type has been refactored.
     * If the commit is NOt found, this means that we had an error in the Paprika analysis.
     *
     * @param previous The previous commit.
     * @throws CommitNotFoundException if no commit exists for the given ordinal and project.
     */
    Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException;

}
