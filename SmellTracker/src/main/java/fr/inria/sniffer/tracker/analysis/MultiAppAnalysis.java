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
package fr.inria.sniffer.tracker.analysis;

import com.mchange.v2.c3p0.DataSources;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class handling a single app analysis process in SmellTracker.
 */
public class MultiAppAnalysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MultiAppAnalysis.class.getName());

    private final List<String> applications;
    private final Map<String, String> remoteRepositories;
    private final String paprikaDBs;
    private final String githubToken;
    private final int threadsCount;
    private final String appLocalRepositories;
    private AnalysisType analysisType;
    private final DataSource connectionPool;

    /**
     * Start a simultaneous analysis on multiple projects.
     *
     * @param appsFile             CSV file containing the app names and Github remoteRepositories.
     * @param paprikaDBs           Path to the Paprika databases under the form paprika_db/$appName.
     * @param githubToken          Github API token to query on developers.
     * @param threadsCount         Number of available threads for the analysis.
     * @param appLocalRepositories Path to the git remoteRepositories of applications to avoid cloning them, under the form repos/$appName.
     */
    MultiAppAnalysis(String appsFile, String paprikaDBs, String githubToken, int threadsCount, String appLocalRepositories, AnalysisType analysisType) {
        this.paprikaDBs = paprikaDBs;
        this.githubToken = githubToken;
        this.threadsCount = threadsCount;
        this.appLocalRepositories = appLocalRepositories;
        this.analysisType = analysisType;

        applications = new ArrayList<>();
        remoteRepositories = new HashMap<>();
        parseAppsCSV(Paths.get(appsFile));
        connectionPool = initializeConnectionPool();
    }

    private void parseAppsCSV(Path appsFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(appsFile.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] appEntry = line.split(",");
                String appName = appEntry[0];
                if (appName.isEmpty()) {
                    continue;
                }

                applications.add(appName);
                if (appEntry.length > 1) {
                    remoteRepositories.put(appName, appEntry[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load applications CSV: " + appsFile, e);
        }

    }

    private DataSource initializeConnectionPool() {
        try {
            DataSource ds_unpooled = DataSources.unpooledDataSource(
                    "jdbc:postgresql:" + Main.DATABASE_URL, Main.DATABASE_USERNAME, Main.DATABASE_PASSWORD);
            return DataSources.pooledDataSource(ds_unpooled);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create DataSource", e);
        }
    }

    public void analyze() throws InterruptedException {
        logger.info("Starting multi application analysis using " + threadsCount + " threads");
        ExecutorService executorService = Executors.newWorkStealingPool(threadsCount);
        String repository;
        String paprikaDB;

        Callable<Void> analysis;

        for (String app : applications) {
            repository = chooseRepository(app);
            paprikaDB = Paths.get(paprikaDBs, app, "databases", "graph.db").toString();
            analysis = analysisType.getCallable(app, repository, paprikaDB, githubToken, remoteRepositories.get(app), connectionPool);
            logger.info("New app analysis: " + analysis);
            executorService.submit(analysis);
        }

        executorService.shutdown();
        executorService.awaitTermination(24, TimeUnit.HOURS);

        logger.info("Done.");
    }

    private String chooseRepository(String app) {
        if (appLocalRepositories != null) {
            return Paths.get(appLocalRepositories, app).toString();
        } else {
            return remoteRepositories.get(app);
        }
    }

    /**
     * Constructor for command line arguments
     *
     * @param arguments The command line arguments.
     */
    MultiAppAnalysis(Namespace arguments) {
        this(
                arguments.getString("apps"),
                arguments.getString("databases"),
                arguments.getString("githubToken"),
                arguments.getInt("threads"),
                arguments.getString("repositories"),
                arguments.get("type") != null ? arguments.get("type") : AnalysisType.SINGLE_APP
        );
    }

    /**
     * Defines the available inputs for a single app analysis.
     *
     * @param parser The parser to configure
     */
    static void setArguments(Subparser parser) {

        parser.addArgument("-a", "--apps")
                .help("CSV containing the list of applications to analyze and their Github path")
                .type(String.class)
                .required(true);

        parser.addArgument("-db", "--databases")
                .help("Path to the Paprika databases under the form paprika_db/$appName")
                .type(String.class)
                .required(true);

        parser.addArgument("-type")
                .help("Chose the analysis type to perform")
                .type(AnalysisType.class)
                .required(false);

        parser.addArgument("-k", "--githubToken")
                .help("Paprika analysis database")
                .type(String.class)
                .required(false);

        parser.addArgument("-t", "--threads")
                .help("Number of threads to allocate")
                .type(Integer.class)
                .setDefault(1)
                .required(false);

        parser.addArgument("-r", "--repositories")
                .help("Local directory containing repositories: $repo/$appName/.git")
                .type(String.class)
                .required(false);

    }
}
