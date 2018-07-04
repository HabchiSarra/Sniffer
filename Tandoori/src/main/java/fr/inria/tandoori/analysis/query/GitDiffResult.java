package fr.inria.tandoori.analysis.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GitDiffResult {
    private static final Logger logger = LoggerFactory.getLogger(GitDiffResult.class.getName());
    private static final GitDiffResult EMPTY = new GitDiffResult(0, 0, 0); // TODO: See if we set to -1 ?
    private static final Pattern DIFF_PATTERN = Pattern.compile("^(\\d+)\\sfiles\\schanged,\\s(\\d+)\\sinsertions\\(\\+\\),\\s(\\d+)\\sdeletions\\(-\\)$");

    private final int addition;
    private final int deletion;
    private final int changedFiles;

    GitDiffResult(int addition, int deletion, int changedFiles) {
        this.addition = addition;
        this.deletion = deletion;
        this.changedFiles = changedFiles;
    }

    /**
     * Syntax example:
     * 3 files changed, 65 insertions(+), 5 deletions(-)
     *
     * @param line Line to parse.
     * @return found {@link GitDiffResult}, null if could not parse.
     */
    public static GitDiffResult parse(String line) throws Exception {
        Matcher matcher = DIFF_PATTERN.matcher(line);

        if (matcher.find()) {
            int changedFiles = Integer.valueOf(matcher.group(1));
            int addition = Integer.valueOf(matcher.group(2));
            int deletion = Integer.valueOf(matcher.group(3));
            return new GitDiffResult(addition, deletion, changedFiles);
        }
        throw new Exception("Could not parse line: " + line);
    }

    /**
     * Check for the git diff --stat on the commit of the given repository.
     *
     * @param repository The repository to check.
     * @param sha1       The commit to check.
     * @return a {@link GitDiffResult} holding the additions and deletions.
     */
    static GitDiffResult fetch(String repository, String sha1) {
        GitDiffResult parsed = null;
        try {
            String options = " --stat";
            Process p = Runtime.getRuntime().exec("git -C " + repository + " diff " + sha1 + options);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine();
            // We wait for the line announcing global additions and deletions numbers
            while (line != null && parsed == null) {
                try {
                    parsed = GitDiffResult.parse(line);
                } catch (Exception e) {
                    // This is not an important failure since we expect it on each commit.
                    logger.trace(e.getLocalizedMessage());
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to execute git command", e);
        }
        return parsed == null ? GitDiffResult.EMPTY : parsed;
    }

    int getAddition() {
        return addition;
    }

    int getDeletion() {
        return deletion;
    }

    int getChangedFiles() {
        return changedFiles;
    }
}
