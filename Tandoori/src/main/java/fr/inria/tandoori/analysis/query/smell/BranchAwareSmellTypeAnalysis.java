package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Analyze a {@link Smell} type considering the commits ordinal as well as their original branch.
 * This class requires the {@link fr.inria.tandoori.analysis.query.branch.BranchQuery} to be processed on the project.
 * <p>
 * This should reduce the number of false positive on smell analysis by sorting commits by branch.
 */
class BranchAwareSmellTypeAnalysis extends AbstractSmellTypeAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(BranchAwareSmellTypeAnalysis.class.getName());

    private final Iterator<Map<String, Object>> smells;
    private final String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    private final Map<Integer, BranchAnalyzer> branchAnalyzers;
    private final Map<Integer, String> branchLastCommitSha;
    private final BranchQueries branchQueries;

    public BranchAwareSmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
                                        String smellType, SmellDuplicationChecker duplicationChecker,
                                        CommitQueries commitQueries, SmellQueries smellQueries, BranchQueries branchQueries) {
        super(LoggerFactory.getLogger(OrdinalSmellTypeAnalysis.class.getName()), projectId, persistence, commitQueries, smellQueries);
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;
        this.branchQueries = branchQueries;

        branchAnalyzers = new HashMap<>();
        branchLastCommitSha = new HashMap<>();
    }

    @Override
    public void query() throws QueryException {
        Smell smell;
        Commit previousCommit;
        Commit commit = Commit.empty();
        Integer previousBranch;
        Integer currentBranch = -1;

        Map<String, Object> instance;
        while (smells.hasNext()) {
            instance = smells.next();
            previousCommit = commit;
            previousBranch = currentBranch;
            commit = Commit.fromInstance(instance);
            smell = Smell.fromPaprikaInstance(instance, smellType);
            try {
                currentBranch = fetchCommitBranch(commit);
            } catch (BranchNotFoundException e) {
                logger.warn("[" + projectId + "] ==> Unable to guess branch for commit (" + commit.sha + "), skipping", e.getMessage());
                continue;
            }

            // We create the new BranchAnalyzer if needed.
            if (!branchAnalyzers.containsKey(currentBranch)) {
                logger.debug("[" + projectId + "] => Initializing new branch: " + currentBranch);
                initializeBranch(currentBranch);
            }

            // We then submit the new smell to analysis
            commit.setOrdinal(fetchCommitOrdinal(currentBranch, commit));
            branchAnalyzers.get(currentBranch).notifyCommit(commit);

            // On commit change, we ensure to merge SmellPresence from the merged commit if necessary.
            if (!previousCommit.equals(commit)) {
                synchronizeMergeSmells(commit, currentBranch);
            }

            // Once the previous Smells are all set, notify our newly found smell.
            branchAnalyzers.get(currentBranch).notifySmell(smell);

            // When we are sure that we passed the last branch commit, we will finalize the branch analysis,
            // i.e. setting introductions and refactoring for the last branch commit.
            if (!previousCommit.equals(commit) && isLastBranchCommit(previousCommit, previousBranch)) {
                finalizeBranch(previousBranch);
                branchAnalyzers.remove(previousBranch);
            }
        }

        // We should only perform operations for branch 0 since all other commits are looped around.
        for (int branchId : branchAnalyzers.keySet()) {
            finalizeBranch(branchId);
        }
    }

    /**
     * This method will check if the current commit is a merge commit.
     * It will then load every smell from the merged commit in its branch.
     *
     * @param commit        The commit to check.
     * @param currentBranch The commit branch to insert smells into.
     */
    private void synchronizeMergeSmells(Commit commit, Integer currentBranch) {
        Integer mergedCommitId = getMergedCommitId(commit);
        if (mergedCommitId != null) {
            persistence.commit();
            addSmellsToMergeCommit(mergedCommitId, currentBranch);
        }
    }

    private int fetchCommitOrdinal(int branchId, Commit commit) throws QueryException {
        List<Map<String, Object>> result = persistence.query(branchQueries.commitOrdinalQuery(projectId, branchId, commit));
        if (result.isEmpty()) {
            throw new QueryException(logger.getName(), "Unable to find commit (" + commit.sha + ") in branch n°" + branchId);
        }
        return (int) result.get(0).get("ordinal");
    }

    /**
     * When we change our commit, we check if it is a merge commit,
     * if we have one, we will retrieve all smells from the merged branch last commit, in order
     * to keep a realistic track of the introductions and refactoring.
     *
     * @param mergedCommitId The commit being merged.
     * @param currentBranch  The branch to add commits onto.
     */
    private void addSmellsToMergeCommit(int mergedCommitId, int currentBranch) {
        branchAnalyzers.get(currentBranch).addMergedSmells(retrieveMergedCommitSmells(mergedCommitId));
    }

    /**
     * Create a new {@link BranchAnalyzer} and add is to the branchAnalyzers,
     * With all the smells from its parent commit.
     *
     * @param currentBranch Identifier of the branch to initialize.
     */
    private void initializeBranch(int currentBranch) {
        logger.debug("[" + projectId + "] => Initializing branch: " + currentBranch);
        persistence.commit();
        BranchAnalyzer analyzer = new MultiBranchAnalyzer(projectId, persistence, duplicationChecker,
                commitQueries, smellQueries, branchQueries, currentBranch);
        analyzer.addExistingSmells(retrieveBranchParentSmells(currentBranch));
        branchAnalyzers.put(currentBranch, analyzer);

        List<Map<String, Object>> query = persistence.query(branchQueries.lastCommitShaQuery(projectId, currentBranch));
        if (query.isEmpty()) {
            logger.warn("No merge commit found for branch: " + currentBranch);
        } else {
            branchLastCommitSha.put(currentBranch, (String) query.get(0).get("sha1"));
        }
    }

    /**
     * Return the sha of the branch's last commit.
     *
     * @param branchId The branch to lookup onto.
     * @return The last commit sha.
     */
    private String getLastBranchCommit(int branchId) {
        return branchLastCommitSha.get(branchId);
    }

    /**
     * Call finalize on {@link BranchAnalyzer} with the correct end commit for this branch.
     *
     * @param branchId The branch identifier.
     * @throws QueryException If anything goes wrong while querying the last commit for this project.
     */
    private void finalizeBranch(int branchId) throws QueryException {
        logger.debug("[" + projectId + "] => Finalizing branch: " + branchId);
        String lastBranchCommit = getLastBranchCommit(branchId);
        if (lastBranchCommit != null) {
            branchAnalyzers.get(branchId).notifyEnd(lastBranchCommit);
        } else {
            branchAnalyzers.get(branchId).notifyEnd();
        }
    }

    /**
     * Retrieve all {@link Smell} presence on the current branch parent commit.
     *
     * @param branchId The branch identifier.
     * @return A {@link List} of present {@link Smell}.
     */
    private List<Smell> retrieveBranchParentSmells(int branchId) {
        String parentSmellsQuery = branchQueries.parentCommitSmellsQuery(projectId, branchId, smellType);
        List<Map<String, Object>> results = persistence.query(parentSmellsQuery);
        return toSmells(results);
    }

    /**
     * Fetch the SmellPresences of the given commit.
     *
     * @param mergedCommitId Identifier of the commit being merged.
     * @return A {@link List} of {@link Smell}.
     */
    private List<Smell> retrieveMergedCommitSmells(int mergedCommitId) {
        String lastCommitSmellsQuery = smellQueries.commitSmellsQuery(projectId, String.valueOf(mergedCommitId), smellType);
        List<Map<String, Object>> results = persistence.query(lastCommitSmellsQuery);
        return toSmells(results);
    }

    /**
     * convert the list of results to a list of Smell.
     *
     * @param results The Tandoori persistence's results to convert.
     * @return A {@link List} of {@link Smell}.
     */
    private static List<Smell> toSmells(List<Map<String, Object>> results) {
        ArrayList<Smell> smells = new ArrayList<>();
        for (Map<String, Object> result : results) {
            smells.add(Smell.fromTandooriInstance(result));
        }
        return smells;
    }

    /**
     * Gives the identifier of the merged commit, if any.
     *
     * @param commit The commit to test.
     * @return An {@link Integer} identifying the merged commit, null if commit is not a merge commit.
     */
    private Integer getMergedCommitId(Commit commit) {
        List<Map<String, Object>> result = persistence.query(commitQueries.mergedCommitIdQuery(projectId, commit));
        return (result.isEmpty() || result.get(0).isEmpty()) ? null : (Integer) result.get(0).get("id");
    }

    /**
     * Tells if the current commit is the last commit in the branch.
     *
     * @param commit        The commit to test.
     * @param currentBranch The branch to check about.
     * @return True if the commit is the last commit of this branch, false otherwise.
     */
    private boolean isLastBranchCommit(Commit commit, int currentBranch) {
        return branchLastCommitSha.containsKey(currentBranch) && branchLastCommitSha.get(currentBranch).equals(commit.sha);
    }

    /**
     * Retrieve the branch on which the commit is located.
     *
     * @param commit The commit to find a branch for.
     * @return The branch ordinal in the project.
     * @throws BranchNotFoundException If no branch could be found for this commit.
     *                                 This can happen until there are no commit gap anymore on Paprika result.
     */
    private int fetchCommitBranch(Commit commit) throws BranchNotFoundException {
        List<Map<String, Object>> result = persistence.query(branchQueries.idFromCommitQueryStatement(projectId, commit));
        if (result.isEmpty() || result.get(0).get("id") == null) {
            throw new BranchNotFoundException(projectId, commit.sha);
        }
        return (int) result.get(0).get("id");
    }
}
