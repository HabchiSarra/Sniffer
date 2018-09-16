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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BranchAwareSmellTypeAnalysisTest extends SmellTypeAnalysisTestCase {

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
        when(branchQueries.shaFromOrdinalQuery(anyInt(), anyInt(), anyInt(), anyBoolean())).then((Answer<String>)
                invocation -> commitShaFromOrdinalStatement(invocation.getArgument(0),
                        invocation.getArgument(1), invocation.getArgument(2)));
        when(commitQueries.mergedCommitIdQuery(anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> mergedCommitStatement(invocation.getArgument(0),
                        ((Commit) invocation.getArgument(1)).sha));
        when(smellQueries.commitSmellsQuery(anyInt(), anyString(), anyString())).then((Answer<String>)
                invocation -> commitSmellsStatement(invocation.getArgument(0),
                        Integer.valueOf(invocation.getArgument(1)), invocation.getArgument(2)));
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

    private static String mergedCommitStatement(int projectId, String sha) {
        return "commitShaFromOrdinalStatement-" + projectId + "-" + sha;
    }

    private static String commitSmellsStatement(int projectId, int commitId, String smellType) {
        return "commitSmellsStatement-" + projectId + "-" + commitId + "-" + smellType;
    }


    private BranchAwareSmellTypeAnalysis getAnalysis() {
        return new BranchAwareSmellTypeAnalysis(projectId, persistence, smellList.iterator(), smellType,
                duplicationChecker, commitQueries, smellQueries, branchQueries);
    }

    protected void mockGapCommit(String sha1, int branch, int ordinal) {
        mockCommitPosition(sha1, commitShaFromOrdinalStatement(projectId, branch, ordinal));
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

    private void mockMergeCommit(Commit merge, Commit merged) {
        List<Map<String, Object>> mergedResult = new ArrayList<>();
        mergedResult.add(Collections.singletonMap("id", merged.ordinal));
        doReturn(mergedResult).when(persistence).query(mergedCommitStatement(projectId, merge.sha));
    }

    private void mockCommitSmells(Commit commit, Smell... smells) {
        List<Map<String, Object>> smellResult = new ArrayList<>();
        Map<String, Object> content;
        for (Smell smell : smells) {
            addSmell(commit, smell);
            content = new HashMap<>();
            content.put("id", fetchId(smell));
            content.put("instance", smell.instance);
            content.put("type", smell.type);
            content.put("file", smell.file);
            smellResult.add(content);
        }
        doReturn(smellResult).when(persistence).query(commitSmellsStatement(projectId, commit.ordinal, smellType));
    }

    private void mockBranchParentCommitSmells(int branchId, List<Smell> smells) {
        List<Map<String, Object>> smellResult = generateSmellsMap(smells);
        doReturn(smellResult).when(persistence).query(branchParentCommitSmellStatement(projectId, branchId, smellType));
    }

    private List<Map<String, Object>> generateSmellsMap(List<Smell> smells) {
        List<Map<String, Object>> smellResult = new ArrayList<>();
        Map<String, Object> content;
        for (Smell smell : smells) {
            content = new HashMap<>();
            content.put("id", fetchId(smell));
            content.put("instance", smell.instance);
            content.put("type", smell.type);
            content.put("file", smell.file);
            if (smell.parent != null) {
                content.put("parent_instance", smell.parent.instance);
                content.put("parent_type", smell.parent.type);
                content.put("parent_file", smell.parent.file);
            }
            smellResult.add(content);
        }
        return smellResult;
    }

    private int fetchId(Smell smell) {
        List<Map<String, Object>> result = persistence.query(smellQueries.smellIdQuery(projectId, smell));
        return (int) result.get(0).get("id");
    }

    private void mockBranchParentCommitSmells(int branchId, Smell... smells) {
        this.mockBranchParentCommitSmells(branchId, Arrays.asList(smells));
    }

    private void mockLastBranchCommit(int branchId, Commit commit) {
        mockLastBranchCommit(branchId, commit.sha);
    }

    private void mockLastBranchCommit(int branchId, String sha) {
        List<Map<String, Object>> commitResult = new ArrayList<>();
        commitResult.add(Collections.singletonMap("sha1", sha));
        doReturn(commitResult).when(persistence).query(branchLastCommitShaStatement(projectId, branchId));
    }

    @Test(expected = QueryException.class)
    public void testNoEndCommitFoundWillThrow() throws QueryException {
        addSmell(firstCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);

        mockNoEndCommit();
        getAnalysis().query();
    }

    /**
     * <pre><code>
     * * A (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandlePresenceAndIntroductionOnSingleCommitAlsoLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);

        mockLastBranchCommit(0, firstCommit);
        getAnalysis().query();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(2)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B ()
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandlePresenceAndIntroductionOnSingleCommitNotLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        String lastCommitSha = "another";

        mockCommitBranch(firstCommit, 0, 0);
        mockGapCommit(lastCommitSha, 0, 1);

        mockLastBranchCommit(0, lastCommitSha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, lastCommitSha, firstSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1   )
     * |
     * * B (   2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleIntroductionAndRefactoring() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockLastBranchCommit(0, secondCommit);
        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(5)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B (1, 2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleIntroductionOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockLastBranchCommit(0, secondCommit);
        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(5)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1, 2)
     * |
     * * B (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleRefactorOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(secondCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockLastBranchCommit(0, secondCommit);
        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(6)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleNoChange() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        mockLastBranchCommit(0, secondCommit);
        getAnalysis().query();

        verify(persistence, times(1)).execute(any());
        // We have only one smell insertion here since we check for existence in the previous commit.
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * * A (1, 2)
     * |
     * x Missing commit B
     * |
     * * C (  2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleCommitGap() throws QueryException {
        Commit someCommit = new Commit("A", 57);
        Commit anotherCommit = new Commit("B", 60);

        mockCommitSmells(someCommit, firstSmell, secondSmell);
        mockCommitSmells(anotherCommit, secondSmell);
        mockCommitBranch(someCommit, 0, 0);
        mockCommitBranch(anotherCommit, 0, 2);

        mockLastBranchCommit(0, anotherCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(6)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, someCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, someCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, someCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, someCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // 3rd commit is counted as consecutive to the first.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, anotherCommit.sha, secondSmell, SmellCategory.PRESENCE);

        // Since we couldn't find the missing commit, we put the smells in LostRefactor and no presence.
        verify(smellQueries).lostSmellCategoryInsertionStatement(projectId, firstSmell, SmellCategory.REFACTOR, someCommit.ordinal, anotherCommit.ordinal);
    }

    /**
     * <pre><code>
     * *  A (1  )
     * |
     * * B (  2) [1 -> 2]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleRenamedSmell() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        Smell expectedSecondSmell = mockSmellRenamed(secondCommit, secondSmell, firstSmell);
        mockSmellId(expectedSecondSmell);
        mockLastBranchCommit(0, secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());

        // Check that the renamed commit has a set parent
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(expectedSecondSmell, renamed);
        assertEquals(firstSmell, renamed.parent);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * *  A (1  )
     * |
     * * B (  2) [1 -> 2]
     * |
     * * C (  2)
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testHandleRenamedSmellMultipleCommits() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(secondCommit, 0, 1);
        mockCommitBranch(thirdCommit, 0, 2);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        Smell expectedSecondSmell = mockSmellRenamed(secondCommit, secondSmell, firstSmell);
        mockSmellId(expectedSecondSmell);
        mockSmellRenamed(thirdCommit, secondSmell, firstSmell);
        mockLastBranchCommit(0, thirdCommit.sha);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        // Check that the renamed commit has a set parent
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(expectedSecondSmell, renamed);
        assertEquals(firstSmell, renamed.parent);

        verify(persistence, times(4)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);
    }

    /**
     * * X (???)
     * |
     * * C (1)
     * @throws QueryException
     */
    @Test
    public void handleFirstCommitIsNotTheFirstOrdinal() throws QueryException {
        addSmell(thirdCommit, firstSmell);
        mockCommitBranch(thirdCommit, 0, 1);

        mockGapCommit(firstCommit.sha, 0, 0);
        mockLastBranchCommit(0, thirdCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(2)).addStatements(any());
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
        Commit A0 = new Commit("0-A", 0);
        Commit B0 = new Commit("0-B", 1);
        Commit C0 = new Commit("0-C", 5);
        Commit A1 = new Commit("1-A", 2);
        Commit B1 = new Commit("1-B", 3);
        Commit C1 = new Commit("1-C", 4);

        // Determines the input order
        mockCommitSmells(A0, thirdSmell);
        mockCommitSmells(B0, firstSmell);

        mockCommitSmells(A1, secondSmell, thirdSmell);
        mockCommitSmells(B1, secondSmell, thirdSmell);
        mockCommitSmells(C1, secondSmell, thirdSmell);

        mockCommitSmells(C0, firstSmell, secondSmell, thirdSmell);

        mockCommitBranch(A0, 0, 0);
        mockCommitBranch(B0, 0, 1);
        mockCommitBranch(C0, 0, 2);
        mockLastBranchCommit(0, C0);
        mockCommitBranch(A1, 1, 0);
        mockCommitBranch(B1, 1, 1);
        mockCommitBranch(C1, 1, 2);
        mockLastBranchCommit(1, C1);

        mockMergeCommit(C0, C1);
        mockBranchParentCommitSmells(1, thirdSmell);

        getAnalysis().query();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(15)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A0.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A0.sha, thirdSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, B0.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B0.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B0.sha, thirdSmell, SmellCategory.REFACTOR);

        // Forked branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A1.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A1.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A1.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B1.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B1.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C1.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C1.sha, thirdSmell, SmellCategory.PRESENCE);

        // Merge commit
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C0.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C0.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C0.sha, thirdSmell, SmellCategory.PRESENCE);
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("2-G", 6);
        Commit H = new Commit("0-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, firstSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell, secondSmell);
        mockCommitSmells(F, firstSmell, secondSmell);
        mockCommitSmells(G, firstSmell);
        mockCommitSmells(H, firstSmell, secondSmell, thirdSmell);
        mockCommitSmells(I, firstSmell, thirdSmell);

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

        mockMergeCommit(F, E);
        mockMergeCommit(I, G);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell, secondSmell);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(18)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
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
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Second merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("1-G", 6);
        Commit H = new Commit("0-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, firstSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell, secondSmell);
        mockCommitSmells(F, firstSmell, thirdSmell);
        mockCommitSmells(G, firstSmell, secondSmell);
        mockCommitSmells(H, firstSmell, thirdSmell);
        mockCommitSmells(I, firstSmell, thirdSmell);

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

        mockMergeCommit(I, G);
        mockBranchParentCommitSmells(1, firstSmell);

        getAnalysis().query();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(17)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);

        // Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, I.sha, thirdSmell, SmellCategory.PRESENCE);
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("2-E", 4);
        Commit F = new Commit("1-F", 5);
        Commit G = new Commit("1-G", 6);
        Commit H = new Commit("0-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, firstSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell, secondSmell);
        mockCommitSmells(F, firstSmell, thirdSmell);
        mockCommitSmells(G, firstSmell, secondSmell, thirdSmell);
        mockCommitSmells(I, secondSmell, thirdSmell);

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

        mockMergeCommit(I, G);
        mockMergeCommit(G, E);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();
        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(17)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.REFACTOR);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, thirdSmell, SmellCategory.PRESENCE);

        // Second Branch
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("2-E", 4);
        Commit F = new Commit("1-F", 5);
        Commit G = new Commit("1-G", 6);
        Commit H = new Commit("0-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, firstSmell, secondSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell, thirdSmell);
        mockCommitSmells(F, firstSmell, secondSmell);
        mockCommitSmells(G, firstSmell, secondSmell);
        mockCommitSmells(H, firstSmell, thirdSmell);
        mockCommitSmells(I, firstSmell, secondSmell, thirdSmell);

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

        mockMergeCommit(I, G);
        mockMergeCommit(H, E);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(19)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First Branch
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
     * | .   F (1, 2,  ) [merge]
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("2-D", 2);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("1-G", 6);
        Commit H = new Commit("0-H", 7);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, firstSmell, secondSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell);
        mockCommitSmells(F, firstSmell, secondSmell);
        mockCommitSmells(G, firstSmell);
        mockCommitSmells(H, firstSmell);

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

        mockMergeCommit(H, G);
        mockMergeCommit(F, D);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell);

        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(12)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);

        // First Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);

        // Second Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);

        // Second Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("1-G", 6);
        Commit H = new Commit("0-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(D, secondSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(E, firstSmell, secondSmell);
        mockCommitSmells(F, firstSmell, thirdSmell);
        mockCommitSmells(G, firstSmell, secondSmell);
        mockCommitSmells(H, firstSmell, thirdSmell);
        mockCommitSmells(I, firstSmell, secondSmell, thirdSmell);

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

        mockMergeCommit(I, G);
        mockMergeCommit(E, C);
        mockBranchParentCommitSmells(1, Collections.emptyList());

        getAnalysis().query();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(17)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, thirdSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, H.sha, thirdSmell, SmellCategory.PRESENCE);

        // Branch
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
        Commit C = new Commit("0-C", 3);
        Commit D = new Commit("1-D", 2);
        Commit E = new Commit("2-E", 4);
        Commit F = new Commit("1-F", 5);
        Commit G = new Commit("0-G", 6);
        Commit H = new Commit("1-H", 7);
        Commit I = new Commit("0-I", 8);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(D, firstSmell, secondSmell);
        mockCommitSmells(E, secondSmell);
        mockCommitSmells(F, firstSmell, secondSmell);
        mockCommitSmells(G, secondSmell);
        mockCommitSmells(H, firstSmell, secondSmell, thirdSmell);
        mockCommitSmells(I, secondSmell, thirdSmell);

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

        mockMergeCommit(I, H);
        mockMergeCommit(G, E);
        mockBranchParentCommitSmells(1, firstSmell);
        mockBranchParentCommitSmells(2, firstSmell, secondSmell);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(persistence, times(17)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.REFACTOR);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);

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
        Commit C = new Commit("3-C", 2);
        Commit D = new Commit("2-D", 3);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);

        // This define the input order
        mockCommitSmells(A, firstSmell, secondSmell);
        mockCommitSmells(B, firstSmell);
        mockCommitSmells(C, firstSmell, thirdSmell);
        mockCommitSmells(D, firstSmell, thirdSmell);
        mockCommitSmells(E, firstSmell, thirdSmell);
        mockCommitSmells(F, firstSmell, thirdSmell);

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

        mockMergeCommit(F, E);
        mockMergeCommit(E, D);
        mockMergeCommit(D, C);
        mockBranchParentCommitSmells(1, firstSmell, secondSmell);
        mockBranchParentCommitSmells(2, firstSmell);
        mockBranchParentCommitSmells(3, firstSmell);

        getAnalysis().query();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);

        verify(persistence, times(15)).addStatements(any());
        // Initial branch
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

    /**
     * <pre><code>
     * .    A (1,     )
     * |\
     * | .  B (1, 2,  )
     * | |
     * . |  C (1,     )
     * |/|
     * . |  D (1, 2,  ) [merge]
     * | |
     * | .  E (1, 2   )
     * |/
     * .    F (1, 2   ) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMultipleMergeOnSameBranch() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("1-B", 1);
        Commit C = new Commit("0-C", 2);
        Commit D = new Commit("0-D", 3);
        Commit E = new Commit("1-E", 4);
        Commit F = new Commit("0-F", 5);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, firstSmell, secondSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(D, firstSmell, secondSmell);
        mockCommitSmells(E, firstSmell, secondSmell);
        mockCommitSmells(F, firstSmell, secondSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 1, 0);
        mockCommitBranch(C, 0, 1);
        mockCommitBranch(D, 0, 2);
        mockCommitBranch(E, 1, 1);
        mockCommitBranch(F, 0, 3);
        mockLastBranchCommit(0, F);
        mockLastBranchCommit(1, E);

        mockMergeCommit(F, E);
        mockMergeCommit(D, B);
        mockBranchParentCommitSmells(1, firstSmell);

        getAnalysis().query();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(12)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, secondSmell, SmellCategory.PRESENCE);

        // First merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, secondSmell, SmellCategory.PRESENCE);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, secondSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * .    A (1, 2,  )
     * |\
     * | .  B (1, 2,  )
     * . |  C (1,     )
     * |\|
     * | .  D (1,     ) [merge]
     * | |
     * . |  E (1,     )
     * |\|
     * | .  F (1,     ) [merge]
     * | |
     * |/
     * .    G (1,     ) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMergeBackAndForthRefactoring() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("1-B", 1);
        Commit C = new Commit("0-C", 2);
        Commit D = new Commit("1-D", 3);
        Commit E = new Commit("0-E", 4);
        Commit F = new Commit("1-F", 5);
        Commit G = new Commit("0-G", 6);

        // This define the input order
        mockCommitSmells(A, firstSmell, secondSmell);
        mockCommitSmells(B, firstSmell, secondSmell);
        mockCommitSmells(C, firstSmell);
        mockCommitSmells(D, firstSmell);
        mockCommitSmells(E, firstSmell);
        mockCommitSmells(F, firstSmell);
        mockCommitSmells(G, firstSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 1, 0);
        mockCommitBranch(C, 0, 1);
        mockCommitBranch(D, 1, 1);
        mockCommitBranch(E, 0, 2);
        mockCommitBranch(F, 1, 2);
        mockCommitBranch(G, 0, 3);
        mockLastBranchCommit(0, G);
        mockLastBranchCommit(1, F);

        mockMergeCommit(G, F);
        mockMergeCommit(F, E);
        mockMergeCommit(D, C);
        mockBranchParentCommitSmells(1, Arrays.asList(secondSmell, firstSmell));

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(12)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, secondSmell, SmellCategory.PRESENCE);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, secondSmell, SmellCategory.REFACTOR);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, firstSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * .  A (1,     )
     * |
     * .  B (   2,  ) [1 -> 2]
     * |
     * .  C (   2,  )
     * |
     * .  D (1,     ) [2 -> 1]
     * |
     * .  E (1,     )
     * |
     * .  F (1,     )
     * |
     * .  G (1,     )
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testRenamedSmellsBackToOriginal() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 2);
        Commit D = new Commit("0-D", 3);
        Commit E = new Commit("0-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("0-G", 6);

        // This define the input order
        mockCommitSmells(A, firstSmell);
        mockCommitSmells(B, secondSmell);
        mockCommitSmells(C, secondSmell);
        mockCommitSmells(D, firstSmell);
        mockCommitSmells(E, firstSmell);
        mockCommitSmells(F, firstSmell);
        mockCommitSmells(G, firstSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 0, 1);
        mockCommitBranch(C, 0, 2);
        mockCommitBranch(D, 0, 3);
        mockCommitBranch(E, 0, 4);
        mockCommitBranch(F, 0, 5);
        mockCommitBranch(G, 0, 6);
        mockLastBranchCommit(0, G);

        Smell expectedSecondSmell = mockSmellRenamed(B, secondSmell, firstSmell);
        mockSmellRenamed(C, secondSmell, firstSmell);
        Smell expectedRenamedFirstSmell = mockSmellRenamed(D, firstSmell, secondSmell);
        mockSmellRenamed(E, firstSmell, secondSmell);
        mockSmellRenamed(F, firstSmell, secondSmell);
        mockSmellRenamed(G, firstSmell, secondSmell);
        mockSmellId(expectedSecondSmell);
        expectedRenamedFirstSmell.parent = expectedSecondSmell;
        mockSmellId(expectedRenamedFirstSmell);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, expectedSecondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, expectedRenamedFirstSmell);

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, expectedSecondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, expectedSecondSmell, SmellCategory.PRESENCE);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * .    A (1, 2,     )
     * |\
     * | .  B (1,    3,  )
     * |/
     * .    C (      3, 4) [merge]
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testMergeCommitRefactorAndIntroduces() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("1-B", 1);
        Commit C = new Commit("0-C", 2);
        Smell fourthSmell = new Smell(smellType, "fourthSmellInstance", "fourthSmellFile");
        mockSmellId(fourthSmell);

        // This define the input order
        mockCommitSmells(A, firstSmell, secondSmell);
        mockCommitSmells(B, firstSmell, thirdSmell);
        mockCommitSmells(C, thirdSmell, fourthSmell);

        mockCommitBranch(A, 0, 0);
        mockCommitBranch(B, 1, 0);
        mockCommitBranch(C, 0, 1);
        mockLastBranchCommit(0, C);
        mockLastBranchCommit(1, B);

        mockMergeCommit(C, B);
        mockBranchParentCommitSmells(1, firstSmell, secondSmell);

        getAnalysis().query();


        verify(persistence, times(4)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, thirdSmell);
        verify(smellQueries).smellInsertionStatement(projectId, fourthSmell);


        verify(persistence, times(12)).addStatements(any());
        // Initial branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, secondSmell, SmellCategory.INTRODUCTION);

        // First Branch
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, secondSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, thirdSmell, SmellCategory.INTRODUCTION);

        // Merge
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, firstSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, thirdSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, fourthSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, fourthSmell, SmellCategory.INTRODUCTION);
    }

}