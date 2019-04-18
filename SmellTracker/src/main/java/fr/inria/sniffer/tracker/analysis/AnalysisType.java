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
