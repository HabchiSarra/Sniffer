package fr.inria.tandoori.analysis.persistence;

import java.util.List;
import java.util.Map;

public interface Persistence {
    /**
     * Add the query statement to execute on the database.
     *
     * @param statements
     */
    void addStatements(String... statements);

    /**
     * Actually persist all the given statements and remove them from the buffer.
     */
    void commit();

    /**
     * Query the persistence with a specific statement.
     *
     * @param statement
     * @return
     */
    List<Map<String, Object>> query(String statement);

    /**
     * Close the database connection.
     */
    void close();

    /**
     * Initialize database schema if necessary.
     */
    void initialize();

    /**
     * Execute a statement modifying the database content, either INSERT, UPDATE or DELETE.
     *
     * @param statement The statement to execute.
     * @return -1 if an error occurred, 0 if no modification, the number of affected rows otherwise.
     */
    int execute(String statement);
}
