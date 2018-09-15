package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.smell.duplication.SmellDuplicationChecker;
import neo4j.HashMapUsageQuery;
import neo4j.InitOnDrawQuery;
import neo4j.InvalidateWithoutRectQuery;
import neo4j.LICQuery;
import neo4j.MIMQuery;
import neo4j.NLMRQuery;
import neo4j.OverdrawQuery;
import neo4j.QueryEngine;
import neo4j.UnsuitedLRUCacheSizeQuery;
import neo4j.UnsupportedHardwareAccelerationQuery;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieve all the smells of a given project for each commits, through Paprika.
 */
public class SmellQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellQuery.class.getName());
    private final String paprikaDB;
    private final Persistence persistence;
    private final int projectId;
    private BranchQueries branchQueries;
    private SmellQueries smellQueries;
    private CommitQueries commitQueries;

    public SmellQuery(int projectId, String paprikaDB, Persistence persistence,
                      CommitQueries commitQueries, SmellQueries smellQueries, BranchQueries branchQueries) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.persistence = persistence;
        this.commitQueries = commitQueries;
        this.smellQueries = smellQueries;
        this.branchQueries = branchQueries;
    }

    private List<neo4j.Query> queries(QueryEngine queryEngine) {
        ArrayList<neo4j.Query> queries = new ArrayList<>();
        queries.add(MIMQuery.createMIMQuery(queryEngine));
        queries.add(LICQuery.createLICQuery(queryEngine));
        queries.add(NLMRQuery.createNLMRQuery(queryEngine));
        queries.add(OverdrawQuery.createOverdrawQuery(queryEngine));
        queries.add(UnsuitedLRUCacheSizeQuery.createUnsuitedLRUCacheSizeQuery(queryEngine));
        queries.add(InitOnDrawQuery.createInitOnDrawQuery(queryEngine));
        queries.add(UnsupportedHardwareAccelerationQuery.createUnsupportedHardwareAccelerationQuery(queryEngine));
        queries.add(HashMapUsageQuery.createHashMapUsageQuery(queryEngine));
        queries.add(InvalidateWithoutRectQuery.createInvalidateWithoutRectQuery(queryEngine));
        return queries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Smells insertion");
        QueryEngine queryEngine = new QueryEngine(paprikaDB);
        SmellDuplicationChecker duplicationChecker = new SmellDuplicationChecker(projectId, persistence);

        for (neo4j.Query query : queries(queryEngine)) {
            logger.info("[" + projectId + "] => Querying Smells of type: " + query.getSmellName());

            Result result = query.streamResult(true, true);
            logger.trace("[" + projectId + "]   ==> Found smells: " + result);

            new BranchAwareSmellTypeAnalysis(projectId, persistence, result, query.getSmellName(), duplicationChecker, commitQueries, smellQueries, branchQueries).query();

            // Calling commit for each smell type to avoid too big request.
            persistence.commit();
        }

        queryEngine.shutDown();
    }
}
