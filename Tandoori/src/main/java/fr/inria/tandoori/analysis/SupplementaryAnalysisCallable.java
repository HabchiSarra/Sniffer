package fr.inria.tandoori.analysis;

import fr.inria.tandoori.analysis.persistence.PostgresqlPersistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCBranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCCommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCDeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.JDBCSmellQueries;
import fr.inria.tandoori.analysis.persistence.queries.ProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

import static fr.inria.tandoori.analysis.Main.GITHUB_URL;

final class SupplementaryAnalysisCallable implements Callable<Void> {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SupplementaryAnalysisCallable.class.getName());
    private int analysisId;
    private String paprikaDB;
    DataSource connections;

    public SupplementaryAnalysisCallable(int analysisId, String paprikaDB, DataSource connections) {
        this.analysisId = analysisId;
        this.paprikaDB = paprikaDB;
        this.connections = connections;
    }

    @Override
    public Void call() throws Exception {
        SupplementaryAnalysis analysis = new SupplementaryAnalysis(analysisId, paprikaDB);
        PostgresqlPersistence persistence = new PostgresqlPersistence(connections.getConnection());
        DeveloperQueries developerQueries = new JDBCDeveloperQueries();
        CommitQueries commitQueries = new JDBCCommitQueries(developerQueries);
        try {
            analysis.analyze(persistence, commitQueries);
        } catch (AnalysisException e) {
            logger.error("Unable to perform analysis on project " + analysisId, e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "SupplementaryAnalysisCallable{" +
                "analysisId=" + analysisId +
                ", paprikaDB='" + paprikaDB + '\'' +
                '}';
    }
}
