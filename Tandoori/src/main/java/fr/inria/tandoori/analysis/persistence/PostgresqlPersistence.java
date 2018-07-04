package fr.inria.tandoori.analysis.persistence;

public class PostgresqlPersistence extends JDBCPersistence {
    public PostgresqlPersistence(String path, String username, String password) {
        super("postgresql", path, "/schema/tandoori-postgresql.sql", username, password);
    }
}
