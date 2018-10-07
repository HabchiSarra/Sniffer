package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCCommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCDeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCTagQueries;
import fr.inria.tandoori.analysis.persistence.queries.ProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.TagQueries;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

final class SupplementaryAnalysisCallable implements Callable<Void> {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SupplementaryAnalysisCallable.class.getName());
    private final String appName;
    private final String repository;
    private final String paprikaDB;
    private final DataSource connections;

    public SupplementaryAnalysisCallable(String appName, String repository, String paprikaDB, DataSource connections) {
        this.appName = appName;
        this.repository = repository;
        this.paprikaDB = paprikaDB;
        this.connections = connections;
    }

    @Override
    public Void call() throws Exception {
        SupplementaryAnalysis analysis = new SupplementaryAnalysis(appName, paprikaDB, repository);
        PostgresqlPersistence persistence = new PostgresqlPersistence(connections.getConnection());
        ProjectQueries projectQueries = new JDBCProjectQueries();
        DeveloperQueries developerQueries = new JDBCDeveloperQueries();
        CommitQueries commitQueries = new JDBCCommitQueries(developerQueries);
        TagQueries tagQueries = new JDBCTagQueries(commitQueries);
        try {
            analysis.analyze(persistence, projectQueries, commitQueries, tagQueries);
        } catch (AnalysisException e) {
            logger.error("Unable to perform analysis on project " + appName, e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "SupplementaryAnalysisCallable{" +
                "appName=" + appName +
                ", paprikaDB='" + paprikaDB + '\'' +
                '}';
    }
}
