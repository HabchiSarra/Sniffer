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
package fr.inria.sniffer.tracker.analysis.query.commit;

import fr.inria.sniffer.tracker.analysis.model.CommitDetails;
import fr.inria.sniffer.tracker.analysis.model.GitChangedFile;
import fr.inria.sniffer.tracker.analysis.model.Repository;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FilesChangeDetectionQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(FilesChangeDetectionQuery.class.getName());

    private final int appId;
    private final Repository repository;
    private final Persistence persistence;
    private final CommitQueries commitQueries;

    public FilesChangeDetectionQuery(int appId, Repository repository, Persistence persistence, CommitQueries commitQueries) {
        this.appId = appId;
        this.repository = repository;
        this.persistence = persistence;
        this.commitQueries = commitQueries;
    }

    @Override
    public void query() throws QueryException {
        List<String> log;
        try {
            log = repository.getLog();
        } catch (IOException e) {
            throw new QueryException(logger.getName(), e);
        }

        CommitDetailsChecker detailsChecker = new CommitDetailsChecker(repository.getRepoDir().toString());

        persistence.execute(createFilesTable());
        for (String sha1 : log) {
            CommitDetails details = detailsChecker.fetch(sha1);
            List<Map<String, Object>> commitId;

            for (GitChangedFile changedFile : details.changedFiles) {
                commitId = persistence.query(commitQueries.idFromShaQuery(appId, sha1));
                if (commitId.isEmpty()) {
                    logger.warn("Unable to find commit id for project " + appId + " - sha: " + sha1);
                } else {
                    persistence.addStatements(commitQueries.fileChangedInsertionStatement(appId, sha1, changedFile));
                }
            }
        }
        persistence.commit();
    }

    private static String createFilesTable() {
        return "CREATE TABLE IF NOT EXISTS file_changed" +
                "(" +
                "project_id INT NOT NULL, " +
                "commit_id INT NOT NULL, " +
                "file_name VARCHAR NOT NULL, " +
                "modification_size INT," +
                "UNIQUE (project_id, commit_id, file_name)" +
                ")";
    }
}
