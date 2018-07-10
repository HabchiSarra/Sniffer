package fr.inria.tandoori.analysis.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiff {
    private static final Logger logger = LoggerFactory.getLogger(GitDiff.class.getName());
    public static final GitDiff EMPTY = new GitDiff(0, 0, 0); // TODO: See if we set to -1 ?
    private static final Pattern DIFF_PATTERN = Pattern.compile("^(\\d+)\\sfiles\\schanged,\\s(\\d+)\\sinsertions\\(\\+\\),\\s(\\d+)\\sdeletions\\(-\\)$");

    private final int addition;
    private final int deletion;
    private final int changedFiles;

    public GitDiff(int addition, int deletion, int changedFiles) {
        this.addition = addition;
        this.deletion = deletion;
        this.changedFiles = changedFiles;
    }

    /**
     * Syntax example:
     * 3 files changed, 65 insertions(+), 5 deletions(-)
     *
     * @param line Line to parse.
     * @return found {@link GitDiff}, null if could not parse.
     */
    static GitDiff parse(String line) throws Exception {
        Matcher matcher = DIFF_PATTERN.matcher(line);

        if (matcher.find()) {
            int changedFiles = Integer.valueOf(matcher.group(1));
            int addition = Integer.valueOf(matcher.group(2));
            int deletion = Integer.valueOf(matcher.group(3));
            return new GitDiff(addition, deletion, changedFiles);
        }
        throw new Exception("Could not parse line: " + line);
    }

    /**
     * Check for the git diff --stat on the commit of the given repository.
     *
     * @param repository The repository to check.
     * @param sha1       The commit to check.
     * @return a {@link GitDiff} holding the additions and deletions.
     */
    static GitDiff fetch(String repository, String sha1) {
        List<String> diffs = GitExecution.commitDiff(repository, sha1);

        GitDiff parsed = null;
        Iterator<String> diffIterator = diffs.iterator();
        while (diffIterator.hasNext() && parsed == null) {
            try {
                parsed = GitDiff.parse(diffIterator.next());
            } catch (Exception e) {
                // This is not an important failure since we expect it on each commit.
                logger.warn(e.getMessage());
            }
        }
        parsed = parsed == null ? GitDiff.EMPTY : parsed;
        logger.trace("=> Commit diff is: " + parsed);
        return parsed;
    }

    public int getAddition() {
        return addition;
    }

    public int getDeletion() {
        return deletion;
    }

    public int getChangedFiles() {
        return changedFiles;
    }

    @Override
    public String toString() {
        return "GitDiff{" +
                "addition=" + addition +
                ", deletion=" + deletion +
                ", changedFiles=" + changedFiles +
                '}';
    }
}
