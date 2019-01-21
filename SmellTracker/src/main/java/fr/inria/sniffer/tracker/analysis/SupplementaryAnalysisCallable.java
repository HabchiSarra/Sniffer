package fr.inria.sniffer.tracker.analysis;

import fr.inria.sniffer.tracker.analysis.persistence.queries.DeveloperQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.JDBCCommitQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.JDBCProjectQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.JDBCSmellQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.SmellQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.TagQueries;
import fr.inria.sniffer.tracker.analysis.persistence.PostgresqlPersistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.JDBCDeveloperQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.JDBCTagQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.ProjectQueries;
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
        SmellQueries smellQueries = new JDBCSmellQueries(commitQueries);
        TagQueries tagQueries = new JDBCTagQueries(commitQueries);
        try {
            analysis.analyze(persistence, projectQueries, commitQueries, smellQueries, tagQueries);
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
