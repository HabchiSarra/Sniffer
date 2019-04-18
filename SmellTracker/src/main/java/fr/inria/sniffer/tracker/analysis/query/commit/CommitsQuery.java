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

import fr.inria.sniffer.tracker.analysis.model.Repository;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.DeveloperQueries;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.detector.neo4j.QueryEngine;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch all commits and developers for a project, then provide them to {@link CommitsAnalysis}
 * for actual persisting.
 */
public class CommitsQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    private final int projectId;
    private final String paprikaDB;
    private final Repository repository;

    private final Persistence persistence;
    private final DeveloperQueries developerQueries;
    private final CommitQueries commitQueries;

    public CommitsQuery(int projectId, String paprikaDB, Repository repository,
                        Persistence persistence, DeveloperQueries developerQueries, CommitQueries commitQueries) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.repository = repository;
        this.persistence = persistence;
        this.developerQueries = developerQueries;
        this.commitQueries = commitQueries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Commits insertion");

        try {
            repository.initializeRepository();
        } catch (Repository.RepositoryException e) {
            throw new QueryException(logger.getName(), e);
        }

        QueryEngine engine = new QueryEngine(paprikaDB);
        CommitDetailsChecker detailsChecker = new CommitDetailsChecker(repository.getRepoDir().toString());

        Result commits = getCommits(engine);
        new CommitsAnalysis(projectId, persistence, repository, commits, detailsChecker, developerQueries, commitQueries).query();

        engine.shutDown();
        repository.finalizeRepository();
    }

    private static Result getCommits(QueryEngine engine) throws QueryException {
        return new fr.inria.sniffer.detector.neo4j.CommitsQuery(engine).streamResult(true, true);
    }




}
