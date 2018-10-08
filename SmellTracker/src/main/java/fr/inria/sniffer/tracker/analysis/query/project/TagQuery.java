package fr.inria.sniffer.tracker.analysis.query.project;

import fr.inria.sniffer.tracker.analysis.model.Repository;
import fr.inria.sniffer.tracker.analysis.model.Tag;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.TagQueries;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TagQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(TagQuery.class.getName());

    private final int appId;
    private final Repository repository;
    private final Persistence persistence;
    private final TagQueries tagQueries;

    public TagQuery(int appId, Repository repository, Persistence persistence,
                    TagQueries tagQueries) {
        this.appId = appId;
        this.repository = repository;
        this.persistence = persistence;
        this.tagQueries = tagQueries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + appId + "] Starting Tags insertion");

        for (Tag tag : fetchTags()) {
            persistence.addStatements(tagQueries.tagInsertionStatement(appId, tag));
            persistence.commit();
        }
    }

    /**
     * Retrieve all tage in the repository.
     *
     * @return The list of {@link Tag}.
     * @throws QueryException If anything goes wrong with the repository.
     */
    private Iterable<Tag> fetchTags() throws QueryException {
        Git gitRepository = repository.getGitRepository();
        List<Ref> call;
        try {
            call = gitRepository.tagList().call();
        } catch (GitAPIException e) {
            throw new QueryException(logger.getName(), e);
        }

        List<Tag> tags = new ArrayList<>();
        final RevWalk walk = new RevWalk(gitRepository.getRepository());
        int timestamp;
        DateTime datetime = null;
        for (Ref ref : call) {
            logger.info("Tag: " + ref.getName() + " - Sha: " + ref.getObjectId().getName());
            try {
                timestamp = walk.parseCommit(ref.getObjectId()).getCommitTime();
                datetime = new DateTime(((long) timestamp) * 1000);
            } catch (IOException e) {
                logger.warn("[" + appId + "] => Unable to parse tag: " + ref.getName(), e);
            }
            tags.add(new Tag(ref.getName(), ref.getObjectId().getName(), datetime));
        }
        return tags;
    }
}