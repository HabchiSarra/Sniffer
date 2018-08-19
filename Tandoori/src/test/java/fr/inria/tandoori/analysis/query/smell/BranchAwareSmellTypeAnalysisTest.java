package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.QueryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BranchAwareSmellTypeAnalysisTest extends SmellTypeAnalysis {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(branchQueries.idFromCommitQueryStatement(anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> branchIdStatement(
                        invocation.getArgument(0),
                        ((Commit) invocation.getArgument(1)).sha));
        when(branchQueries.commitOrdinalQuery(anyInt(), anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> branchCommitOrdinalStatement(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        ((Commit) invocation.getArgument(2)).sha));
        when(branchQueries.mergedBranchIdQuery(anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> mergedBranchIdQuery(invocation.getArgument(0),
                        ((Commit) invocation.getArgument(1)).sha));
        when(branchQueries.lastCommitSmellsQuery(anyInt(), any(Commit.class), anyString())).then((Answer<String>)
                invocation -> branchLastCommitSmellStatement(invocation.getArgument(0),
                        ((Commit) invocation.getArgument(1)).sha, invocation.getArgument(2)));
        when(branchQueries.parentCommitSmellsQuery(anyInt(), anyInt(), anyString())).then((Answer<String>)
                invocation -> branchParentCommitSmellStatement(invocation.getArgument(0),
                        invocation.getArgument(1), invocation.getArgument(2)));
        when(branchQueries.lastCommitShaQuery(anyInt(), anyInt())).then((Answer<String>)
                invocation -> branchLastCommitShaStatement(invocation.getArgument(0),
                        invocation.getArgument(1)));
        when(branchQueries.shaFromOrdinalQuery(anyInt(), anyInt(), anyInt())).then((Answer<String>)
                invocation -> commitShaFromOrdinalStatement(invocation.getArgument(0),
                        invocation.getArgument(1), invocation.getArgument(2)));
    }

    private static String mergedBranchIdQuery(int projectId, String sha) {
        return "mergedBranchIdQuery-" + projectId + "-" + sha;
    }

    private static String branchIdStatement(int projectId, String sha) {
        return "BranchIdQueryStatement-" + projectId + "-" + sha;
    }

    private static String branchCommitOrdinalStatement(int projectId, int branchId, String sha) {
        return "BranchCommitOrdinalQueryStatement-" + projectId + "-" + branchId + "-" + sha;
    }

    private static String branchLastCommitSmellStatement(int projectId, String sha, String smellType) {
        return "branchLastCommitSmellStatement-" + projectId + "-" + sha + "-" + smellType;
    }

    private static String branchParentCommitSmellStatement(int projectId, int branchId, String smellType) {
        return "parentCommitSmellsQuery-" + projectId + "-" + branchId + "-" + smellType;
    }

    private static String branchLastCommitShaStatement(int projectId, int branchId) {
        return "branchLastCommitShaStatement-" + projectId + "-" + branchId;
    }

    private static String commitShaFromOrdinalStatement(int projectId, int branchId, int commitOrdinal) {
        return "commitShaFromOrdinalStatement-" + projectId + "-" + branchId + "-" + commitOrdinal;
    }

    private BranchAwareSmellTypeAnalysis getAnalysis() {
        return new BranchAwareSmellTypeAnalysis(projectId, persistence, smellList.iterator(), smellType,
                duplicationChecker, commitQueries, smellQueries, branchQueries);
    }

    private void mockCommitBranch(Commit commit, int branch, int commitOrdinal) {
        List<Map<String, Object>> branchResult = new ArrayList<>();
        branchResult.add(Collections.singletonMap("id", branch));
        doReturn(branchResult).when(persistence).query(branchIdStatement(projectId, commit.sha));

        List<Map<String, Object>> ordinalResult = new ArrayList<>();
        ordinalResult.add(Collections.singletonMap("ordinal", commitOrdinal));
        doReturn(ordinalResult).when(persistence).query(branchCommitOrdinalStatement(projectId, branch, commit.sha));

        List<Map<String, Object>> commitResult = new ArrayList<>();
        commitResult.add(Collections.singletonMap("sha1", commit.sha));
        doReturn(commitResult).when(persistence).query(commitShaFromOrdinalStatement(projectId, branch, commitOrdinal));
    }

    private void mockMergeCommit(Commit merge, int branchId) {
        List<Map<String, Object>> branchResult = new ArrayList<>();
        branchResult.add(Collections.singletonMap("id", branchId));
        doReturn(branchResult).when(persistence).query(mergedBranchIdQuery(projectId, merge.sha));
    }

    private void mockLastBranchCommitSmells(Commit merge, Smell... smells) {
        List<Map<String, Object>> smellResult = new ArrayList<>();
        Map<String, Object> content;
        for (Smell smell : smells) {
            content = new HashMap<>();
            content.put("instance", smell.instance);
            content.put("type", smell.type);
            content.put("file", smell.file);
            smellResult.add(content);
        }
        doReturn(smellResult).when(persistence).query(branchLastCommitSmellStatement(projectId, merge.sha, smellType));
    }


    private void mockBranchParentCommitSmells(int branchId, List<Smell> smells) {
        List<Map<String, Object>> smellResult = new ArrayList<>();
        Map<String, Object> content;
        for (Smell smell : smells) {
            content = new HashMap<>();
            content.put("instance", smell.instance);
            content.put("type", smell.type);
            content.put("file", smell.file);
            smellResult.add(content);
        }
        doReturn(smellResult).when(persistence).query(branchParentCommitSmellStatement(projectId, branchId, smellType));
    }

    private void mockBranchParentCommitSmells(int branchId, Smell... smells) {
        this.mockBranchParentCommitSmells(branchId, Arrays.asList(smells));
    }

    private void mockLastBranchCommit(int branchId, Commit commit) {
        List<Map<String, Object>> commitResult = new ArrayList<>();
        commitResult.add(Collections.singletonMap("sha1", commit.sha));
        doReturn(commitResult).when(persistence).query(branchLastCommitShaStatement(projectId, branchId));
    }

    @Test(expected = QueryException.class)
    public void testNoEndCommitFoundWillThrow() throws QueryException {
        addSmell(firstCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);

        mockNoEndCommit();
        getAnalysis().query();
    }

    @Test
    public void handlePresenceAndIntroductionOnSingleCommitAlsoLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);

        mockEndCommit(firstCommit.sha);
        getAnalysis().query();

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    @Test
    public void handlePresenceAndIntroductionOnSingleCommitNotLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        String lastCommitSha = "another";
        mockCommitBranch(firstCommit, 0, 0);

        mockEndCommit(lastCommitSha);
        getAnalysis().query();

        verify(persistence, times(4)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, lastCommitSha, firstSmell, SmellCategory.REFACTOR);
    }

    @Test
    public void handleIntroductionAndRefactoring() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(7)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.REFACTOR);
    }

    @Test
    public void handleIntroductionOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(7)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    @Test
    public void handleRefactorOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(secondCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.REFACTOR);
    }

    @Test
    public void handleNoChange() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(4)).addStatements(any());
        // We have only one smell insertion here since we check for existence in the previous commit.
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
    }

    @Test
    public void handleCommitGap() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(thirdCommit, 0, 1);

        String gapCommitSha = "someSha";
        mockGapCommit(gapCommitSha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // 3rd commit is counted as consecutive to the first.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);
    }

    @Test
    public void handleRenamedSmell() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(5)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);

        // Check that the renamed commit has a set parentInstance
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(secondSmell, renamed);
        assertEquals(firstSmell.instance, renamed.parentInstance);
    }

    @Test
    public void handleRenamedSmellMultipleCommits() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);
        mockCommitBranch(thirdCommit, 0, 2);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(6)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        // Check that the renamed commit has a set parentInstance
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(secondSmell, renamed);
        assertEquals(firstSmell.instance, renamed.parentInstance);

        // We won't introduce the same rename multiple times, as before.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);
    }

    @Test
    public void handleFirstCommitIsNotTheFirstOrdinal() throws QueryException {
        addSmell(thirdCommit, firstSmell);
        mockCommitBranch(thirdCommit, 0, 0);

        mockGapCommit(firstCommit.sha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * *   0-A (      3)
     * |\
     * * | 0-B (1      )
     * | * 1-A (   2, 3)
     * | * 1-B (   2, 3)
     * | * 1-C (   2, 3)
     * |/
     * *   0-C (1, 2, 3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testAnotherBranchSeparateSmells() throws QueryException {
        Commit branch0commit0 = new Commit("0-A", 0);
        Commit branch0commit1 = new Commit("0-B", 1);
        Commit branch0commit2 = new Commit("0-C", 5);
        Commit branch1commit0 = new Commit("1-A", 2);
        Commit branch1commit1 = new Commit("1-B", 3);
        Commit branch1commit2 = new Commit("1-C", 4);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        addSmell(branch0commit0, thirdSmell);
        addSmell(branch0commit1, firstSmell);

        addSmell(branch1commit0, secondSmell);
        addSmell(branch1commit0, thirdSmell);
        addSmell(branch1commit1, secondSmell);
        addSmell(branch1commit1, thirdSmell);
        addSmell(branch1commit2, secondSmell);
        addSmell(branch1commit2, thirdSmell);

        addSmell(branch0commit2, firstSmell);
        addSmell(branch0commit2, secondSmell);
        addSmell(branch0commit2, thirdSmell);

        mockCommitBranch(branch0commit0, 0, 0);
        mockCommitBranch(branch0commit1, 0, 1);
        mockCommitBranch(branch0commit2, 0, 2);
        mockLastBranchCommit(0, branch0commit2);
        mockCommitBranch(branch1commit0, 1, 0);
        mockCommitBranch(branch1commit1, 1, 1);
        mockCommitBranch(branch1commit2, 1, 2);
        mockLastBranchCommit(1, branch1commit2);

        mockEndCommit(branch0commit2.sha);
        mockMergeCommit(branch0commit2, 1);
        mockLastBranchCommitSmells(branch0commit2, secondSmell, thirdSmell);
        mockBranchParentCommitSmells(1, thirdSmell);

        getAnalysis().query();

        verify(persistence, times(18)).addStatements(any());

        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit0.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit0.sha, thirdSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit1.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit1.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit1.sha, thirdSmell, SmellCategory.REFACTOR);

        // Forked branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit0.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit0.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit0.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit1.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit1.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit2.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch1commit2.sha, thirdSmell, SmellCategory.PRESENCE);

        // Merge commit
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit2.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit2.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, branch0commit2.sha, thirdSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * .   A (1      )
     * |\
     * . | B (1      )
     * | . D (1      )
     * . | C (1      )
     * | . E (1, 2   )
     * |/
     * .   F (1, 2   ) [merge]
     * |\
     * | . G (1      )
     * . | H (1, 2, 3)
     * |/
     * .   I (1,    3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testSuccessiveBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("1-E", 5);
        Commit F = new Commit("0-F", 6);
        Commit G = new Commit("2-G", 7);
        Commit H = new Commit("0-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, firstSmell);
        addSmell(D, firstSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(E, secondSmell);
        addSmell(F, firstSmell);
        addSmell(F, secondSmell);
        addSmell(G, firstSmell);
        addSmell(H, firstSmell);
        addSmell(H, secondSmell);
        addSmell(H, thirdSmell);
        addSmell(I, firstSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 1, 1);
        mockCommitBranch(F, 0, 3);
        mockCommitBranch(G, 2, 0);
        mockCommitBranch(H, 0, 4);
        mockCommitBranch(I, 0, 5);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, E);
        mockLastBranchCommit(2, G);

        mockEndCommit(I.sha);
        mockMergeCommit(F, 1);
        mockMergeCommit(I, 2);
        mockLastBranchCommitSmells(F, firstSmell, secondSmell);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell, thirdSmell);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell, secondSmell);

        getAnalysis().query();

        verify(persistence, times(22)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.INTRODUCTION);

        // First merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);

        // Second branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.REFACTOR);

        // Second branch's master
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Second merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * .   A (1      )
     * |\
     * . | B (1      )
     * | . D (1      )
     * . | C (1      )
     * | . E (1, 2   )
     * |/|
     * . | F (1     3) [merge]
     * | |
     * | . G (1, 2   )
     * . | H (1,    3)
     * |/
     * .   I (1,    3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testContinuingBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("1-E", 5);
        Commit F = new Commit("0-F", 6);
        Commit G = new Commit("1-G", 7);
        Commit H = new Commit("0-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, firstSmell);
        addSmell(D, firstSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(E, secondSmell);
        addSmell(F, firstSmell);
        addSmell(F, thirdSmell);
        addSmell(G, firstSmell);
        addSmell(G, secondSmell);
        addSmell(H, firstSmell);
        addSmell(H, thirdSmell);
        addSmell(I, firstSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 1, 1);
        mockCommitBranch(F, 0, 3);
        mockCommitBranch(G, 1, 2);
        mockCommitBranch(H, 0, 4);
        mockCommitBranch(I, 0, 5);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, G);

        mockEndCommit(I.sha);
        mockMergeCommit(I, 1);
        mockLastBranchCommitSmells(F, firstSmell, secondSmell);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell, thirdSmell);
        mockBranchParentCommitSmells(1, firstSmell);

        getAnalysis().query();

        verify(persistence, times(21)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);

        // Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * .     A (1      )
     * |\
     * . |   B (1      )
     * | .   D (1      )
     * . |\  C (1      )
     * | | . E (1, 2   )
     * | . | F (1,    3)
     * | |/
     * | .   G (1, 2, 3) [merge]
     * . |   H (       )
     * |/
     * .     I (   2, 3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testOverlappingBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("2-E", 5);
        Commit F = new Commit("1-F", 6);
        Commit G = new Commit("1-G", 7);
        Commit H = new Commit("0-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, firstSmell);
        addSmell(D, firstSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(E, secondSmell);
        addSmell(F, firstSmell);
        addSmell(F, thirdSmell);
        addSmell(G, firstSmell);
        addSmell(G, secondSmell);
        addSmell(G, thirdSmell);
        addSmell(I, secondSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 2, 0);
        mockCommitBranch(F, 1, 1);
        mockCommitBranch(G, 1, 2);
        mockCommitBranch(H, 0, 3);
        mockCommitBranch(I, 0, 4);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, G);
        mockLastBranchCommit(2, E);

        mockEndCommit(I.sha);
        mockMergeCommit(I, 1);
        mockMergeCommit(G, 2);
        mockLastBranchCommitSmells(G, firstSmell, secondSmell);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell, thirdSmell);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();

        verify(persistence, times(21)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.REFACTOR);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, thirdSmell, SmellCategory.PRESENCE);

        // Second Branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.INTRODUCTION);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     *   .   A (1      )
     *  /|\
     * | . | B (1      )
     * | | . D (1, 2   )
     * | . | C (1      )
     * . | | E (1,    3)
     * | | . F (1, 2   )
     * | | |
     * \ | . G (1, 2   )
     *   . | H (1,    3) [merge]
     *   |/
     *   .   I (1, 2, 3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testParallelBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("2-E", 5);
        Commit F = new Commit("1-F", 6);
        Commit G = new Commit("1-G", 7);
        Commit H = new Commit("0-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, firstSmell);
        addSmell(D, firstSmell);
        addSmell(D, secondSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(E, thirdSmell);
        addSmell(F, firstSmell);
        addSmell(F, secondSmell);
        addSmell(G, firstSmell);
        addSmell(G, secondSmell);
        addSmell(H, firstSmell);
        addSmell(H, thirdSmell);
        addSmell(I, firstSmell);
        addSmell(I, secondSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 2, 0);
        mockCommitBranch(F, 1, 1);
        mockCommitBranch(G, 1, 2);
        mockCommitBranch(H, 0, 3);
        mockCommitBranch(I, 0, 4);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, G);
        mockLastBranchCommit(2, E);

        mockEndCommit(I.sha);
        mockMergeCommit(I, 1);
        mockMergeCommit(H, 2);
        mockLastBranchCommitSmells(H, firstSmell, thirdSmell);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();

        verify(persistence, times(22)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First Branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);

        // First Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);

        // Second Branch
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Second Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     *   .   A (1,     )
     *   |\
     *   . | B (1,     )
     *  /| . D (1, 2,  )
     * | . | C (1,     )
     * . |/  E (1,     )
     * | .   F (1, 2, 3) [merge]
     * . |   G (1,     )
     *  \|
     *   .   H (1,     ) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testCrossedBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("2-D", 3);
        Commit E = new Commit("1-E", 5);
        Commit F = new Commit("0-F", 6);
        Commit G = new Commit("1-G", 7);
        Commit H = new Commit("0-H", 8);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, firstSmell);
        addSmell(D, firstSmell);
        addSmell(D, secondSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(F, firstSmell);
        addSmell(F, secondSmell);
        addSmell(F, thirdSmell);
        addSmell(G, firstSmell);
        addSmell(H, firstSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 2, 0);
        mockCommitBranch(E, 1, 0);
        mockCommitBranch(F, 0, 3);
        mockCommitBranch(G, 1, 1);
        mockCommitBranch(H, 0, 4);
        mockLastBranchCommit(0, H);
        mockLastBranchCommit(1, G);
        mockLastBranchCommit(2, D);

        mockEndCommit(H.sha);
        mockMergeCommit(H, 1);
        mockMergeCommit(F, 2);
        mockLastBranchCommitSmells(H, firstSmell);
        mockLastBranchCommitSmells(F, firstSmell, secondSmell);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();

        verify(persistence, times(19)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First Branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);

        // First Merge
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Second Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);

        // Second Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, secondSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * .   A (       )
     * |\
     * . | B (1,     )
     * | . D (   2,  )
     * . | C (1,     )
     * |\|
     * | . E (1, 2,  ) [merge]
     * . | F (1,    3)
     * | |
     * | . G (1, 2,  )
     * . | H (1,    3)
     * |/
     * .   I (1, 2, 3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMergeBackAndForthBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("1-E", 5);
        Commit F = new Commit("0-F", 6);
        Commit G = new Commit("1-G", 7);
        Commit H = new Commit("0-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(B, firstSmell);
        addSmell(D, secondSmell);
        addSmell(C, firstSmell);
        addSmell(E, firstSmell);
        addSmell(E, secondSmell);
        addSmell(F, firstSmell);
        addSmell(F, thirdSmell);
        addSmell(G, firstSmell);
        addSmell(G, secondSmell);
        addSmell(H, firstSmell);
        addSmell(H, thirdSmell);
        addSmell(I, firstSmell);
        addSmell(I, secondSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 1, 1);
        mockCommitBranch(F, 0, 3);
        mockCommitBranch(G, 1, 2);
        mockCommitBranch(H, 0, 4);
        mockCommitBranch(I, 0, 5);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, G);

        mockEndCommit(I.sha);
        mockMergeCommit(I, 1);
        mockMergeCommit(E, 0);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell);
        mockLastBranchCommitSmells(E, firstSmell);
        mockBranchParentCommitSmells(1, Collections.emptyList());

        getAnalysis().query();

        verify(persistence, times(20)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);

        // Branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * .     A (1,     )
     * |\
     * . |   B (       )
     * | .   D (1, 2,  )
     * . |\  C (       )
     * | | . E (   2,  )
     * | . | F (1, 2,  )
     * | |/
     * |/|
     * . |   G (   2,  ) [merge]
     * | .   H (1, 2, 3)
     * |/
     * .     I (   2, 3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMergeNotInDirectParentBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 4);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("2-E", 5);
        Commit F = new Commit("1-F", 6);
        Commit G = new Commit("0-G", 7);
        Commit H = new Commit("1-H", 8);
        Commit I = new Commit("0-I", 9);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(D, firstSmell);
        addSmell(D, secondSmell);
        addSmell(E, secondSmell);
        addSmell(F, firstSmell);
        addSmell(F, secondSmell);
        addSmell(G, secondSmell);
        addSmell(H, firstSmell);
        addSmell(H, secondSmell);
        addSmell(H, thirdSmell);
        addSmell(I, secondSmell);
        addSmell(I, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 1, 0);
        mockCommitBranch(E, 2, 0);
        mockCommitBranch(F, 1, 1);
        mockCommitBranch(G, 0, 3);
        mockCommitBranch(H, 1, 2);
        mockCommitBranch(I, 0, 4);
        mockLastBranchCommit(0, I);
        mockLastBranchCommit(1, H);
        mockLastBranchCommit(2, E);

        mockEndCommit(I.sha);
        mockMergeCommit(I, 1);
        mockMergeCommit(G, 2);
        mockLastBranchCommitSmells(I, firstSmell, secondSmell, thirdSmell);
        mockLastBranchCommitSmells(G, secondSmell);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell, secondSmell);

        getAnalysis().query();

        verify(persistence, times(21)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.REFACTOR);

        // First Branch
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Second Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);

        // First Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);

        // Second Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);

    }

    /**
     * <pre><code>
     * .       A (1, 2,  )
     * |\
     * | .     B (1,     )
     * | |\
     * | | |\
     * | | | . C (1,    3)
     * | | |/
     * | | .   D (1,    3) [merge]
     * | |/
     * | .     E (1,    3) [merge]
     * |/
     * .       F (1,    3) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMergeIsLastBranchCommitBranchesSeparateSmells() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("1-B", 1);
        Commit C = new Commit("3-C", 3);
        Commit D = new Commit("2-D", 4);
        Commit E = new Commit("1-E", 5);
        Commit F = new Commit("0-F", 6);
        Smell thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(A, secondSmell);
        addSmell(B, firstSmell);
        addSmell(C, firstSmell);
        addSmell(C, thirdSmell);
        addSmell(D, firstSmell);
        addSmell(D, thirdSmell);
        addSmell(E, firstSmell);
        addSmell(E, thirdSmell);
        addSmell(F, firstSmell);
        addSmell(F, thirdSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 1, 0);
        mockCommitBranch(C, 3, 0);
        mockCommitBranch(D, 2, 0);
        mockCommitBranch(E, 1, 1);
        mockCommitBranch(F, 0, 1);
        mockLastBranchCommit(0, F);
        mockLastBranchCommit(1, E);
        mockLastBranchCommit(2, D);
        mockLastBranchCommit(3, C);

        mockEndCommit(F.sha);
        mockMergeCommit(F, 1);
        mockMergeCommit(E, 2);
        mockMergeCommit(D, 3);
        mockLastBranchCommitSmells(F, firstSmell, thirdSmell);
        mockLastBranchCommitSmells(E, firstSmell, thirdSmell);
        mockLastBranchCommitSmells(D, firstSmell, thirdSmell);
        mockBranchParentCommitSmells(1, firstSmell, secondSmell);
        mockBranchParentCommitSmells(2, firstSmell);
        mockBranchParentCommitSmells(3, firstSmell);

        getAnalysis().query();

        verify(persistence, times(19)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.INTRODUCTION);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, secondSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, thirdSmell, SmellCategory.PRESENCE);

        // Second Branch
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Third Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, thirdSmell, SmellCategory.PRESENCE);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
    }


    private void debugSmellInsertions() {
        ArgumentCaptor<Smell> instancesCaptor = ArgumentCaptor.forClass(Smell.class);

        ArgumentCaptor<Smell> instancesCateCaptor = ArgumentCaptor.forClass(Smell.class);
        ArgumentCaptor<String> shaCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SmellCategory> typeCaptor = ArgumentCaptor.forClass(SmellCategory.class);

        verify(smellQueries, atLeastOnce()).smellInsertionStatement(eq(projectId), instancesCaptor.capture());
        verify(smellQueries, atLeastOnce()).smellCategoryInsertionStatement(eq(projectId),
                shaCaptor.capture(), instancesCateCaptor.capture(), typeCaptor.capture());

        List<Smell> instances = instancesCaptor.getAllValues();
        System.out.println("--------");
        for (Smell instance : instances) {
            System.out.println("Call to insertSmellInstance: " + instance);
        }
        System.out.println("--------");

        List<Smell> instancesCategory = instancesCateCaptor.getAllValues();
        List<String> shas = shaCaptor.getAllValues();
        List<SmellCategory> categories = typeCaptor.getAllValues();
        for (int i = 0; i < instancesCategory.size(); i++) {
            System.out.println("Call to insertSmellCategory: " + shas.get(i) + " - " + categories.get(i) + " - " +
                    instancesCategory.get(i).instance
            );
        }
        System.out.println("--------");
    }
}