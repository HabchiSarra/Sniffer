package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import fr.inria.tandoori.analysis.query.CommitsQuery;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.SmellQuery;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.inria.tandoori.analysis.Main.DATABASE_PASSWORD;
import static fr.inria.tandoori.analysis.Main.DATABASE_URL;
import static fr.inria.tandoori.analysis.Main.DATABASE_USERNAME;

/**
 * Class handling a single app analysis process in Tandoori.
 */
public class SingleAppAnalysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SingleAppAnalysis.class.getName());

    private final List<Query> analysisProcess;
    // TODO: Configurable persistence
    // private final Persistence persistence = new SQLitePersistence("output.sqlite");
    private final Persistence persistence;

    // Used for logging purpose
    private final String appName;
    private final int appId;

    /**
     * Compute a single project analysis.
     *
     * @param appName     Name of the application under analysis.
     * @param appRepo     Github repository as "username/repository" or local path.
     * @param paprikaDB   Path to paprika database.
     * @param githubToken Github API token to query on developers.
     * @param persistence Persistence to set, the connection will be closed at the end of the analysis.
     */
    SingleAppAnalysis(String appName, String appRepo, String paprikaDB, String githubToken, Persistence persistence) {
        persistence.initialize();
        this.appName = appName;
        this.persistence = persistence;
        appId = persistApp(appName, persistence);

        analysisProcess = new ArrayList<>();
        analysisProcess.add(new CommitsQuery(appId, appRepo, persistence));
        analysisProcess.add(new SmellQuery(appId, paprikaDB, persistence));
        // if (githubToken != null) {
        //     analysisProcess.add(new DevelopersQuery(appRepo, githubToken));
        // }
    }

    /**
     * Compute a single project analysis.
     *
     * @param appName     Name of the application under analysis.
     * @param appRepo     Github repository as "username/repository" or local path.
     * @param paprikaDB   Path to paprika database.
     * @param githubToken Github API token to query on developers.
     */
    SingleAppAnalysis(String appName, String appRepo, String paprikaDB, String githubToken) {
        this(appName, appRepo, paprikaDB, githubToken, new PostgresqlPersistence(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD));
    }

    /**
     * Creates a new project in the persistence if not already existing,
     * <p>
     * then always fetch and return the project ID.
     *
     * @param appName     The project to persist.
     * @param persistence The persistence to use.
     * @return The project identifier in the database.
     */
    private static int persistApp(String appName, Persistence persistence) {
        String projectInsert = "INSERT INTO Project (name) VALUES ('" + appName + "') ON CONFLICT DO NOTHING;";
        persistence.addStatements(projectInsert);
        persistence.commit();

        String idQuery = persistence.projectQueryStatement(appName);
        List<Map<String, Object>> result = persistence.query(idQuery + ";");
        // TODO: Maybe be less violent / test the returned data
        return (int) result.get(0).get("id");
    }

    public void analyze() {

        logger.info("[" + appId + "] Analyzing application: " + appName);
        for (Query process : analysisProcess) {
            try {
                process.query();
            } catch (QueryException e) {
                logger.warn("An error occurred during query!", e);
            }
        }


        persistence.close();
    }

    /**
     * Constructor for command line arguments
     *
     * @param arguments The command line arguments.
     */
    SingleAppAnalysis(Namespace arguments) {
        this(
                arguments.getString("name"),
                arguments.getString("repository"),
                arguments.getString("database"),
                arguments.getString("githubToken")
        );
    }

    /**
     * Defines the available inputs for a single app analysis.
     *
     * @param parser The parser to configure
     */
    static void setArguments(Subparser parser) {
        parser.addArgument("-n", "--name")
                .help("Application name")
                .type(String.class)
                .required(true);

        parser.addArgument("-r", "--repository")
                .help("Github repository as \"username/repository\" or local path")
                .type(String.class)
                .required(true);

        parser.addArgument("-db", "--database")
                .help("Path to Paprika database")
                .type(String.class)
                .required(true);

        parser.addArgument("-k", "--githubToken")
                .help("Github API token to query on developers")
                .type(String.class)
                .required(false);
    }
}
