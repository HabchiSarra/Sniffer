package fr.inria.tandoori.analysis.query.smell.gap;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;

import java.util.List;
import java.util.Map;

public class SingleBranchGapHandler implements CommitGapHandler {
    private final Persistence persistence;
    private final CommitQueries commitQueries;
    private final int projectId;

    public SingleBranchGapHandler(int projectId, Persistence persistence, CommitQueries commitQueries) {
        this.persistence = persistence;
        this.commitQueries = commitQueries;
        this.projectId = projectId;
    }

    @Override
    public boolean hasGap(Commit first, Commit second) {
        return Math.abs(second.getOrdinal() - first.getOrdinal()) > 1;
    }

    @Override
    public Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException {
        int ordinal = previous.getOrdinal() + 1;
        String statement = commitQueries.shaFromOrdinalQuery(projectId, ordinal);
        List<Map<String, Object>> result = persistence.query(statement);
        if (result.isEmpty()) {
            throw new CommitNotFoundException(projectId, ordinal);
        }
        return new Commit(String.valueOf(result.get(0).get("sha1")), ordinal);
    }

}
