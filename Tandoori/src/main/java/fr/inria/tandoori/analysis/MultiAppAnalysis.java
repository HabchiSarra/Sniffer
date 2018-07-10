package fr.inria.tandoori.analysis;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class handling a single app analysis process in Tandoori.
 */
public class MultiAppAnalysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MultiAppAnalysis.class.getName());

    private final List<String> applications;
    private final Map<String, String> remoteRepositories;
    private final String paprikaDBs;
    private final String githubToken;
    private final int threadsCount;
    private final String appLocalRepositories;

    /**
     * Start a simultaneous analysis on multiple projects.
     *
     * @param appsFile             CSV file containing the app names and Github remoteRepositories.
     * @param paprikaDBs           Path to the Paprika databases under the form paprika_db/$appName.
     * @param githubToken          Github API token to query on developers.
     * @param threadsCount         Number of available threads for the analysis.
     * @param appLocalRepositories Path to the git remoteRepositories of applications to avoid cloning them, under the form repos/$appName.
     */
    MultiAppAnalysis(String appsFile, String paprikaDBs, String githubToken, int threadsCount, String appLocalRepositories) {
        this.paprikaDBs = paprikaDBs;
        this.githubToken = githubToken;
        this.threadsCount = threadsCount;
        this.appLocalRepositories = appLocalRepositories;

        applications = new ArrayList<>();
        remoteRepositories = new HashMap<>();
        parseAppsCSV(Paths.get(appsFile));
    }

    private void parseAppsCSV(Path appsFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(appsFile.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] appEntry = line.split(",");
                String appName = appEntry[0];

                applications.add(appName);
                if (appEntry.length > 1) {
                    remoteRepositories.put(appName, appEntry[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load applications CSV: " + appsFile, e);
        }

    }

    public void analyze() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        String repository;
        String paprikaDB;

        List<SingleAnalysisTask> tasks = new ArrayList<>();
        for (String app : applications) {
            repository = chooseRepository(app);
            paprikaDB = Paths.get(paprikaDBs, app).toString();
            tasks.add(new SingleAnalysisTask(app, repository, paprikaDB, githubToken));
        }

        executorService.invokeAll(tasks);
    }

    private String chooseRepository(String app) {
        if (appLocalRepositories != null) {
            return Paths.get(appLocalRepositories, app).toString();
        } else {
            return remoteRepositories.get(app);
        }
    }

    private static final class SingleAnalysisTask implements Callable<Void> {
        private String application;
        private String repository;
        private String paprikaDB;
        private String githubToken;

        public SingleAnalysisTask(String application, String repository, String paprikaDB, String githubToken) {
            this.application = application;
            this.repository = repository;
            this.paprikaDB = paprikaDB;
            this.githubToken = githubToken;
        }

        @Override
        public Void call() throws Exception {
            new SingleAppAnalysis(application, repository, paprikaDB, githubToken).analyze();
            return null;
        }
    }

    /**
     * Constructor for command line arguments
     *
     * @param arguments The command line arguments.
     */
    MultiAppAnalysis(Namespace arguments) {
        this(
                arguments.getString("name"),
                arguments.getString("databases"),
                arguments.getString("githubToken"),
                arguments.getInt("threads"),
                arguments.getString("repository")
        );
    }

    /**
     * Defines the available inputs for a single app analysis.
     *
     * @param parser The parser to configure
     */
    static void setArguments(Subparser parser) {

        parser.addArgument("-a", "--apps")
                .help("CSV containing the list of applications to analyze")
                .type(String.class)
                .required(true);

        parser.addArgument("-db", "--databases")
                .help("Path to the Paprika databases under the form paprika_db/$appName")
                .type(String.class)
                .required(true);

        parser.addArgument("-k", "--githubToken")
                .help("Paprika analysis database")
                .type(String.class)
                .required(false);

        parser.addArgument("-t", "--threads")
                .help("Number of threads to allocate")
                .type(Integer.class)
                .required(false);

        parser.addArgument("-r", "--repositories")
                .help("Local directory containing repositories: $repo/$appName/.git")
                .type(String.class)
                .required(false);

    }
}
