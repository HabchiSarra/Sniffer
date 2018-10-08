package fr.inria.sniffer.tracker.analysis.persistence.queries;

public class JDBCDeveloperQueries extends JDBCQueriesHelper implements DeveloperQueries {
    @Override
    public String developerInsertStatement(String developerName) {
        return "INSERT INTO developer (username) VALUES ($$" + escapeStringEntry(developerName) + "$$) ON CONFLICT DO NOTHING;";
    }

    @Override
    public String projectDeveloperInsertStatement(int projectId, String developerName) {
        return "INSERT INTO project_developer (developer_id, project_id) VALUES (" +
                "(" + idFromEmailQuery(developerName) + "), " + projectId + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromEmailQuery(String email) {
        return "SELECT id FROM developer WHERE username = $$" + escapeStringEntry(email) + "$$";
    }

    @Override
    public String projectDeveloperQuery(int projectId, String email) {
        String devQuery = idFromEmailQuery(email);
        return "SELECT id FROM project_developer WHERE developer_id = (" + devQuery + ") AND project_id = " + projectId;
    }
}
