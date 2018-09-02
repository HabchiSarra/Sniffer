package fr.inria.tandoori.analysis.persistence.queries;

public interface ProjectQueries {

    /**
     * Generate a statement inserting the project into the persistence.
     *
     * @param projectName The project name.
     * @param url         The project url.
     * @return The generated insertion statement.
     */
    String projectInsertStatement(String projectName, String url);

    /**
     * Query the identifier of a project.
     *
     * @param name Project name to look for.
     * @return The generated query statement.
     */
    String idFromNameQuery(String name);
}
