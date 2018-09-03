package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.SmellCategory;

public class JDBCSmellQueries extends JDBCQueriesHelper implements SmellQueries {

    private CommitQueries commitQueries;

    public JDBCSmellQueries(CommitQueries commitQueries) {
        this.commitQueries = commitQueries;
    }

    @Override
    public String smellInsertionStatement(int projectId, Smell smell) {
        String parentQueryOrNull = null;
        // We know that the parent smell is the last inserted one.
        if (smell.parent != null) {
            parentQueryOrNull = "(" + smellIdQuery(projectId, smell.parent) + ")";
        }

        return "INSERT INTO smell (project_id, instance, type, file, renamed_from) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '"
                + smell.file + "', " + parentQueryOrNull + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category) {
        return "INSERT INTO " + category.getName() + " (project_id, smell_id, commit_id) VALUES " +
                "(" + projectId + ", (" + smellIdQuery(projectId, smell) + "), (" +
                commitQueries.idFromShaQuery(projectId, sha1) + "));";
    }

    @Override
    public String lostSmellCategoryInsertionStatement(int projectId, Smell smell, SmellCategory category, int since, int until) {
        String lostCategory = "lost_" + category.getName();
        return "INSERT INTO " + lostCategory + " (project_id, smell_id, since, until) VALUES " +
                "(" + projectId + ", (" + smellIdQuery(projectId, smell) +
                "), " + "" + since + " , " + until + ");";
    }

    @Override
    public String smellIdQuery(int projectId, Smell smell) {
        String parentQuery = smell.parent == null ? null : "(" + smellIdQuery(projectId, smell.parent) + ")";
        return smellIdQuery(projectId, smell.instance, smell.file, smell.type, parentQuery);
    }

    /**
     * Generate a query to fetch a smell by its unicity.
     *
     * @param projectId   Project to look into.
     * @param instance    The instance to look for.
     * @param file        The file to look for.
     * @param type        The type to look for.
     * @param renamedFrom The id or query (between parenthesis) of the renamed smell.
     *                    Look for null renamed_from if null.
     * @return
     */
    private String smellIdQuery(int projectId, String instance, String file, String type, String renamedFrom) {
        String statement = "SELECT id FROM smell " +
                "WHERE instance = '" + instance + "' " +
                "AND type = '" + type + "' " +
                "AND file = '" + file + "' " +
                "AND project_id = " + projectId;
        if (renamedFrom != null) {
            statement += " AND renamed_from = " + renamedFrom;
        } else {
            statement += " AND renamed_from IS NULL";
        }
        return statement;

    }

    @Override
    public String commitSmellsQuery(int projectId, String commitId, String smellType) {
        String query = "SELECT type, instance, file FROM smell " +
                "RIGHT JOIN smell_presence ON smell_presence.smell_id = smell.id " +
                "WHERE smell_presence.commit_id = " + commitId;
        if (smellType != null) {
            query += " AND smell.type = '" + smellType + "'";
        }
        return query;
    }
}
