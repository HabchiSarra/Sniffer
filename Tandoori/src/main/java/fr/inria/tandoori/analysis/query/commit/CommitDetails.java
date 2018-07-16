package fr.inria.tandoori.analysis.query.commit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    private static final Logger logger = LoggerFactory.getLogger(CommitDetails.class.getName());
    public final GitDiff diff;
    public final List<GitRename> renames;

    public CommitDetails(GitDiff diff, List<GitRename> renames) {
        this.diff = diff;
        this.renames = renames;
    }

    public static CommitDetails fetch(String repository, String sha1) {
        List<GitRename> renames = new ArrayList<>();
        GitDiff diff = GitDiff.EMPTY;

        List<String> lines = GitExecution.commitSummary(repository, sha1);
        for (String line : lines) {
            try {
                renames.add(GitRenameParser.parseRenamed(line));
            } catch (Exception e) {
                // This is an expected behavior
                logger.trace("[Rename] " + e.getMessage(), e);
            }
            try {
                diff = GitDiff.parse(line);
            } catch (Exception e) {
                // This is an expected behavior
                logger.trace("[Diff] " + e.getMessage(), e);
            }

        }
        return new CommitDetails(diff, renames);
    }

    @Override
    public String toString() {
        return "CommitDetails{" +
                "diff=" + diff +
                ", renames=" + renames +
                '}';
    }
}
