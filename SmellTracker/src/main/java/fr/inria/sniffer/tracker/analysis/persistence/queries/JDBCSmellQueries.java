package fr.inria.sniffer.tracker.analysis.persistence.queries;

import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.SmellCategory;

public class JDBCSmellQueries extends JDBCQueriesHelper implements SmellQueries {

    private CommitQueries commitQueries;

    public JDBCSmellQueries(CommitQueries commitQueries) {
        this.commitQueries = commitQueries;
    }

    @Override
    public String smellInsertionStatement(int projectId, Smell smell) {
        String parentIdOrNull = smell.parent == null ? null : String.valueOf(smell.parent.id);

        return "INSERT INTO smell (project_id, instance, type, file, renamed_from) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '"
                + smell.file + "', " + parentIdOrNull + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category) {
        return "INSERT INTO " + category.getName() + " (project_id, smell_id, commit_id) VALUES " +
                "(" + projectId + ", " + smell.id + ", (" +
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
        String parentQuery = null;
        if (smell.parent != null) {
            if (smell.parent.id > -1) {
                parentQuery = String.valueOf(smell.parent.id);
            } else {
                parentQuery = "(" + smellIdQuery(projectId, smell.parent) + ")";
            }
        }
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
        String smellsQuery = "SELECT smell.id, type, instance, file, renamed_from FROM smell " +
                "RIGHT JOIN smell_presence ON smell_presence.smell_id = smell.id " +
                "WHERE smell_presence.commit_id = " + commitId;
        if (smellType != null) {
            smellsQuery += " AND smell.type = '" + smellType + "'";
        }

        String query = "WITH sm AS (" + smellsQuery + ") " +
                "SELECT sm.id, sm.type, sm.instance, sm.file, " +
                "parent.type AS parent_type, parent.instance AS parent_instance, parent.file AS parent_file " +
                "FROM sm " +
                "LEFT JOIN (SELECT id, type, instance, file FROM smell) parent " +
                "ON parent.id = sm.renamed_from";

        return query;
    }

    public String lastSmellIdQuery(int projectId) {
        return "SELECT id FROM smell " +
                "WHERE project_id = " + projectId + " " +
                "ORDER BY id DESC LIMIT 1";
    }
}
