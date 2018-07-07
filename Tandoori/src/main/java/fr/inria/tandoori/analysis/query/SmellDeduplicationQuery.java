package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;

public class SmellDeduplicationQuery implements Query {
    private final Persistence persistence;

    public SmellDeduplicationQuery(Persistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public void query() throws QueryException {
        int result = 1;
        // We update the renamed smells until we are sure to only
        // have original smells inside SmellPresence
        while (result > 0) {
            result = persistence.execute(MERGE_SMELL_PRESENCE);
        }
        persistence.execute(REMOVE_DUPLICATE_SMELLS);
    }

    private static final String MERGE_SMELL_PRESENCE =
            "UPDATE SmellPresence SET " +
                    "smellId = subquery.oldId" +
                    "WHERE smellId = subquery.newID" +
                    "FROM (" + SmellDeduplicationQuery.QUERY_RENAMED_SMELLS + ") AS subquery";


    private static final String REMOVE_DUPLICATE_SMELLS =
            "DELETE FROM Smell " +
                    "WHERE id NOT IN (SELECT smellId FROM SmellPresence)";


    private static final String QUERY_RENAMED_SMELLS =
            "SELECT id as oldId, newId " +
                    "JOIN (" +
                    "   FileRename" +
                    "   JOIN Smell " +
                    "       ON file = oldFile" +
                    "   AND oldFile in (SELECT file FROM Smell)" +
                    ")" +
                    "ON file = newFile";

}
