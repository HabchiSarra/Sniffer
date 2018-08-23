package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;

public interface CommitQueries {
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
     * Generate a statement inserting a {@link GitRename} into the persistence.
     *
     * @param projectId The project identifier.
     * @param commitSha Sha1 of the commit to link.
     * @param rename    {@link GitRename} instance to persist.
     * @return The generated insertion statement.
     */
    String fileRenameInsertionStatement(int projectId, String commitSha, GitRename rename);

    /**
     * Query the identifier of a commit.
     *
     * @param projectId Project to look into.
     * @param sha       Commit sha.
     * @return The generated query statement.
     */
    String idFromShaQuery(int projectId, String sha);

    /**
     * Query the sha1 of a commit.
     *
     * @param projectId Project to look into.
     * @param ordinal   Commit ordinal in the project.
     * @return The generated query statement.
     */
    String shaFromOrdinalQuery(int projectId, int ordinal);

    /**
     * Returns the sha1 of the last project's commit.
     *
     * @param projectId Project to look into.
     * @return The generated query statement.
     */
    String lastProjectCommitShaQuery(int projectId);
}