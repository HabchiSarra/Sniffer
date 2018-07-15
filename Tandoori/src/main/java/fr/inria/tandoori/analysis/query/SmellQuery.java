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
    private final List<Smell> previousCommitSmells;
    private final List<Smell> currentCommitSmells;
    private final List<Smell> currentCommitOriginal;
    private final List<Smell> currentCommitRenamed;
    private String currentSha;

    public SmellQuery(int projectId, String db, Persistence persistence) {
        this.projectId = projectId;
        this.db = db;
        this.persistence = persistence;
        previousCommitSmells = new ArrayList<>();
        currentCommitSmells = new ArrayList<>();
        currentCommitOriginal = new ArrayList<>();
        currentCommitRenamed = new ArrayList<>();
        currentSha = "";
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
            // TODO: Use another class to manage per smell type query.
            logger.info("[" + projectId + "] => Querying Smells of type: " + query.getSmellName());
            List<Map<String, Object>> result = query.fetchResult(showDetails);

            logger.trace("[" + projectId + "]   ==> Found smells: " + result);
            writeResults(result, query.getSmellName(), duplicationChecker);

            // Calling commit for each smell type to avoid too big request.
            persistence.commit();
        }

    }

    private void writeResults(List<Map<String, Object>> results, String smellName, SmellDuplicationChecker duplicationChecker) {
        Smell currentSmell;
        for (Map<String, Object> instance : results) {
            // We keep track of the smells present in our commit.
            currentSmell = Smell.fromInstance(instance, smellName);
            if (!currentSha.equals(currentSmell.commitSha)) {
                changeCurrentCommit(currentSmell.commitSha);
            }
            currentCommitSmells.add(currentSmell);

            /* In this test we have a smell with its file directing to a newly renamed file.
             * We will have to guess the previous smell instance by rewriting its instance id with the file before being renamed.
             * This instance ID will then be compared with the list of instances from the previous smell to find a match.
             */
            Smell original = duplicationChecker.original(currentSmell);
            // If we correctly guessed the smell identifier, we will find it in the previous commit smells
            if (original != null && previousCommitSmells.contains(original)) {
                logger.debug("[" + projectId + "] => Guessed rename for smell: " + currentSmell);
                logger.trace("[" + projectId + "]   => potential parent: " + original);
                currentCommitOriginal.add(original);
                currentCommitRenamed.add(currentSmell);
                currentSmell.parentInstance = original.instance;
            }
            if (!previousCommitSmells.contains(currentSmell)) {
                insertSmellInstance(currentSmell);
            }
            insertSmellPresence(currentSmell);
        }

        // We clean all data, it would be better to not use global state...x
        currentCommitOriginal.clear();
        currentCommitRenamed.clear();
        previousCommitSmells.clear();
        currentCommitSmells.clear();
    }

    private void insertSmellInstance(Smell smell) {
        // We know that the parent smell is the last inserted one.
        String parentSmellQuery = persistence.smellQueryStatement(projectId, smell.parentInstance, smell.type, true);
        String parentQuery = smell.parentInstance != null ? "(" + parentSmellQuery + ")" : null;
        String smellInsert = "INSERT INTO Smell (projectId, instance, type, file, renamedFrom) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '" + smell.file + "', " + parentQuery + ") ON CONFLICT DO NOTHING;";
        persistence.addStatements(smellInsert);
    }

    private void insertSmellPresence(Smell smell) {
        insertSmellInCategory(smell, "SmellPresence");
    }

    private void insertSmellInCategory(Smell smell, String category) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        String smellQuery = persistence.smellQueryStatement(projectId, smell.instance, smell.type, true);
        String commitQuery = persistence.commitQueryStatement(this.projectId, smell.commitSha);
        String smellPresenceInsert = "INSERT INTO " + category + " (smellId, commitId) VALUES " +
                "((" + smellQuery + "), (" + commitQuery + "));";
        persistence.addStatements(smellPresenceInsert);

    }

    /**
     * Transfer all smells from current commit to the previous one, and change the current sha.
     *
     * @param commitSha The new sha.
     */
    private void changeCurrentCommit(String commitSha) {
        logger.debug("[" + projectId + "] ==> Handling commit: " + commitSha);
        if (logger.isTraceEnabled()) {
            traceCommitIdentifier(commitSha);
        }
        insertSmellIntroductions();
        insertSmellRefactoring();

        currentSha = commitSha;
        currentCommitOriginal.clear();
        currentCommitRenamed.clear();
        previousCommitSmells.clear();
        previousCommitSmells.addAll(currentCommitSmells);
        currentCommitSmells.clear();
    }

    /**
     * Trace the identifier of the currently analyzed commit.
     *
     * @param commitSha The sha to print ID for.
     */
    private void traceCommitIdentifier(String commitSha) {
        String commitQuery = persistence.commitQueryStatement(this.projectId, commitSha);
        List<Map<String, Object>> result = persistence.query(commitQuery);
        if (!result.isEmpty()) {
            logger.trace("[" + projectId + "]  => commit id: " + String.valueOf(result.get(0).get("id")));
        } else {
            logger.trace("[" + projectId + "] NO FOUND COMMIT!");
        }
    }

    private void insertSmellIntroductions() {
        List<Smell> introduction = new ArrayList<>(currentCommitSmells);
        introduction.removeAll(previousCommitSmells);

        for (Smell smell : introduction) {
            if (!currentCommitRenamed.contains(smell)) {
                insertSmellInCategory(smell, "SmellIntroduction");
            }
        }
    }

    private void insertSmellRefactoring() {
        List<Smell> refactoring = new ArrayList<>(previousCommitSmells);
        refactoring.removeAll(currentCommitSmells);

        for (Smell smell : refactoring) {
            if (!currentCommitOriginal.contains(smell)) {
                insertSmellInCategory(smell, "SmellRefactor");
            }
        }
    }
}
