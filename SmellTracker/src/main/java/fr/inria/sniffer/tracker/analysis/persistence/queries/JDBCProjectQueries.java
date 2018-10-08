package fr.inria.sniffer.tracker.analysis.persistence.queries;

public class JDBCProjectQueries implements ProjectQueries {

    @Override
    public String projectInsertStatement(String projectName, String url) {
        return "INSERT INTO Project (name, url) " +
                "VALUES ('" + projectName + "', '" + url + "') ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromNameQuery(String name) {
        return "SELECT id FROM project WHERE name = '" + name + "'";
    }

}
