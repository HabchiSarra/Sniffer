package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Retrieve all the smells of a given project for each commits, through Paprika.
 */
public class SmellQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellQuery.class.getName());
    private final String db;
    private final Persistence persistence;
    private final int projectId;

    public SmellQuery(int projectId, String db, Persistence persistence) {
        this.projectId = projectId;
        this.db = db;
        this.persistence = persistence;
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
    public void query() {
        logger.info("[" + projectId + "] Starting Smells insertion");
        boolean showDetails = true;
        QueryEngine queryEngine = new QueryEngine(db);
        SmellDuplicationChecker duplicationChecker = new SmellDuplicationChecker(projectId, persistence);

        for (neo4j.Query query : queries(queryEngine)) {
            logger.info("[" + projectId + "] => Querying Smells of type: " + query.getSmellName());

            List<Map<String, Object>> result = query.fetchResult(showDetails);
            logger.trace("[" + projectId + "]   ==> Found smells: " + result);

            new SmellTypeAnalysis(projectId, persistence, result, query.getSmellName(), duplicationChecker).query();

            // Calling commit for each smell type to avoid too big request.
            persistence.commit();
        }

    }
}
