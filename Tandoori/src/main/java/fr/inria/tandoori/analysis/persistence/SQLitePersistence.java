package fr.inria.tandoori.analysis.persistence;

import java.io.File;

public class SQLitePersistence extends JDBCPersistence {

    public SQLitePersistence(String path) {
        super("sqlite", path, "/schema/tandoori-sqlite.sql");
    }

    public SQLitePersistence(String path, String schemaResourcePath) {
        super("sqlite", path, schemaResourcePath);
    }
}
