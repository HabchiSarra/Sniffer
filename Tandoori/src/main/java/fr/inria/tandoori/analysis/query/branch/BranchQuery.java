package fr.inria.tandoori.analysis.query.branch;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import fr.inria.tandoori.analysis.query.commit.Repository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
            commit = getCommit("HEAD");
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
     * @param branch
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
        while (commit.getParentCount() > 0 && !isInBranch(mother, commit.getParent(0))) {
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

    private boolean isInBranch(Branch branch, RevCommit commit) {
        return branch != null && branch.contains(commit.getParent(0));
    }

    private RevCommit getCommit(String sha) throws IOException {
        org.eclipse.jgit.lib.Repository gitRepo = this.repository.getGitRepository().getRepository();
        Ref head = gitRepo.findRef(sha);
        logger.debug("[" + projectId + "] ==> Found commit for identifier: " + sha);

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk walk = new RevWalk(gitRepo)) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            walk.dispose();
            return commit;
        }
    }
}