package fr.inria.tandoori.analysis.query.commit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Execute a command using the host 'git' command.
 * Because sometimes libraries can't do everything...
 * // TODO: See if we can use Jgit instead of raw calls to git
 */
public class GitExecution {
    private static final Logger logger = LoggerFactory.getLogger(GitExecution.class.getName());

    public static List<String> execute(String repository, String query) {
        List<String> result = new ArrayList<>();
        try {
            String command = gitCommand(repository, query);
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                p.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Git execution took too much time, skipping", e);
                return result;
            }
            String line;

            while ((line = stdIn.readLine()) != null) {
                result.add(line);
            }

            stdIn.close();
            p.waitFor(10, TimeUnit.SECONDS);
            p.destroy();
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to execute git command", e);
        }
        return result;
    }

    private static String gitCommand(String repository, String query) {
        return "git -C " + repository + " " + query;
    }

    public static List<String> commitSummary(String repository, String commit) {
        return execute(repository, " show " + commit + " -M50% --stat --summary --format=");
    }
}
