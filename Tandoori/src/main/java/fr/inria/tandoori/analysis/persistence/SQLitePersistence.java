package fr.inria.tandoori.analysis.persistence;

public class SQLitePersistence extends JDBCPersistence {

    public SQLitePersistence(String path) {
        super("sqlite", path, "/schema/tandoori-sqlite.sql");
    }
}
