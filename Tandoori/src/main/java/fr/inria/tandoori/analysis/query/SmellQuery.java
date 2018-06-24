package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
import neo4j.HashMapUsageQuery;
import neo4j.IGSQuery;
import neo4j.InitOnDrawQuery;
import neo4j.InvalidateWithoutRectQuery;
import neo4j.LICQuery;
import neo4j.MIMQuery;
import neo4j.NLMRQuery;
import neo4j.NoSmellsQuery;
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
    private Persistence persistence;
    private int projectId;

    public SmellQuery(int projectId, String db, Persistence persistence) {
        this.projectId = projectId;
        this.db = db;
        this.persistence = persistence;
    }

    private List<neo4j.Query> queries(QueryEngine queryEngine) {
        ArrayList<neo4j.Query> queries = new ArrayList<>();
        queries.add(IGSQuery.createIGSQuery(queryEngine));
        queries.add(MIMQuery.createMIMQuery(queryEngine));
        queries.add(LICQuery.createLICQuery(queryEngine));
        queries.add(NLMRQuery.createNLMRQuery(queryEngine));
        queries.add(OverdrawQuery.createOverdrawQuery(queryEngine));
        queries.add(UnsuitedLRUCacheSizeQuery.createUnsuitedLRUCacheSizeQuery(queryEngine));
        queries.add(InitOnDrawQuery.createInitOnDrawQuery(queryEngine));
        queries.add(UnsupportedHardwareAccelerationQuery.createUnsupportedHardwareAccelerationQuery(queryEngine));
        queries.add(HashMapUsageQuery.createHashMapUsageQuery(queryEngine));
        queries.add(InvalidateWithoutRectQuery.createInvalidateWithoutRectQuery(queryEngine));
        queries.add(new NoSmellsQuery(queryEngine));
        return queries;
    }

    @Override
    public void query() {
        boolean showDetails = true;
        QueryEngine queryEngine = new QueryEngine(db);

        for (neo4j.Query query : queries(queryEngine)) {
            logger.info("Querying Smells of type: " + query.getSmellName());
            List<Map<String, Object>> result = query.fetchResult(showDetails);
            logger.debug("Got result: " + result);
            writeResults(result, query.getSmellName());
        }
    }

    private void writeResults(List<Map<String, Object>> results, String smellName) {
        for (Map<String, Object> row : results) {
            String instance = (String) row.get("instance");
            Object commitSha = row.get("sha1");

            String smellQuery = "SELECT id FROM Smell WHERE instance = '" + instance +
                    "' AND type = '" + smellName + "'";
            String commitQuery = "SELECT id FROM `Commit` WHERE sha1 = '" + commitSha +
                    "' AND projectId = " + this.projectId;

            String smellInsert = "INSERT INTO Smell (instance, type) VALUES" +
                    "('" + instance + "', '" + smellName + "');";

            String smellPresenceInsert = "INSERT INTO SmellPresence (smellId, commitId) VALUES" +
                    "('" + smellQuery + "', (" + commitQuery + "));";
            persistence.addStatements(smellInsert, smellPresenceInsert);
        }
        persistence.commit();
    }
}
