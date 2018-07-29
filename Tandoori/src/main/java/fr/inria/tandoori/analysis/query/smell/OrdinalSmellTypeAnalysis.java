package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * Analyze a smell type only considering the commits ordinal.
 * This method may create too much {@link Smell} insertions and refactoring since we may have 2 branches
 * with commits mixed up in the ordinal (time based) commits ordering.
 *
 * @see <a href="https://git.evilantrules.xyz/antoine/test-git-log">https://git.evilantrules.xyz/antoine/test-git-log</a>
 */
public class OrdinalSmellTypeAnalysis extends AbstractSmellTypeAnalysis implements Query {
    private final Iterator<Map<String, Object>> smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    public OrdinalSmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
                                    String smellType, SmellDuplicationChecker duplicationChecker) {
        super(LoggerFactory.getLogger(OrdinalSmellTypeAnalysis.class.getName()), projectId, persistence);
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;
    }


    @Override
    public void query() throws QueryException {
        Smell smell;
        Commit commit;

        Map<String, Object> instance;
        BranchAnalyzer branchAnalyzer = new BranchAnalyzer(projectId, persistence, duplicationChecker);
        while (smells.hasNext()) {
            instance = smells.next();
            smell = Smell.fromPaprikaInstance(instance, smellType);
            commit = Commit.fromInstance(instance);
            branchAnalyzer.addSmellCommit(smell, commit);
        }

        branchAnalyzer.finalizeAnalysis();
    }
}
