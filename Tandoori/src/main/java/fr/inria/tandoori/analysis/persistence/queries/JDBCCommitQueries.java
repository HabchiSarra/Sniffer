package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;

public class JDBCCommitQueries extends JDBCQueriesHelper implements CommitQueries {

    private DeveloperQueries developerQueries;

    public JDBCCommitQueries(DeveloperQueries developerQueries) {
        this.developerQueries = developerQueries;
    }

    @Override
    public String commitInsertionStatement(int projectId, Commit commit, GitDiff diff, int ordinal) {
        logger.trace("[" + projectId + "] Inserting commit: " + commit.sha
                + " - ordinal: " + ordinal + " - diff: " + diff + " - time: " + commit.date);

        // Escaping double dollars to avoid exiting dollar quoted string too soon.
        String commitMessage = escapeStringEntry(commit.message);

        String developerQuery = developerQueries.idFromEmailQuery(commit.authorEmail);
        return "INSERT INTO commit_entry (project_id, developer_id, sha1, ordinal, date, additions, deletions, files_changed, message) VALUES ('" +
                projectId + "', (" + developerQuery + "), '" + commit.sha + "', " + ordinal + ", '" + commit.date.toString() +
                "', " + diff.getAddition() + ", " + diff.getDeletion() + ", " + diff.getChangedFiles() +
                ", $$" + commitMessage + "$$) ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromShaQuery(int projectId, String sha) {
        return "SELECT id FROM commit_entry WHERE sha1 = '" + sha + "' AND project_id = " + projectId;
    }

    @Override
    public String shaFromOrdinalQuery(int projectId, int ordinal) {
        return "SELECT sha1 FROM commit_entry WHERE ordinal = '" + ordinal + "' AND project_id = " + projectId;
    }

    @Override
    public String lastProjectCommitShaQuery(int projectId) {
        return "SELECT sha1 FROM commit_entry WHERE project_id = '" + projectId + "' ORDER BY ordinal DESC LIMIT 1";
    }

    @Override
    public String fileRenameInsertionStatement(int projectId, String commitSha, GitRename rename) {
        return "INSERT INTO file_rename (project_id, commit_id, old_file, new_file, similarity) VALUES ('" +
                projectId + "', (" + idFromShaQuery(projectId, commitSha) + "), '" + rename.oldFile + "', '" +
                rename.newFile + "', " + rename.similarity + ") ON CONFLICT DO NOTHING;";
    }
}
