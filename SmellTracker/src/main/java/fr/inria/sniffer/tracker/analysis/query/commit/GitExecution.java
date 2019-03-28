/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.inria.sniffer.tracker.analysis.query.commit;

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
class GitExecution {
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
        // stat=800 avoids losing file name when too long (default is 80)
        return execute(repository, " show " + commit + " -M50% --stat=800 --summary --format=");
    }
}
