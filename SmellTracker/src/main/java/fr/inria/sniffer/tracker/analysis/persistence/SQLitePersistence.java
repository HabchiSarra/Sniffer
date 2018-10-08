package fr.inria.sniffer.tracker.analysis.persistence;

public class SQLitePersistence extends JDBCPersistence {

    public SQLitePersistence(String path) {
        super("sqlite", path, "/schema/tracker-sqlite.sql");
    }

    public SQLitePersistence(String path, String schemaResourcePath) {
        super("sqlite", path, schemaResourcePath);
    }
}
