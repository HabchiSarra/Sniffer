package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.query.QueryException;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.args4j.Argument;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SmellTypeAnalysisTest {
    public static final String SMELL = " Smell ";
    public static final String PRESENCE = " SmellPresence ";
    public static final String INTRODUCTION = " SmellIntroduction ";
    public static final String REFACTOR = " SmellRefactor ";

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
    private ArgumentCaptor<String> statementsCaptor;

    @Before
    public void setUp() throws Exception {
        smellList = new ArrayList<>();
        persistence = Mockito.mock(Persistence.class);
        duplicationChecker = Mockito.mock(SmellDuplicationChecker.class);
        statementsCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(END_COMMIT_STATEMENT).when(persistence).lastProjectCommitSha1QueryStatement(projectId);
        doReturn(GAP_COMMIT_STATEMENT).when(persistence).commitSha1QueryStatement(eq(projectId), anyInt());
    }

    private void addSmell(Smell smell, Commit commit) {
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

    @Test
    public void handlePresenceAndIntroductionOnSingleCommit() throws QueryException {
        addSmell(firstSmell, firstCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockNoEndCommit();
        analysis.query();

        verify(persistence, times(3)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(), SMELL, PRESENCE, INTRODUCTION);
    }

    private void checkActionSuite(List<String> statements, String... actions) {
        assertEquals(actions.length, statements.size());
        for (int i = 0; i < actions.length; i++) {
            assertTrue(statements.get(i).contains(actions[i]));
        }
    }

    @Test
    public void handlePresenceNotTheLastCommitWillAddRefactor() throws QueryException {
        addSmell(firstSmell, firstCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockEndCommit("another");
        analysis.query();

        verify(persistence, times(4)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(), SMELL, PRESENCE, INTRODUCTION, REFACTOR);
    }

    @Test
    public void handleIntroductionAndRefactoring() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, secondCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockNoEndCommit();
        analysis.query();

        verify(persistence, times(7)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, INTRODUCTION,
                SMELL, PRESENCE, INTRODUCTION, REFACTOR
        );
    }

    @Test
    public void handleIntroductionOnly() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(firstSmell, secondCommit);
        addSmell(secondSmell, secondCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockEndCommit(secondCommit.sha);
        analysis.query();

        verify(persistence, times(7)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, INTRODUCTION,
                PRESENCE, // We Won't have the smell insertion here since we check for existence in the previous commit.
                SMELL, PRESENCE, INTRODUCTION
        );
    }

    @Test
    public void handleRefactorOnly() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, firstCommit);
        addSmell(firstSmell, secondCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockEndCommit(secondCommit.sha);
        analysis.query();

        verify(persistence, times(8)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, SMELL, PRESENCE, INTRODUCTION, INTRODUCTION,
                PRESENCE, REFACTOR
        );
    }

    @Test
    public void handleNoChange() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(firstSmell, secondCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockEndCommit(secondCommit.sha);
        analysis.query();

        verify(persistence, times(4)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, INTRODUCTION,
                PRESENCE // We Won't have the smell insertion here since we check for existence in the previous commit.
        );
    }

    @Test
    public void handleCommitGap() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, firstCommit);
        addSmell(secondSmell, thirdCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockGapCommit("someSha");
        mockEndCommit(thirdCommit.sha);
        analysis.query();

        verify(persistence, times(11)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, SMELL, PRESENCE, INTRODUCTION, INTRODUCTION,
                REFACTOR, REFACTOR, // The missing Ordinal 1 will be replaced by an empty commit
                SMELL, PRESENCE, INTRODUCTION
        );
    }

    @Test
    public void handleMultipleMissingCommits() throws QueryException {
        Commit anotherCommit = new Commit("thirdSha", 24);
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, firstCommit);
        addSmell(secondSmell, anotherCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockGapCommit("someSha");
        mockEndCommit(anotherCommit.sha);
        analysis.query();

        verify(persistence, times(11)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, SMELL, PRESENCE, INTRODUCTION, INTRODUCTION,
                REFACTOR, REFACTOR, // The missing Ordinal 1 will be replaced by an empty commit
                SMELL, PRESENCE, INTRODUCTION
        );
    }

    @Test
    public void handleMissingGapCommit() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, firstCommit);
        addSmell(secondSmell, thirdCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockNoGapCommit(); // We pray for this not to happen, but we have to be prepared for the worst.
        mockEndCommit(thirdCommit.sha);
        analysis.query();

        verify(persistence, times(8)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, SMELL, PRESENCE, INTRODUCTION, INTRODUCTION,
                PRESENCE, REFACTOR
        );
    }

    @Test
    public void handleRenamedSmell() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, secondCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(secondCommit.sha);
        analysis.query();

        verify(persistence, times(5)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, INTRODUCTION,
                SMELL, PRESENCE // We introduce the new smell instance definition with renamedFrom filled in.
        );
    }

    @Test
    public void handleRenamedSmellMultipleCommits() throws QueryException {
        addSmell(firstSmell, firstCommit);
        addSmell(secondSmell, secondCommit);
        addSmell(secondSmell, thirdCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        doReturn(firstSmell).when(duplicationChecker).original(secondSmell, secondCommit);
        mockEndCommit(thirdCommit.sha);
        analysis.query();

        verify(persistence, times(6)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                SMELL, PRESENCE, INTRODUCTION,
                SMELL, PRESENCE,
                PRESENCE // We won't introduce the same rename multiple times, as before.
        );
    }

    @Test
    public void handleFirstCommitIsNotTheFirstOrdinal() throws QueryException {
        addSmell(firstSmell, thirdCommit);
        SmellTypeAnalysis analysis = new SmellTypeAnalysis(1, persistence, smellList.iterator(),
                smellType, duplicationChecker);

        mockGapCommit(firstCommit.sha);
        mockEndCommit(thirdCommit.sha);
        analysis.query();

        verify(persistence, times(3)).addStatements(statementsCaptor.capture());
        checkActionSuite(statementsCaptor.getAllValues(),
                // We should have the same sequence as if it was the first commit ordinal.
                SMELL, PRESENCE, INTRODUCTION
        );
    }
}