package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCCommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCDeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCTagQueries;
import fr.inria.tandoori.analysis.persistence.queries.ProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.TagQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.commit.SizeQuery;
import fr.inria.tandoori.analysis.query.project.TagQuery;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.inria.tandoori.analysis.Main.DATABASE_PASSWORD;
import static fr.inria.tandoori.analysis.Main.DATABASE_URL;
import static fr.inria.tandoori.analysis.Main.DATABASE_USERNAME;

public class SupplementaryAnalysis implements Analysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SingleAppAnalysis.class.getName());
    private final String appName;
    private final String paprikaDB;
    private final String appRepo;

    private List<Query> getAnalysisProcess(int appId, Repository repository, Persistence persistence,
                                           CommitQueries commitQueries, TagQueries tagQueries) {
        List<Query> analysisProcess = new ArrayList<>();
        analysisProcess.add(new SizeQuery(appId, paprikaDB, persistence, commitQueries));
        analysisProcess.add(new TagQuery(appId, repository, persistence, tagQueries));
        return analysisProcess;
    }

    /**
     * Compute a single project analysis.
     *
     * @param paprikaDB Path to paprika database.
     */
    SupplementaryAnalysis(String appName, String paprikaDB, String appRepo) {
        this.appName = appName;
        this.paprikaDB = paprikaDB;
        this.appRepo = appRepo;
    }

    /**
     * Fetch and return the project ID.
     *
     * @param appName     The project to persist.
     * @param persistence The persistence to use.
     * @return The project identifier in the database.
     */
    private static int appId(String appName, Persistence persistence, ProjectQueries projectQueries) throws AnalysisException {
        String idQuery = projectQueries.idFromNameQuery(appName);
        List<Map<String, Object>> result = persistence.query(idQuery + ";");
        if (result.isEmpty()) {
            throw new AnalysisException("Unable to find ID for project: " + appName);
        }
        return (int) result.get(0).get("id");
    }

    public void analyze() throws AnalysisException {
        // TODO: Use dependency injection someday
        // Persistence persistence = new SQLitePersistence("output.sqlite");
        Persistence persistence = new PostgresqlPersistence(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        ProjectQueries projectQueries = new JDBCProjectQueries();
        DeveloperQueries developerQueries = new JDBCDeveloperQueries();
        CommitQueries commitQueries = new JDBCCommitQueries(developerQueries);
        TagQueries tagQueries = new JDBCTagQueries(commitQueries);
        this.analyze(persistence, projectQueries, commitQueries, tagQueries);
    }

    public void analyze(Persistence persistence, ProjectQueries projectQueries,
                        CommitQueries commitQueries, TagQueries tagQueries) throws AnalysisException {
        persistence.initialize();
        int appId = appId(appName, persistence, projectQueries);
        logger.info("[" + appId + "] Starting supplementary analysis");
        Repository repository = new Repository(appRepo);
        try {
            repository.initializeRepository();
        } catch (Repository.RepositoryException e) {
            throw new AnalysisException("Unable to open repository", e);
        }

        for (Query process : getAnalysisProcess(appId, repository, persistence,
                commitQueries, tagQueries)) {
            try {
                process.query();
            } catch (QueryException e) {
                logger.warn("An error occurred during query!", e);
            }
        }

        logger.info("[" + appId + "] Supplementary analysis done.");
        repository.finalizeRepository();
        persistence.close();
    }

    /**
     * Constructor for command line arguments
     *
     * @param arguments The command line arguments.
     */
    SupplementaryAnalysis(Namespace arguments) {
        this(
                arguments.getString("name"),
                arguments.getString("database"),
                arguments.getString("repository")
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

        parser.addArgument("-db", "--database")
                .help("Path to Paprika database")
                .type(String.class)
                .required(true);

        parser.addArgument("-r", "--repository")
                .help("Github repository as \"username/repository\" or local path")
                .type(String.class)
                .required(true);
    }
}
