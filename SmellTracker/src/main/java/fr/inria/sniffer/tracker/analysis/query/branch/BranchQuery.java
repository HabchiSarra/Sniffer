package fr.inria.sniffer.tracker.analysis.query.branch;

import fr.inria.sniffer.tracker.analysis.model.Branch;
import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Repository;
import fr.inria.sniffer.tracker.analysis.query.PersistenceAnalyzer;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.BranchQueries;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Build a branch tree in the Persistence for the given project.
 */
public class BranchQuery extends PersistenceAnalyzer implements Query {
    private final Repository repository;
    private final BranchQueries branchQueries;

    private int branchCounter;

    public BranchQuery(int projectId, Repository repository,
                       Persistence persistence, CommitQueries commitQueries, BranchQueries branchQueries) {
        super(LoggerFactory.getLogger(BranchQuery.class.getName()), projectId, persistence, commitQueries);
        this.repository = repository;
        this.branchQueries = branchQueries;
        branchCounter = 0;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Branches insertion");
        Commit commit = retrieveHeadCommit();

        List<Branch> branches = buildBranchTree(null, commit);
        for (Branch branch : branches) {
            persistBranch(branch);
        }
        persistence.commit();
    }

    /**
     * Retrieve the last commit analyzed by Paprika.
     *
     * @return The first commit to analyze on branch query.
     * @throws QueryException If the commit could not be found.
     */
    private Commit retrieveHeadCommit() throws QueryException {
        Commit commit;
        try {
            commit = repository.getCommitWithParents(repository.getHead().sha);
            logger.info("[" + projectId + "] => Found HEAD commit: " + commit.sha);
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new QueryException(logger.getName(), e);
        }
        return commit;
    }

    /**
     * Persist the given branch in repository.
     *
     * @param branch The branch to persist.
     */
    private void persistBranch(Branch branch) {
        String statement = branchQueries.branchInsertionStatement(projectId, branch.getOrdinal(),
                branch.getParentCommit(), branch.getMergedInto());
        persistence.addStatements(statement);

        List<Commit> commits = branch.getCommits();
        Collections.reverse(commits);
        reverse_ordinal(commits);
        for (Commit commit : commits) {
            statement = branchQueries.branchCommitInsertionQuery(projectId, branch.getOrdinal(), commit.sha, commit.getBranchOrdinal());
            persistence.addStatements(statement);

        }
    }

    private void reverse_ordinal(List<Commit> commits) {
        List<Integer> ordinals = new ArrayList<>();
        for (Commit commit : commits) {
            ordinals.add(commit.getBranchOrdinal());
        }

        for (int i = 0; i < commits.size(); i++) {
            commits.get(i).setBranchOrdinal(ordinals.get(ordinals.size() - i - 1));
        }
    }

    /**
     * Create the Tree of branches from the principal one.
     * This will parse all merge commits and follow their child branches recursively.
     *
     * @param mother The branch on which this branch is built.
     * @param start  The branch first commit.
     * @return The list of {@link Branch} in this project.
     */
    private List<Branch> buildBranchTree(Branch mother, Commit start) {
        // In the case that a merge commit will send us to already analyzed commits.
        // It can happen in the case of BranchQueryTest#testContinuingBranches
        if (isInBranch(mother, start)) {
            logger.debug("We already analyzed this commit, returning.");
            return Collections.emptyList();
        }

        Branch current = buildBranch(mother, start);

        List<Branch> newBranches;
        List<Branch> branches = new ArrayList<>();
        Branch traversedCommits = Branch.newMother(mother, current);

        // We build a branch tree for all merge commits of the current branch.
        for (Commit merge : current.getMerges()) {
            Commit parentCommit = retrieveParentCommit(merge, 1);
            if (parentCommit != null) {
                logger.debug("[" + projectId + "] => Handling merge commit: " + merge.sha);
                // We set the merge commit to the branch to be able to retrieve it in the child branch.
                traversedCommits.setMergedInto(merge);
                newBranches = buildBranchTree(traversedCommits, parentCommit);

                for (Branch traversedBranch : newBranches) {
                    traversedCommits.addCommits(traversedBranch.getCommits());
                }
                branches.addAll(newBranches);
            }
        }

        branches.add(current);
        return branches;
    }

    /**
     * Build a branch with ordered commits.
     *
     * @param mother The branch from which the current one is forked.
     * @param start  The starting commit of our current branch.
     * @return The newly built branch.
     */
    private Branch buildBranch(Branch mother, Commit start) {
        Branch current = Branch.fromMother(mother, branchCounter++);

        Commit commit = start;
        int commitOrdinal = 0;
        while (nextStillInBranch(commit, mother)) {
            logger.trace("[" + projectId + "] => Handling commit: " + commit.sha);
            logger.trace("[" + projectId + "] ==> commit parents (" + commit.getParentCount() + "): " + commit.parents);

            current.addCommit(commit, commitOrdinal);
            if (commit.getParentCount() >= 2) {
                current.addMerge(commit);
            }

            // Retrieve the parent commit, and do the same.
            commit = retrieveParentCommit(commit, 0);
            // But we increase the ordinal whichever the commit to notify the commit gap
            // in case that Paprika does not know the commit.
            commitOrdinal++;
        }

        // Last execution setting parent commit
        if (commit != null) {
            if (commit.getParentCount() >= 2) {
                current.addMerge(commit);
            }
            current.addCommit(commit, commitOrdinal);
            // If the current commit has a parent, we set this parent
            // as the whole branch parent commit.
            if (commit.getParentCount() >= 1) {
                current.setParentCommit(retrieveParentCommit(commit, 0));
            }
        }
        return current;
    }

    /**
     * Retrieve the nth parent of the given commit.
     * If it could not retrieve the parent for any reason, we log as error the reason and return null.
     *
     * @param commit   The commit to retrieve parent from.
     * @param position The nth parent to fetch.
     * @return The {@link Commit} if found, null otherwise.
     */
    private Commit retrieveParentCommit(Commit commit, int position) {
        try {
            return repository.getCommitWithParents(commit.getParent(position).sha);
        } catch (IOException e) {
            logger.error("[" + projectId + "] ==> Unable to fetch parent n°" + position + " (" + commit.getParent(1).sha +
                    ") for commit: " + commit.sha, e);
            return null;
        }
    }

    /**
     * Determine if the next commit (the given commit's parent)
     * is still in the analyzed branch.
     *
     * @param commit The commit to check.
     * @param mother The mother of the analyzed branch.
     * @return False if the commit parent is null or in the mother branch,
     * True otherwise.
     */
    private boolean nextStillInBranch(Commit commit, Branch mother) {
        return !(isProjectFirstCommit(commit)
                || isInBranch(mother, commit.getParent(0)));
    }

    /**
     * Determine if the given commit is the origin of the currently parsed branch.
     *
     * @param commit The current commit to assert. If the commit is null or has no parent,
     *               we consider that we reached the end of the commit tree,
     *               i.e. the original commit of the principal branch.
     * @return True if the commit is null or has no parent, false otherwise.
     */
    private boolean isProjectFirstCommit(Commit commit) {
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