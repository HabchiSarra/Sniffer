package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
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
public class BranchAwareSmellTypeAnalysis extends AbstractSmellTypeAnalysis implements Query {
    private static final Logger logger = LoggerFactory.getLogger(BranchAwareSmellTypeAnalysis.class.getName());

    private final Iterator<Map<String, Object>> smells;
    private String smellType;
    private final SmellDuplicationChecker duplicationChecker;

    private final Map<Integer, BranchAnalyzer> branchAnalyzers;
    private final Map<Integer, String> branchLastCommitSha;

    public BranchAwareSmellTypeAnalysis(int projectId, Persistence persistence, Iterator<Map<String, Object>> smells,
                                        String smellType, SmellDuplicationChecker duplicationChecker) {
        super(LoggerFactory.getLogger(OrdinalSmellTypeAnalysis.class.getName()), projectId, persistence);
        this.smells = smells;
        this.smellType = smellType;
        this.duplicationChecker = duplicationChecker;

        branchAnalyzers = new HashMap<>();
        branchLastCommitSha = new HashMap<>();
    }


    @Override
    public void query() throws QueryException {
        Smell smell;
        Commit previousCommit;
        Commit commit = Commit.EMPTY;
        int currentBranch;

        Map<String, Object> instance;
        while (smells.hasNext()) {
            instance = smells.next();
            previousCommit = commit;
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

            // We ensure to merge SmellPresence from the merged branch if necessary.
            if (!previousCommit.equals(commit) && isMergeCommit(commit)) {
                addSmellsToMergeCommit(commit, currentBranch);
            }

            // We then submit the new smell to analysis
            branchAnalyzers.get(currentBranch).addSmellCommit(smell, commit);

            // If we reach the last branch commit, we will finalize the branch analysis,
            // i.e. setting introductions and refactoring for the last branch commit.
            if (!previousCommit.equals(commit) && isLastBranchCommit(commit, currentBranch)) {
                logger.debug("[" + projectId + "] => Finalizing branch: " + currentBranch);
                branchAnalyzers.get(currentBranch).finalizeAnalysis();
                branchAnalyzers.remove(currentBranch);
            }
        }

        // We should'nt have to finalize any branch since every one should have a last commit.
        for (int branchId : branchAnalyzers.keySet()) {
            logger.warn("Finalizing branch without last commit: " + branchId);
            branchAnalyzers.get(branchId).finalizeAnalysis();
        }
    }

    /**
     * When we change our commit, we check if it is a merge commit,
     * if we have one, we will retrieve all smells from the merged branch last commit, in order
     * to keep a realistic track of the introductions and refactoring.
     *
     * @param merge         The commit in which the branch is merged.
     * @param currentBranch The branch to add commits onto.
     */
    private void addSmellsToMergeCommit(Commit merge, int currentBranch) {
        branchAnalyzers.get(currentBranch).addCurrentSmells(retrieveMergedBranchFinalSmells(merge));
    }

    /**
     * Create a new {@link BranchAnalyzer} and add is to the branchAnalyzers,
     * With all the smells from its parent commit.
     *
     * @param currentBranch Identifier of the branch to initialize.
     */
    private void initializeBranch(int currentBranch) {
        BranchAnalyzer analyzer = new BranchAnalyzer(projectId, persistence, duplicationChecker, false);
        analyzer.addCurrentSmells(retrieveBranchParentSmells(currentBranch));
        branchAnalyzers.put(currentBranch, analyzer);

        List<Map<String, Object>> query = persistence.query(persistence.branchLastCommitShaQuery(projectId, currentBranch));
        if (query.isEmpty()) {
            logger.warn("No merge commit found for branch: " + currentBranch);
        } else {
            branchLastCommitSha.put(currentBranch, (String) query.get(0).get("sha1"));
        }

    }

    /**
     * Retrieve all {@link Smell} presence on the current branch parent commit.
     *
     * @param branchId The branch identifier.
     * @return A {@link List} of present {@link Smell}.
     */
    private List<Smell> retrieveBranchParentSmells(int branchId) {
        List<Map<String, Object>> results = persistence.query(persistence.branchParentCommitSmellPresencesQuery(projectId, branchId));
        return toSmells(results);
    }

    /**
     * Fetch the SmellPresences of the given branch's last commit.
     *
     * @param merge The commit in which the branch is merged.
     * @return A {@link List} of {@link Smell}.
     */
    private List<Smell> retrieveMergedBranchFinalSmells(Commit merge) {
        List<Map<String, Object>> results = persistence.query(persistence.branchLastCommitSmellsQuery(projectId, merge));
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
     * Tells if the current commit is a merge commit.
     *
     * @param commit The commit to test.
     * @return True if it is a merge commit, false otherwise.
     */
    private boolean isMergeCommit(Commit commit) {
        List<Map<String, Object>> result = persistence.query(persistence.mergedBranchIdQuery(projectId, commit));
        return !result.isEmpty();
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
        List<Map<String, Object>> result = persistence.query(persistence.branchOrdinalQueryStatement(projectId, commit));
        if (result.isEmpty()) {
            throw new BranchNotFoundException(projectId, commit.sha);
        }
        return (int) result.get(0).get("ordinal");
    }
}
