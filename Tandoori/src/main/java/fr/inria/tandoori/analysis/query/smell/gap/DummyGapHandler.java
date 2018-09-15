package fr.inria.tandoori.analysis.query.smell.gap;

import fr.inria.tandoori.analysis.model.Commit;

/**
 * Use this {@link CommitGapHandler} implementation if you don't need gap handling.
 */
public class DummyGapHandler implements CommitGapHandler {
    private final int projectId;

    public DummyGapHandler(int projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean hasGap(Commit first, Commit second) {
        return false;
    }

    @Override
    public Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException {
        throw new CommitNotFoundException(projectId, previous.ordinal + 1);
    }

}
