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
            "WITH subquery AS (" + SmellDeduplicationQuery.QUERY_RENAMED_SMELLS + ") " +
                    "UPDATE SmellPresence SET " +
                    "smellId = 'subquery.oldId' " +
                    "WHERE smellId = 'subquery.newID' ";


    private static final String REMOVE_DUPLICATE_SMELLS =
            "DELETE FROM Smell " +
                    "WHERE id NOT IN (SELECT smellId FROM SmellPresence)";


    private static final String QUERY_RENAMED_SMELLS =
            "SELECT Smell.id as newId, Smell.file as newFile, oldFile, oldId, oldType FROM Smell \n" +
                    "\tINNER JOIN (\n" +
                    "\tselect FileRename.newFile as newFile, FileRename.oldFile as oldFile, oldId, oldType from FileRename \n" +
                    "\t\tINNER JOIN  (SELECT Smell.file as file, Smell.id as oldId, Smell.type as oldType FROM Smell) ON file = oldFile\n" +
                    "\t) ON file = newFile WHERE oldType = Smell.type";

}
