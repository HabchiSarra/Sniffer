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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BranchAwareSmellTypeAnalysisTest extends SmellTypeAnalysis {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(persistence.branchOrdinalQueryStatement(anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> branchOrdinalStatement(
                        invocation.getArgument(0),
                        ((Commit) invocation.getArgument(1)).sha));
        when(persistence.branchCommitOrdinalQuery(anyInt(), anyInt(), any(Commit.class))).then((Answer<String>)
                invocation -> branchCommitOrdinalStatement(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        ((Commit) invocation.getArgument(2)).sha));
    }

    private static String branchOrdinalStatement(int projectid, String sha) {
        return "BranchOrdinalStatement-" + projectid + "-" + sha;
    }

    private static String branchCommitOrdinalStatement(int projectid, int branchId, String sha) {
        return "BranchCommitOrdinalStatement-" + projectid + "-" + branchId + "-" + sha;
    }

    private BranchAwareSmellTypeAnalysis getAnalysis() {
        return new BranchAwareSmellTypeAnalysis(projectId, persistence, smellList.iterator(), smellType, duplicationChecker);
    }

    private void mockCommitBranch(Commit commit, int branch, int commitOrdinal) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Collections.singletonMap("ordinal", branch));

        doReturn(result).when(persistence).query(branchOrdinalStatement(projectId, commit.sha));
        result.clear();
        result.add(Collections.singletonMap("ordinal", commitOrdinal));
        doReturn(result).when(persistence).query(branchCommitOrdinalStatement(projectId, commitOrdinal, commit.sha));
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    @Test
    public void handlePresenceAndIntroductionOnSingleCommitNotLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        String lastCommitSha = "another";
        mockCommitBranch(firstCommit, 0, 0);

        mockEndCommit(lastCommitSha);
        getAnalysis().query();

        verify(persistence, times(4)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(persistence).smellCategoryInsertionStatement(projectId, lastCommitSha, firstSmell, SmellCategory.REFACTOR);
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(persistence).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.REFACTOR);
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(persistence).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.REFACTOR);
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
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

        verify(persistence, times(11)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and 3rd commits will insert the secondSmell since 3rd has no idea it existed.
        verify(persistence, times(2)).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // The missing Ordinal 1 will be replaced by an empty commit
        verify(persistence).smellCategoryInsertionStatement(projectId, gapCommitSha, firstSmell, SmellCategory.REFACTOR);
        verify(persistence).smellCategoryInsertionStatement(projectId, gapCommitSha, secondSmell, SmellCategory.REFACTOR);

        // 3rd commit is counted as introducing the commit back.
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    @Test
    public void handleMultipleMissingCommits() throws QueryException {
        Commit anotherCommit = new Commit("thirdSha", 24);
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(anotherCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(anotherCommit, 0, 1);

        String gapCommitSha = "someSha";
        mockGapCommit(gapCommitSha);
        mockEndCommit(anotherCommit.sha);
        getAnalysis().query();

        verify(persistence, times(11)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and Nth commits will insert the secondSmell since 3rd has no idea it existed.
        verify(persistence, times(2)).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // The missing Ordinal 1 will be replaced by an empty commit
        verify(persistence).smellCategoryInsertionStatement(projectId, gapCommitSha, firstSmell, SmellCategory.REFACTOR);
        verify(persistence).smellCategoryInsertionStatement(projectId, gapCommitSha, secondSmell, SmellCategory.REFACTOR);

        // Nth commit is counted as introducing the commit back.
        verify(persistence).smellCategoryInsertionStatement(projectId, anotherCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, anotherCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    @Test
    public void handleMissingGapCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);
        mockCommitBranch(firstCommit, 0, 0);
        mockCommitBranch(thirdCommit, 0, 1);

        mockNoGapCommit(); // We pray for this not to happen, but we have to be prepared for the worst.
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(8)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and Nth commits will insert the secondSmell since 3rd has no idea it existed.
        verify(persistence).smellInsertionStatement(projectId, secondSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // Since we couldn't find the missing commit, we put the smells in LostRefactor and no presence.
        verify(persistence).lostSmellCategoryInsertionStatement(projectId, firstSmell, SmellCategory.REFACTOR, secondCommit.ordinal, thirdCommit.ordinal);
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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(persistence, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);

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
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(persistence, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        verify(persistence).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        // Check that the renamed commit has a set parentInstance
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(secondSmell, renamed);
        assertEquals(firstSmell.instance, renamed.parentInstance);

        // We won't introduce the same rename multiple times, as before.
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);

    }

    @Test
    public void handleFirstCommitIsNotTheFirstOrdinal() throws QueryException {
        addSmell(thirdCommit, firstSmell);
        mockCommitBranch(thirdCommit, 0, 0);

        mockGapCommit(firstCommit.sha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(3)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }
}