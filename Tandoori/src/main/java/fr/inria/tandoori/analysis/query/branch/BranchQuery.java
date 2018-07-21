package fr.inria.tandoori.analysis.query.branch;

import fr.inria.tandoori.analysis.model.Branch;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.model.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BranchQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(Repository.class.getName());

    private final int projectId;
    private final Repository repository;
    private final Persistence persistence;

    public BranchQuery(int projectId, Repository repository, Persistence persistence) {
        this.projectId = projectId;
        this.repository = repository;
        this.persistence = persistence;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Branches insertion");
        RevCommit commit;
        try {
            commit = repository.getCommit("HEAD");
            logger.info("[" + projectId + "] => Found HEAD for identifier: " + commit.name());
        } catch (IOException e) {
            throw new QueryException(logger.getName(), e);
        }

        List<Branch> branches = buildBranchTree(null, commit);
        for (Branch branch : branches) {
            persistBranch(branch);
        }
        persistence.commit();
    }

    /**
     * Persist the given branch in repository.
     *
     * @param branch The branch to persist.
     */
    private void persistBranch(Branch branch) {
        String statement = persistence.branchInsertionStatement(projectId, branch.getOrdinal(), branch.isMaster());
        persistence.addStatements(statement);

        for (RevCommit commit : branch.getCommits()) {
            statement = persistence.branchCommitInsertionQuery(projectId, branch.getOrdinal(), commit.name());
            persistence.addStatements(statement);
        }
    }

    /**
     * @param mother
     * @param start
     * @return
     */
    private List<Branch> buildBranchTree(Branch mother, RevCommit start) {
        Branch current = Branch.fromMother(mother);
        List<RevCommit> merges = new ArrayList<>();

        // TODO check if equality on RevCommit is working
        RevCommit commit = start;
        while (isNotOriginCommit(mother, commit)) {
            if (commit.getParentCount() >= 2) {
                merges.add(commit);
            }
            current.addCommit(commit);

            commit = commit.getParent(0);
        }
        current.addCommit(commit);

        List<Branch> branches = new ArrayList<>();
        for (RevCommit merge : merges) {
            branches.addAll(buildBranchTree(current, merge));
        }

        branches.add(current);
        return branches;
    }

    /**
     * Determine if the given commit is the origin of the currently parsed branch.
     *
     * @param mother The mother branch in which we could find the original commit.
     *               If we are handling the principal branch, this can be null,
     *               thus we will only wait for a null commit.
     * @param commit The current commit to assert. If the commit is null, we consider that we reached the end of the
     *               commit tree, i.e. the original commit of the principal branch.
     * @return True if the commit
     */
    private boolean isNotOriginCommit(Branch mother, RevCommit commit) {
        return commit != null && commit.getParentCount() > 0 && !isInBranch(mother, commit.getParent(0));
    }

    private boolean isInBranch(Branch branch, RevCommit commit) {
        return branch != null && branch.contains(commit.getParent(0));
    }

}