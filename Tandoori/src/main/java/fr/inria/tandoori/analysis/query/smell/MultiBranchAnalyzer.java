package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;

import java.util.List;
import java.util.Map;

/**
 * Specialized branch analyzer which will fetch commit ordinal in the current branch
 * rather than in the 'commit_entry' table.
 */
public class MultiBranchAnalyzer extends BranchAnalyzer {
    private BranchQueries branchQueries;
    private int branchId;

    MultiBranchAnalyzer(int projectId, Persistence persistence, SmellDuplicationChecker duplicationChecker,
                        CommitQueries commitQueries, SmellQueries smellQueries, BranchQueries branchQueries, int branchId) {
        super(projectId, persistence, duplicationChecker, commitQueries, smellQueries);
        this.branchQueries = branchQueries;
        this.branchId = branchId;
    }

    @Override
    Commit createNoSmellCommit(int ordinal) throws CommitNotFoundException {
        List<Map<String, Object>> result = persistence.query(branchQueries.shaFromOrdinalQuery(projectId, branchId, ordinal));
        if (result.isEmpty()) {
            throw new CommitNotFoundException(projectId, ordinal);
        }
        return new Commit(String.valueOf(result.get(0).get("sha1")), ordinal);
    }
}
