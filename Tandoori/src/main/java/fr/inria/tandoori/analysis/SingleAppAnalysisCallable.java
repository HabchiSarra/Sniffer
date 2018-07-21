package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

import static fr.inria.tandoori.analysis.Main.GITHUB_URL;

final class SingleAppAnalysisCallable implements Callable<Void> {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SingleAppAnalysisCallable.class.getName());
    private String application;
    private String repository;
    private String paprikaDB;
    private String githubToken;
    private String url;
    DataSource connections;

    public SingleAppAnalysisCallable(String application, String repository, String paprikaDB,
                                     String githubToken, String url, DataSource connections) {
        this.application = application;
        this.repository = repository;
        this.paprikaDB = paprikaDB;
        this.githubToken = githubToken;
        // Set null if no url, else join the GITHUB_URL with the given 'owner/project' path.
        if (url == null) {
            this.url = null;
        } else {
            url = url.trim();
            this.url = GITHUB_URL + (url.startsWith("/") ? url.substring(1) : url);
        }
        this.connections = connections;
    }

    @Override
    public Void call() throws Exception {
        SingleAppAnalysis analysis = new SingleAppAnalysis(application, repository, paprikaDB, githubToken, url);
        try {
            analysis.analyze(new PostgresqlPersistence(connections.getConnection()));
        } catch (AnalysisException e) {
            logger.error("Unable to perform analysis on project " + application, e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "SingleAnalysisTask{" +
                "application='" + application + '\'' +
                ", repository='" + repository + '\'' +
                ", paprikaDB='" + paprikaDB + '\'' +
                ", url='" + url + '\'' +
                ", githubToken='" + (githubToken == null ? null : "XXXX (is set)") + '\'' +
                '}';
    }
}
