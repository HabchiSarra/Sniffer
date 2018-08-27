package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.SmellCategory;

public interface SmellQueries {
    /**
     * @param projectId The project identifier.
     * @param smell     The smell instance to insert.
     * @return The generated insertion statement.
     */
    String smellInsertionStatement(int projectId, Smell smell);

    /**
     * Generate a statement inserting a {@link Smell} introduction, presence, or refactor into the persistence.
     *
     * @param projectId The project identifier.
     * @param sha1      Sha1 of the commit to bind the Smell category onto.
     * @param smell     The smell instance to insert.
     * @param category  The {@link SmellCategory} to insert the smell into.
     * @return The generated insertion statement.
     */
    String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category);

    /**
     * Generate a statement inserting a lost {@link Smell} introduction, or refactor into the persistence.
     *
     * @param projectId The project identifier.
     * @param smell     The smell instance to insert.
     * @param category  The {@link SmellCategory} to insert the smell into.
     * @param since     The lower ordinal of the interval in which the smell it lost.
     * @param until     The upper ordinal of the interval in which the smell it lost.
     * @return The generated insertion statement.
     */
    String lostSmellCategoryInsertionStatement(int projectId, Smell smell, SmellCategory category, int since, int until);

    /**
     * Query the identifier of a smell.
     *
     * @param projectId Project to look into.
     * @param instance  Smell instance name.
     * @param file      Smell file name.
     * @param type      Smell type.
     * @param onlyLast  Ensure that only the last matching smell is returned.
     * @return The generated query statement.
     */
    String smellIdQuery(int projectId, String instance, String file, String type, boolean onlyLast);

    /**
     * Query the {@link Smell} instances for a specific commit identifier.
     *
     * @param projectId The project identifier.
     * @param commitId  Commit identifier or query returning the commit identifier between parenthesis.
     * @param smellType Filter the type of smells to retrieve. Unused if null
     * @return The generated query statement.
     */
    String commitSmellsQuery(int projectId, String commitId, String smellType);
}
