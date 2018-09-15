package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;

import java.util.Iterator;
import java.util.Map;

/**
 * Analyze a smell type only considering the commits ordinal.
 * This method may create too much {@link Smell} insertions and refactoring since we may have 2 branches
 * with commits mixed up in the ordinal (time based) commits ordering.
 *
 * @see <a href="https://git.evilantrules.xyz/antoine/test-git-log">https://git.evilantrules.xyz/antoine/test-git-log</a>
 */
class OrdinalSmellTypeAnalysis implements Query {

    // Analysis configuration
    private final int projectId;
    private String smellType;

    // Analysis data source
    private final Persistence persistence;
    private final CommitQueries commitQueries;
    private final SmellQueries smellQueries;
    private final SmellDuplicationChecker duplicationChecker;

    // Processed data
    private final Iterator<Map<String, Object>> smells;


    OrdinalSmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
                             String smellType, SmellDuplicationChecker duplicationChecker,
                             CommitQueries commitQueries, SmellQueries smellQueries) {
        this.projectId = projectId;
        this.persistence = persistence;
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;
        this.commitQueries = commitQueries;
        this.smellQueries = smellQueries;
    }


    @Override
    public void query() throws QueryException {
        Smell smell;
        Commit commit;

        Map<String, Object> instance;
        BranchAnalyzer branchAnalyzer = new BranchAnalyzer(
                projectId, persistence,
                duplicationChecker, commitQueries, smellQueries
        );
        while (smells.hasNext()) {
            instance = smells.next();
            smell = Smell.fromPaprikaInstance(instance, smellType);
            commit = Commit.fromInstance(instance);
            branchAnalyzer.notifyCommit(commit);
            branchAnalyzer.notifySmell(smell);
        }

        branchAnalyzer.notifyEnd();
    }
}
