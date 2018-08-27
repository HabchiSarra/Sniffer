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
        // We know that the parent smell is the last inserted one.
        String parentSmellQuery = smellIdQuery(projectId, smell.parentInstance, smell.file, smell.type, true);
        String parentQueryOrNull = smell.parentInstance != null ? "(" + parentSmellQuery + ")" : null;

        return "INSERT INTO smell (project_id, instance, type, file, renamed_from) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '"
                + smell.file + "', " + parentQueryOrNull + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        return "INSERT INTO " + category.getName() + " (project_id, smell_id, commit_id) VALUES " +
                "(" + projectId + ", (" + smellIdQuery(projectId, smell.instance, smell.file, smell.type, true) + "), (" +
                commitQueries.idFromShaQuery(projectId, sha1) + "));";
    }

    @Override
    public String lostSmellCategoryInsertionStatement(int projectId, Smell smell, SmellCategory category, int since, int until) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        String lostCategory = "lost_" + category.getName();
        return "INSERT INTO " + lostCategory + " (project_id, smell_id, since, until) VALUES " +
                "(" + projectId + ", (" + smellIdQuery(projectId, smell.instance, smell.file, smell.type, true) +
                "), " + "" + since + " , " + until + ");";
    }

    @Override
    public String smellIdQuery(int projectId, String instance, String file, String type, boolean onlyLast) {
        String statement = "SELECT id FROM smell WHERE instance = '" + instance + "' " +
                "AND type = '" + type + "' AND file = '" + file + "' AND project_id = " + projectId;
        if (onlyLast) {
            statement += " ORDER BY id desc LIMIT 1";
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
