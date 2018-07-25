package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.model.CommitDetails;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommitDetailsChecker {
    private static final Logger logger = LoggerFactory.getLogger(CommitDetails.class.getName());
    private final String repository;

    public CommitDetailsChecker(String repository) {
        this.repository = repository;
    }

    public CommitDetails fetch(String sha1) {
        List<GitRename> renames = new ArrayList<>();
        GitDiff diff = GitDiff.EMPTY;

        List<String> lines = GitExecution.commitSummary(repository, sha1);
        for (String line : lines) {
            try {
                renames.add(GitRename.parseRenamed(line));
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
}