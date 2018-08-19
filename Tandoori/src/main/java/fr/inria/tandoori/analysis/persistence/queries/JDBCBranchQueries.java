package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;

public class JDBCBranchQueries extends JDBCQueriesHelper implements BranchQueries {
    private CommitQueries commitQueries;

    public JDBCBranchQueries(CommitQueries commitQueries) {
        this.commitQueries = commitQueries;
    }

    @Override
    public String branchInsertionStatement(int projectId, int ordinal, Commit parentCommit, Commit mergedInto) {
        String parentCommitQuery = parentCommit == null ? null : "(" + commitQueries.idFromShaQuery(projectId, parentCommit.sha) + ")";
        String mergedIntoQuery = mergedInto == null ? null : "(" + commitQueries.idFromShaQuery(projectId, mergedInto.sha) + ")";
        return "INSERT INTO branch (project_id, ordinal, parent_commit, merged_into) VALUES ('"
                + projectId + "', '" + ordinal + "', " + parentCommitQuery + ", " + mergedIntoQuery
                + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String branchCommitInsertionQuery(int projectId, int branchOrdinal, String commitSha, int ordinal) {
        return "INSERT INTO branch_commit (branch_id, commit_id, ordinal) VALUES (" +
                "(" + idFromOrdinalQueryStatement(projectId, branchOrdinal) + "), " +
                "(" + commitQueries.idFromShaQuery(projectId, commitSha) + "), " + ordinal + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromOrdinalQueryStatement(int projectId, int branchOrdinal) {
        return "SELECT id FROM branch WHERE project_id='" + projectId + "' AND ordinal=" + branchOrdinal;
    }

    public String idFromCommitQueryStatement(int projectId, Commit commit) {
        return "SELECT branch.id FROM branch " +
                "RIGHT JOIN branch_commit ON branch.id = branch_commit.branch_id " +
                "RIGHT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE commit_entry.sha1 = '" + commit.sha + "' AND commit_entry.project_id = '" + projectId + "'";
    }

    @Override
    public String parentCommitSmellsQuery(int projectId, int branchId, String smellType) {
        return commitSmellsQuery(projectId, parentCommitIdQuery(projectId, branchId), smellType);
    }

    @Override
    public String lastCommitSmellsQuery(int projectId, Commit merge, String smellType) {
        String branchId = "(" + mergedBranchIdQuery(projectId, merge) + ")";
        return commitSmellsQuery(projectId, branchLastCommitQuery(projectId, branchId, "id"), smellType);
    }


    @Override
    public String mergedBranchIdQuery(int projectId, Commit commit) {
        return "SELECT id FROM branch WHERE merged_into = (" + commitQueries.idFromShaQuery(projectId, commit.sha) + ")";
    }

    @Override
    public String shaFromOrdinalQuery(int projectId, int currentBranch, int ordinal) {
        String branchId = idFromOrdinalQueryStatement(projectId, currentBranch);
        return "SELECT sha1 FROM commit_entry " +
                "RIGHT JOIN branch_commit " +
                "ON branch_commit.commit_id = commit_entry.id " +
                "AND branch_commit.branch_id = (" + branchId + ") " +
                "AND branch_commit.ordinal = " + ordinal;
    }

    @Override
    public String parentCommitIdQuery(int projectId, int branchId) {
        return "SELECT parent_commit FROM branch where id = " + branchId + " AND project_id = " + projectId;
    }

    @Override
    public String lastCommitShaQuery(int projectId, int currentBranch) {
        return branchLastCommitQuery(projectId, currentBranch, "sha1");
    }

    @Override
    public String lastCommitIdQuery(int projectId, int currentBranch) {
        return branchLastCommitQuery(projectId, currentBranch, "id");
    }

    @Override
    public String commitOrdinalQuery(int projectId, int currentBranch, Commit commit) {
        return "SELECT branch_commit.ordinal FROM branch_commit " +
                "LEFT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE branch_commit.branch_id = " + currentBranch + " " +
                "AND commit_entry.sha1 = '" + commit.sha + "'";
    }

    /**
     * Helper method to fetch a last branch commit's commit_entry specific field.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @param field     The commit_entry field to retrieve. can be multiple coma separated fields.
     * @return The generated query statement.
     */
    private String branchLastCommitQuery(int projectId, int branchId, String field) {
        return branchLastCommitQuery(projectId, String.valueOf(branchId), field);
    }

    /**
     * Helper method to fetch a last branch commit's commit_entry specific field.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @param field     The commit_entry field to retrieve.
     * @return The generated query statement.
     */
    private String branchLastCommitQuery(int projectId, String branchId, String field) {
        return "SELECT commit_entry." + field + " FROM commit_entry " +
                "JOIN branch_commit " +
                "ON branch_commit.branch_id =  " + branchId + " " +
                "AND branch_commit.commit_id = commit_entry.id " +
                "WHERE commit_entry.project_id = " + projectId + " " +
                "ORDER BY commit_entry.ordinal DESC LIMIT 1";
    }

    /**
     * Helper method to fetch {@link Smell} instances for a specific commit identifier.
     * TODO: extract to {@link SmellQueries} interface.
     *
     * @param projectId     The project identifier.
     * @param commitIdQuery Query returning the commit identifier.
     * @param smellType     Filter the type of smells to retrieve.
     * @return The generated query statement.
     */
    private static String commitSmellsQuery(int projectId, String commitIdQuery, String smellType) {
        return "SELECT type, instance, file FROM smell " +
                "RIGHT JOIN smell_presence ON smell_presence.smell_id = smell.id " +
                "WHERE smell_presence.commit_id = (" + commitIdQuery + ") " +
                "AND smell.type = '" + smellType + "'";
    }
}
