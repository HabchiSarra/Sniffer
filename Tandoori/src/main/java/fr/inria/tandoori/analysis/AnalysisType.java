package fr.inria.tandoori.analysis;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

public enum AnalysisType {
    SINGLE_APP {
        @Override
        public Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                          String githubToken, String url, DataSource connections, int analysisId) {
            return new SingleAppAnalysisCallable(application, repository, paprikaDB, githubToken, url, connections);
        }
    },
    SUPPLEMENTARY {
        @Override
        public Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                          String githubToken, String url, DataSource connections, int analysisId) {
            return new SupplementaryAnalysisCallable(analysisId, paprikaDB, connections);
        }
    };

    public abstract Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                               String githubToken, String url, DataSource connections, int analysisId);
}
