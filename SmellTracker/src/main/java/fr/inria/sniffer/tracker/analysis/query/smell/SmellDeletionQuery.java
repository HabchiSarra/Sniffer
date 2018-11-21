package fr.inria.sniffer.tracker.analysis.query.smell;

import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.SmellQueries;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import neo4j.IsClassExistingQuery;
import neo4j.IsMethodExistingQuery;
import neo4j.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SmellDeletionQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellDeletionQuery.class.getName());
    private final String paprikaDB;
    private final Persistence persistence;
    private final int projectId;
    private final SmellQueries smellQueries;

    public SmellDeletionQuery(int projectId, String paprikaDB, Persistence persistence,
                              SmellQueries smellQueries) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.persistence = persistence;
        this.smellQueries = smellQueries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Smells deletion query");
        QueryEngine queryEngine = new QueryEngine(paprikaDB);

        String refactoredQuery = smellQueries.allRefactoredInstancesWithSha1(projectId);
        List<Map<String, Object>> result = persistence.query(refactoredQuery);

        int refactoringId;
        String instance;
        String type;
        String sha1;
        logger.info("[" + projectId + "] Found " + result.size() + " refactoring to analyze");

        int index = 0;
        for (Map<String, Object> refactored : result) {
            refactoringId = (int) refactored.get("id");
            sha1 = (String) refactored.get("sha1");
            instance = (String) refactored.get("instance");
            type = (String) refactored.get("type");
            logger.debug("[" + projectId + "] Checking smell: " + instance + " (" + type + ", " + sha1 + ")");

            try {
                boolean deleted = isDeleted(queryEngine, sha1, instance, type);
                logger.debug("[" + projectId + "] Setting smell as deleted: " + deleted + " - sha1: " + sha1
                        + " - instance: " + instance + " (" + type + ")");
                persistence.addStatements(smellQueries.setAsDeleted(projectId, refactoringId, deleted));
            } catch (QueryException e) {
                logger.warn("[" + projectId + "] Unable to set deleted", e);
            }

            if (++index % 1000 == 0) {
                logger.info("[" + projectId + "] Persisting smells deletion (" + index + ")");
                persistence.commit();
            }
        }
        persistence.commit();

        queryEngine.shutDown();
    }

    /**
     * Tells if the entity holding the smell has been deleted in the commit.
     *
     * @param queryEngine The Harissa's neo4j {@link QueryEngine}.
     * @param sha1        The commit sha1.
     * @param instance    The smell instance name, referencing its holding entity.
     * @param type        The smell type to check.
     * @return True if the smell has been deleted, False otherwise.
     */
    private boolean isDeleted(QueryEngine queryEngine, String sha1, String instance, String type) throws QueryException {
        neo4j.Query query;
        switch (type) {
            case "HMU":
            case "IOD":
            case "IWR":
            case "MIM":
            case "UCS":
            case "UHA":
            case "UIO":
                query = new IsMethodExistingQuery(queryEngine, sha1, instance);
                return query.fetchResult(false).isEmpty();
            case "LIC":
            case "NLMR":
                query = new IsClassExistingQuery(queryEngine, sha1, instance);
                return query.fetchResult(false).isEmpty();
        }
        throw new QueryException(logger.getName(), "Could not recognize smell type");
    }
}
