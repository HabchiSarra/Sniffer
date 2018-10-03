package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCCommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCDeveloperQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.commit.SizeQuery;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.tandoori.analysis.Main.DATABASE_PASSWORD;
import static fr.inria.tandoori.analysis.Main.DATABASE_URL;
import static fr.inria.tandoori.analysis.Main.DATABASE_USERNAME;

public class SupplementaryAnalysis implements Analysis {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SingleAppAnalysis.class.getName());
    private final int appId;
    private final String paprikaDB;

    private List<Query> getAnalysisProcess(int appId, Persistence persistence, CommitQueries commitQueries) {
        List<Query> analysisProcess = new ArrayList<>();
        analysisProcess.add(new SizeQuery(appId, paprikaDB, persistence, commitQueries));
        return analysisProcess;
    }

    /**
     * Compute a single project analysis.
     *
     * @param paprikaDB   Path to paprika database.
     */
    SupplementaryAnalysis(int appId, String paprikaDB) {
        this.appId = appId;
        this.paprikaDB = paprikaDB;
    }

    public void analyze() throws AnalysisException {
        // TODO: Use dependency injection someday
        // Persistence persistence = new SQLitePersistence("output.sqlite");
        Persistence persistence = new PostgresqlPersistence(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        DeveloperQueries developerQueries = new JDBCDeveloperQueries();
        CommitQueries commitQueries = new JDBCCommitQueries(developerQueries);
        this.analyze(persistence, commitQueries);
    }

    public void analyze(Persistence persistence, CommitQueries commitQueries) throws AnalysisException {
        persistence.initialize();
        logger.info("[" + appId + "] Starting supplementary analysis");
        for (Query process : getAnalysisProcess(appId, persistence, commitQueries)) {
            try {
                process.query();
            } catch (QueryException e) {
                logger.warn("An error occurred during query!", e);
            }
        }

        logger.info("[" + appId + "] Supplementary analysis done.");
        persistence.close();
    }

    /**
     * Constructor for command line arguments
     *
     * @param arguments The command line arguments.
     */
    SupplementaryAnalysis(Namespace arguments) {
        this(
                arguments.getInt("id") != null ? arguments.getInt("id") : 0,
                arguments.getString("database")
        );
    }

    /**
     * Defines the available inputs for a single app analysis.
     *
     * @param parser The parser to configure
     */
    static void setArguments(Subparser parser) {
        parser.addArgument("-db", "--database")
                .help("Path to Paprika database")
                .type(String.class)
                .required(true);

        parser.addArgument("-id", "--id")
                .help("Set an identifier to the processing, defaults to 0")
                .type(Integer.class)
                .required(false);
    }
}
