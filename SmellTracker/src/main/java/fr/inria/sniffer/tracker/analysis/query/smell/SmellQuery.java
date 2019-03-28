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
package fr.inria.sniffer.tracker.analysis.query.smell;

import fr.inria.sniffer.tracker.analysis.persistence.queries.SmellQueries;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.BranchQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.query.smell.duplication.SmellDuplicationChecker;
import fr.inria.sniffer.detector.neo4j.HashMapUsageQuery;
import fr.inria.sniffer.detector.neo4j.InitOnDrawQuery;
import fr.inria.sniffer.detector.neo4j.InvalidateWithoutRectQuery;
import fr.inria.sniffer.detector.neo4j.LICQuery;
import fr.inria.sniffer.detector.neo4j.MIMQuery;
import fr.inria.sniffer.detector.neo4j.NLMRQuery;
import fr.inria.sniffer.detector.neo4j.OverdrawQuery;
import fr.inria.sniffer.detector.neo4j.QueryEngine;
import fr.inria.sniffer.detector.neo4j.UnsuitedLRUCacheSizeQuery;
import fr.inria.sniffer.detector.neo4j.UnsupportedHardwareAccelerationQuery;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieve all the smells of a given project for each commits, through Paprika.
 */
public class SmellQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellQuery.class.getName());
    private final String paprikaDB;
    private final Persistence persistence;
    private final int projectId;
    private BranchQueries branchQueries;
    private SmellQueries smellQueries;
    private CommitQueries commitQueries;

    public SmellQuery(int projectId, String paprikaDB, Persistence persistence,
                      CommitQueries commitQueries, SmellQueries smellQueries, BranchQueries branchQueries) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.persistence = persistence;
        this.commitQueries = commitQueries;
        this.smellQueries = smellQueries;
        this.branchQueries = branchQueries;
    }

    private List<fr.inria.sniffer.detector.neo4j.Query> queries(QueryEngine queryEngine) {
        ArrayList<fr.inria.sniffer.detector.neo4j.Query> queries = new ArrayList<>();
        queries.add(MIMQuery.createMIMQuery(queryEngine));
        queries.add(LICQuery.createLICQuery(queryEngine));
        queries.add(NLMRQuery.createNLMRQuery(queryEngine));
        queries.add(OverdrawQuery.createOverdrawQuery(queryEngine));
        queries.add(UnsuitedLRUCacheSizeQuery.createUnsuitedLRUCacheSizeQuery(queryEngine));
        queries.add(InitOnDrawQuery.createInitOnDrawQuery(queryEngine));
        queries.add(UnsupportedHardwareAccelerationQuery.createUnsupportedHardwareAccelerationQuery(queryEngine));
        queries.add(HashMapUsageQuery.createHashMapUsageQuery(queryEngine));
        queries.add(InvalidateWithoutRectQuery.createInvalidateWithoutRectQuery(queryEngine));
        return queries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Smells insertion");
        QueryEngine queryEngine = new QueryEngine(paprikaDB);
        SmellDuplicationChecker duplicationChecker = new SmellDuplicationChecker(projectId, persistence, queryEngine);

        for (fr.inria.sniffer.detector.neo4j.Query query : queries(queryEngine)) {
            logger.info("[" + projectId + "] => Querying Smells of type: " + query.getSmellName());

            Result result = query.streamResult(true, true);
            logger.trace("[" + projectId + "]   ==> Found smells: " + result);

            new BranchAwareSmellTypeAnalysis(projectId, persistence, result, query.getSmellName(), duplicationChecker, commitQueries, smellQueries, branchQueries).query();

            // Calling commit for each smell type to avoid too big request.
            persistence.commit();
        }

        queryEngine.shutDown();
    }
}
