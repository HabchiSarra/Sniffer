package fr.inria.tandoori;

import fr.inria.tandoori.query.DevelopersQuery;
import fr.inria.tandoori.query.SmellQuery;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Class handling a single app analysis process in Tandoori.
 */
public class SingleAppAnalysis {
    private final String name;
    private final String repo;
    private final String db;
    private final String githubToken;

    SingleAppAnalysis(String appName, String appRepo, String db, String githubToken) {
        this.name = appName;
        this.repo = appRepo;
        this.db = db;
        this.githubToken = githubToken;
    }

    SingleAppAnalysis(Namespace arguments) {
        this(
                arguments.getString("name"),
                arguments.getString("repository"),
                arguments.getString("database"),
                arguments.getString("githubToken")
        );
    }

    public void start() {
        new SmellQuery(db).query();
        new DevelopersQuery(repo, githubToken).query();
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
