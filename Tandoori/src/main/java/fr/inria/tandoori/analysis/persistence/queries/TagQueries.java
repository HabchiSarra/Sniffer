package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Tag;

public interface TagQueries {
    /**
     * Generate a statement inserting the {@link Tag} into the persistence.
     *
     * @param projectId The project identifier.
     * @param tag       The {@link Tag} to insert.
     * @return The generated insertion statement.
     */
    String tagInsertionStatement(int projectId, Tag tag);
}
