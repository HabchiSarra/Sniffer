package fr.inria.sniffer.tracker.analysis;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

public enum AnalysisType {
    SINGLE_APP {
        @Override
        public Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                          String githubToken, String url, DataSource connections) {
            return new SingleAppAnalysisCallable(application, repository, paprikaDB, githubToken, url, connections);
        }
    },
    SUPPLEMENTARY {
        @Override
        public Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                          String githubToken, String url, DataSource connections) {
            return new SupplementaryAnalysisCallable(application, repository, paprikaDB, connections);
        }
    };

    public abstract Callable<Void> getCallable(String application, String repository, String paprikaDB,
                                               String githubToken, String url, DataSource connections);
}
