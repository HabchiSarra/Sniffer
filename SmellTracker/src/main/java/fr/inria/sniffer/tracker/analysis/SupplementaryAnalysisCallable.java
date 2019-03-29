/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
