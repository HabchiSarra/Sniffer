package fr.inria.sniffer.tracker.analysis.persistence.queries;

public interface DeveloperQueries {
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
    String idFromEmailQuery(String email);

    /**
     * Query the identifier of a project_developer.
     *
     * @param projectId Project to look into.
     * @param email     Developer email.
     * @return The generated query statement.
     */
    String projectDeveloperQuery(int projectId, String email);
}
