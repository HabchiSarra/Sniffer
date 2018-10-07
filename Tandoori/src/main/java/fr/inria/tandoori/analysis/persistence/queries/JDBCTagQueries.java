package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Tag;

public class JDBCTagQueries implements TagQueries {

    private CommitQueries commitQueries;

    public JDBCTagQueries(CommitQueries commitQueries) {
        this.commitQueries = commitQueries;
    }

    @Override
    public String tagInsertionStatement(int projectId, Tag tag) {
        String commitId = "(" + commitQueries.idFromShaQuery(projectId, tag.getSha()) + ")";
        return "INSERT INTO tag (project_id, commit_id, name, date) " +
                "VALUES " +
                "(" + projectId + ", " + commitId + ",  '"
                + tag.getName() + "', '" + tag.getDate() + "') " +
                "ON CONFLICT DO NOTHING";
    }
}
