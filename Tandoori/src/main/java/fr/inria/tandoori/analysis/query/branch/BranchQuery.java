package fr.inria.tandoori.analysis.query.branch;

import fr.inria.tandoori.analysis.model.Branch;
import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
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
    private int branchCounter;

    public BranchQuery(int projectId, Repository repository, Persistence persistence) {
        this.projectId = projectId;
        this.repository = repository;
        this.persistence = persistence;
        branchCounter = 0;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Branches insertion");
        Commit commit;
        try {
            commit = repository.getHead();
            logger.info("[" + projectId + "] => Found HEAD commit: " + commit.sha);
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

        for (Commit commit : branch.getCommits()) {
            statement = persistence.branchCommitInsertionQuery(projectId, branch.getOrdinal(), commit.sha);
            persistence.addStatements(statement);
        }
    }

    /**
     * @param mother
     * @param start
     * @return
     */
    private List<Branch> buildBranchTree(Branch mother, Commit start) {
        Branch current = Branch.fromMother(mother, branchCounter++);
        List<Commit> merges = new ArrayList<>();

        // TODO check if equality on RevCommit is working
        Commit commit = start;
        while (!isFirstCommit(commit) && !isInBranch(mother, commit.getParent(0))) {
            // We do not add current merge commit if it is part of the branch above.
            if (commit.getParentCount() >= 2) {
                merges.add(commit);
            }
            current.addCommit(commit);

            // Retrieve the parent commit, and do the same.
            try {
                commit = repository.getCommitWithParents(commit.getParent(0).sha);
            } catch (IOException e) {
                // We won't continue insertion if we couldn't find the parent.
                logger.error("Unable to fetch parent for commit: " + commit.sha, e);
                break;
            }
        }
        current.addCommit(commit);

        List<Branch> branches = new ArrayList<>();
        for (Commit merge : merges) {
            branches.addAll(buildBranchTree(current, merge.getParent(1)));
        }

        branches.add(current);
        return branches;
    }

    /**
     * Determine if the given commit is the origin of the currently parsed branch.
     *
     * @param commit The current commit to assert. If the commit is null or has no parent,
     *               we consider that we reached the end of the commit tree,
     *               i.e. the original commit of the principal branch.
     * @return True if the commit is null or has no parent, false otherwise.
     */
    private boolean isFirstCommit(Commit commit) {
        return commit == null || commit.getParentCount() == 0;
    }

    /**
     * Determine if the given commit is contained in the given branch.
     *
     * @param branch The mother branch in which we could find the original commit.
     *               If we are handling the principal branch, this can be null,
     *               thus the commit will never be part of the mother branch.
     * @param commit The commit to check.
     * @return True if the commit is part of the mother branch, false otherwise.
     */
    private boolean isInBranch(Branch branch, Commit commit) {
        return branch != null && branch.contains(commit);
    }

}