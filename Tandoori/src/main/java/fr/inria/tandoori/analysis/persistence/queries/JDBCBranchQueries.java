package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Commit;

public class JDBCBranchQueries extends JDBCQueriesHelper implements BranchQueries {
    private CommitQueries commitQueries;
    private SmellQueries smellQueries;

    public JDBCBranchQueries(CommitQueries commitQueries, SmellQueries smellQueries) {
        this.commitQueries = commitQueries;
        this.smellQueries = smellQueries;
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
                "LEFT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE commit_entry.sha1 = '" + commit.sha + "' AND commit_entry.project_id = '" + projectId + "'";
    }

    @Override
    public String parentCommitSmellsQuery(int projectId, int branchId, String smellType) {
        return smellQueries.commitSmellsQuery(projectId, "(" + parentCommitIdQuery(projectId, branchId) + ")", smellType);
    }

    @Override
    public String lastCommitSmellsQuery(int projectId, Commit merge, String smellType) {
        String branchId = "(" + mergedBranchIdQuery(projectId, merge) + ")";
        return smellQueries.commitSmellsQuery(projectId, "(" + branchLastCommitQuery(projectId, branchId, "id") + ")", smellType);
    }


    @Override
    public String mergedBranchIdQuery(int projectId, Commit commit) {
        return "SELECT id FROM branch WHERE merged_into = (" + commitQueries.idFromShaQuery(projectId, commit.sha) + ")";
    }

    @Override
    public String shaFromOrdinalQuery(int projectId, int branchId, int ordinal) {
        return shaFromOrdinalQuery(projectId, branchId, ordinal, false);
    }

    @Override
    public String shaFromOrdinalQuery(int projectId, int branchId, int ordinal, boolean paprikaOnly) {
        // TODO: Be sure of this request
        String query = "SELECT sha1 FROM commit_entry " +
                "JOIN branch_commit " +
                "ON branch_commit.commit_id = commit_entry.id " +
                "AND branch_commit.branch_id = (" + branchId + ") " +
                "AND branch_commit.ordinal = " + ordinal;
        if (paprikaOnly) {
            query += " AND commit_entry.in_paprika IS TRUE";
        }
        return query;
    }

    @Override
    public String parentCommitIdQuery(int projectId, int branchId) {
        return "SELECT parent_commit AS id FROM branch where id = " + branchId + " AND project_id = " + projectId;
    }

    @Override
    public String lastCommitShaQuery(int projectId, int branchId) {
        return branchLastCommitQuery(projectId, branchId, "sha1");
    }

    @Override
    public String lastCommitIdQuery(int projectId, int branchId) {
        return branchLastCommitQuery(projectId, branchId, "id");
    }

    @Override
    public String commitOrdinalQuery(int projectId, int branchId, Commit commit) {
        return "SELECT branch_commit.ordinal FROM branch_commit " +
                "LEFT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE branch_commit.branch_id = " + branchId + " " +
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
        String last_branch_commit_id = "SELECT commit_id FROM branch_commit " +
                "WHERE branch_id =  " + branchId + " " +
                "ORDER BY ordinal DESC LIMIT 1";

        return "SELECT commit_entry." + field + " FROM commit_entry " +
                "JOIN (" + last_branch_commit_id + ") AS bc " +
                "ON bc.commit_id = commit_entry.id " +
                "WHERE commit_entry.project_id = " + projectId;
    }
}
