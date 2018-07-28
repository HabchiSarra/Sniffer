package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.QueryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrdinalSmellTypeAnalysisTest {
    public static final String END_COMMIT_STATEMENT = "EndCommitStatement";
    public static final String GAP_COMMIT_STATEMENT = "GapCommitStatement";

    private final int projectId = 1;
    private final String smellType = "TEST";

    private final Smell firstSmell = new Smell(smellType, "instance", "/file");
    private final Smell secondSmell = new Smell(smellType, "anotherInstance", "/file");
    private final Commit firstCommit = new Commit("sha1", 0);
    private final Commit secondCommit = new Commit("sha1-2", 1);
    private final Commit thirdCommit = new Commit("sha1-3", 2);

    private List<Map<String, Object>> smellList;
    private Persistence persistence;
    private SmellDuplicationChecker duplicationChecker;

    @Before
    public void setUp() throws Exception {
        smellList = new ArrayList<>();
        persistence = Mockito.mock(Persistence.class);
        duplicationChecker = Mockito.mock(SmellDuplicationChecker.class);

        doReturn(END_COMMIT_STATEMENT).when(persistence).lastProjectCommitSha1QueryStatement(projectId);
        doReturn(GAP_COMMIT_STATEMENT).when(persistence).commitSha1QueryStatement(eq(projectId), anyInt());
    }

    private void addSmell(Commit commit, Smell smell) {
        Map<String, Object> smellMap = new HashMap<>();
        smellMap.put("key", commit.sha);
        smellMap.put("commit_number", commit.ordinal);
        smellMap.put("instance", smell.instance);
        smellMap.put("file_path", smell.file);
        smellList.add(smellMap);
    }

    private void mockEndCommit(String sha1) {
        ArrayList<Object> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("sha1", sha1);
        result.add(map);
        doReturn(result).when(persistence).query(END_COMMIT_STATEMENT);
    }

    private void mockNoEndCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(END_COMMIT_STATEMENT);
    }

    private void mockGapCommit(String sha1) {
        ArrayList<Object> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("sha1", sha1);
        result.add(map);
        doReturn(result).when(persistence).query(GAP_COMMIT_STATEMENT);
    }

    private void mockNoGapCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(GAP_COMMIT_STATEMENT);
    }

    @Test(expected = QueryException.class)
    public void testNoEndCommitFoundWillThrow() throws QueryException {
        addSmell(firstCommit, firstSmell);

        mockNoEndCommit();
        getAnalysis().query();
    }

    @Test
    public void handlePresenceAndIntroductionOnSingleCommitAlsoLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);

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

    private OrdinalSmellTypeAnalysis getAnalysis() {
        return new OrdinalSmellTypeAnalysis(1, persistence, smellList.iterator(), smellType, duplicationChecker);
    }

    @Test
    public void handleRefactorOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(secondCommit, firstSmell);

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

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(secondCommit.sha);
        getAnalysis().query();

        verify(persistence, times(5)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamedFrom filled in.
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

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(6)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamedFrom filled in.
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

        mockGapCommit(firstCommit.sha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(3)).addStatements(any());
        verify(persistence).smellInsertionStatement(projectId, firstSmell);
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(persistence).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }
}