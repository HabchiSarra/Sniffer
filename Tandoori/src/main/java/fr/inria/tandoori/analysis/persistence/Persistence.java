package fr.inria.tandoori.analysis.persistence;

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
     * Close the database connection.
     */
    void close();

    /**
     * Initialize database schema if necessary.
     */
    void initialize();
}
