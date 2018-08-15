package fr.inria.tandoori.analysis.persistence.queries;

public class JDBCProjectQueries implements ProjectQueries {

    @Override
    public String idFromNameQuery(String name) {
        return "SELECT id FROM project WHERE name = '" + name + "'";
    }

}
