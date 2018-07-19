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

    /**
     * Query the identifier of a project.
     *
     * @param name Project name to look for.
     * @return The generated query statement.
     */
    String projectQueryStatement(String name);

    /**
     * Query the identifier of a commit.
     *
     * @param projectId Project to look into.
     * @param sha Commit sha.
     * @return The generated query statement.
     */
    String commitIdQueryStatement(int projectId, String sha);

    /**
     * Query the identifier of a commit.
     *
     * @param projectId Project to look into.
     * @param ordinal Commit ordinal in the project.
     * @return The generated query statement.
     */
    String commitSha1QueryStatement(int projectId, int ordinal);

    /**
     * Query the identifier of a developer.
     *
     * @param projectId Project to look into.
     * @param email Developer email.
     * @return The generated query statement.
     */
    String developerQueryStatement(int projectId, String email);


    /**
     * Query the identifier of a smell.
     *
     * @param projectId Project to look into.
     * @param instance Smell instance name.
     * @param type Smell type.
     * @param onlyLast Ensure that only the last matching smell is returned.
     * @return The generated query statement.
     */
    String smellQueryStatement(int projectId, String instance, String type, boolean onlyLast);


    /**
     * Query the identifier of a ProjectDeveloper.
     *
     * @param projectId Project to look into.
     * @param email Developer email.
     * @return The generated query statement.
     */
    String projectDevQueryStatement(int projectId, String email);

    /**
     * Returns the sha1 of the last project's commit.
     *
     * @param projectId Project to look into.
     * @return The generated query statement.
     */
    String lastProjectCommitSha1QueryStatement(int projectId);
    // TODO: insertion statement generation should be here to override behaviour per persistence basis.

    /**
     * Create a Branch insertion query.
     *
     * @param projectId Current project.
     * @param ordinal Branch ordinal.
     * @param master branch is the main branch.
     * @return The string to insert in persistence.
     */
    String branchInsertionStatement(int projectId, int ordinal, boolean master);

    /**
     * Create a BranchCommit insertion query.
     *
     * @param projectId Current project.
     * @param branchOrdinal Branch ordinal.
     * @param commitSha Sha1 of the commit to insert.
     * @return The string to insert in persistence.
     */
    String branchCommitInsertionQuery(int projectId, int branchOrdinal, String commitSha);

    /**
     * Query the identifier of a Branch.
     *
     * @param projectId Current project.
     * @param branchOrdinal Branch ordinal.
     * @return The string to insert in persistence.
     */
    String branchIdQueryStatement(int projectId, int branchOrdinal);
}
