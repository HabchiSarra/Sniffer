package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SQLitePersistence;
import fr.inria.tandoori.analysis.query.CommitsQuery;
import fr.inria.tandoori.analysis.query.DevelopersQuery;
import fr.inria.tandoori.analysis.query.MetricsQuery;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.SmellQuery;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class handling a single app analysis process in Tandoori.
 */
public class SingleAppAnalysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SingleAppAnalysis.class.getName());

    private final List<Query> analysisProcess;
    // TODO: Configurable persistence
    private final Persistence persistence = new SQLitePersistence("output.sqlite");

    SingleAppAnalysis(String appName, String appRepo, String db, String githubToken) {
        // TODO: Should we initialize it only once, in a more specific place?
        persistence.initialize();

        int appId = persistApp(appName, persistence);

        analysisProcess = new ArrayList<>();
        analysisProcess.add(new SmellQuery(db));
        analysisProcess.add(new DevelopersQuery(appRepo, githubToken));
        analysisProcess.add(new CommitsQuery(appId, appRepo, persistence));
        analysisProcess.add(new MetricsQuery(persistence));
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
        // TODO: Insert and retrieve an identifier for the project
        return 0;
    }

    public void analyze() {
        for (Query process : analysisProcess) {
            try {
                process.query();
            } catch (QueryException e) {
                logger.warn("An error occurred during query!", e);
            }
        }

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
                .help("Github repository as \"username/repository\"")
                .type(String.class)
                .required(true);

        parser.addArgument("-db", "--database")
                .help("Paprika analysis database")
                .type(String.class)
                .required(true);

        parser.addArgument("-k", "--githubToken")
                .help("Paprika analysis database")
                .type(String.class)
                .required(true);
    }
}
