package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;
import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import neo4j.QueryEngine;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch all commits and developers for a project, then provide them to {@link CommitsAnalysis}
 * for actual persisting.
 */
public class CommitsQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    public static final int BATCH_SIZE = 1000;
    private final int projectId;
    private final String paprikaDB;
    private final Repository repository;
    private final Persistence persistence;

    public CommitsQuery(int projectId, String paprikaDB, Repository repository, Persistence persistence) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.repository = repository;
        this.persistence = persistence;
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
        new CommitsAnalysis(projectId, persistence, repository, commits, detailsChecker).query();

        engine.shutDown();
        repository.finalizeRepository();
    }

    private static Result getCommits(QueryEngine engine) throws QueryException {
        return new neo4j.CommitsQuery(engine).streamResult(true, true);
    }


}
