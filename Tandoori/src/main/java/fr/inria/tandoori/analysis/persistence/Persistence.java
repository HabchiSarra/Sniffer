package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;
import fr.inria.tandoori.analysis.model.Smell;

import java.util.List;
import java.util.Map;

public interface Persistence {
    /**
     * Add the query statement to execute on the database.
     *
     * @param statements An array of statements to execute on {@link Persistence#commit()}.
     */
    void addStatements(String... statements);

    /**
     * Actually persist all the given statements and remove them from the buffer.
     */
    void commit();

    /**
     * Query the persistence with a specific statement.
     *
     * @param statement The query statement to execute.
     * @return Results from database as a {@link List} of {@link Map}, each list item being a row.
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
     * Generate a statement inserting the commit into the persistence.
     *
     * @param projectId The project identifier.
     * @param commit    The commit to insert.
     * @param diff      {@link GitDiff} for this commit.
     * @param ordinal   Commit ordinal in Paprika dataset.
     * @return The generated insertion statement.
     */
    String commitInsertionStatement(int projectId, Commit commit, GitDiff diff, int ordinal);

    /**
     * Query the identifier of a commit.
     *
     * @param projectId Project to look into.
     * @param sha       Commit sha.
     * @return The generated query statement.
     */
    String commitIdQueryStatement(int projectId, String sha);

    /**
     * Query the identifier of a commit.
     *
     * @param projectId Project to look into.
     * @param ordinal   Commit ordinal in the project.
     * @return The generated query statement.
     */
    String commitSha1QueryStatement(int projectId, int ordinal);


    /**
     * Generate a statement inserting the developer into the persistence.
     *
     * @param developerName The developer name
     * @return The generated insertion statement.
     */
    String developerInsertStatement(String developerName);

    /**
     * Generate a statement binding the developer to the project into the persistence.
     *
     * @param projectId     The project identifier.
     * @param developerName The developer name (must be in developer table).
     * @return The generated insertion statement.
     */
    String projectDeveloperInsertStatement(int projectId, String developerName);

    /**
     * Query the identifier of a developer.
     *
     * @param email Developer email.
     * @return The generated query statement.
     */
    String developerQueryStatement(String email);

    /**
     * @param projectId The project identifier.
     * @param smell     The smell instance to insert.
     * @return The generated insertion statement.
     */
    String smellInsertionStatement(int projectId, Smell smell);

    /**
     * @param projectId The project identifier.
     * @param sha1      Sha1 of the commit to bind the Smell category onto.
     * @param smell     The smell instance to insert.
     * @param category  The {@link SmellCategory} to insert the smell into.
     * @return The generated insertion statement.
     */
    String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category);

    /**
     * Query the identifier of a smell.
     *
     * @param projectId Project to look into.
     * @param instance  Smell instance name.
     * @param type      Smell type.
     * @param onlyLast  Ensure that only the last matching smell is returned.
     * @return The generated query statement.
     */
    String smellQueryStatement(int projectId, String instance, String type, boolean onlyLast);

    /**
     * Query the identifier of a ProjectDeveloper.
     *
     * @param projectId Project to look into.
     * @param email     Developer email.
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

    /**
     * Generate a statement inserting a {@link GitRename} into the persistence.
     *
     * @param projectId The project identifier.
     * @param commitSha Sha1 of the commit to link.
     * @param rename    {@link GitRename} instance to persist.
     * @return The generated insertion statement.
     */
    String fileRenameInsertionStatement(int projectId, String commitSha, GitRename rename);

    /**
     * Create a Branch insertion query.
     *
     * @param projectId Current project.
     * @param ordinal   Branch ordinal.
     * @param master    branch is the main branch.
     * @return The generated insertion statement.
     */
    String branchInsertionStatement(int projectId, int ordinal, boolean master);

    /**
     * Create a BranchCommit insertion query.
     *
     * @param projectId     Current project.
     * @param branchOrdinal Branch ordinal.
     * @param commitSha     Sha1 of the commit to insert.
     * @return The generated insertion statement.
     */
    String branchCommitInsertionQuery(int projectId, int branchOrdinal, String commitSha);

    /**
     * Query the identifier of a Branch.
     *
     * @param projectId     Current project.
     * @param branchOrdinal Branch ordinal.
     * @return The generated insertion statement.
     */
    String branchIdQueryStatement(int projectId, int branchOrdinal);
}
