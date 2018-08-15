package fr.inria.tandoori.analysis.persistence.queries;

public interface ProjectQueries {
    /**
     * Query the identifier of a project.
     *
     * @param name Project name to look for.
     * @return The generated query statement.
     */
    String idFromNameQuery(String name);
}
