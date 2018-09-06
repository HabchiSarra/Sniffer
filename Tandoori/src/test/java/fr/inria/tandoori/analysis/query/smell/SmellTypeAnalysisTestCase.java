package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public abstract class SmellTypeAnalysisTestCase {
    public static final String END_COMMIT_STATEMENT = "EndCommitStatement";
    public static final String GAP_COMMIT_STATEMENT = "GapCommitStatement";

    protected final int projectId = 1;
    protected final String smellType = "TEST";

    protected Persistence persistence;
    protected CommitQueries commitQueries;
    protected BranchQueries branchQueries;
    protected SmellQueries smellQueries;
    protected SmellDuplicationChecker duplicationChecker;
    protected List<Map<String, Object>> smellList;

    protected Smell firstSmell;
    protected Smell secondSmell;

    protected Commit firstCommit;
    protected Commit secondCommit;
    protected Commit thirdCommit;

    @Before
    public void setUp() throws Exception {
        smellList = new ArrayList<>();

        persistence = Mockito.mock(Persistence.class);
        smellQueries = Mockito.mock(SmellQueries.class);
        commitQueries = Mockito.mock(CommitQueries.class);
        branchQueries = Mockito.mock(BranchQueries.class);
        duplicationChecker = Mockito.mock(SmellDuplicationChecker.class);

        doReturn(END_COMMIT_STATEMENT).when(commitQueries).lastProjectCommitShaQuery(projectId);
        doReturn(GAP_COMMIT_STATEMENT).when(commitQueries).shaFromOrdinalQuery(eq(projectId), anyInt());

        firstSmell = new Smell(smellType, "instance", "/file");
        secondSmell = new Smell(smellType, "secondInstance", "/file");
        firstCommit = new Commit("A", 0);
        secondCommit = new Commit("B", 1);
        thirdCommit = new Commit("C", 2);
    }

    protected void addSmell(Commit commit, Smell smell) {
        Map<String, Object> smellMap = new HashMap<>();
        smellMap.put("key", commit.sha);
        smellMap.put("commit_number", commit.ordinal);
        smellMap.put("instance", smell.instance);
        smellMap.put("file_path", "/" + smell.file);
        smellList.add(smellMap);
    }

    protected void mockEndCommit(String sha1) {
        mockCommitPosition(sha1, END_COMMIT_STATEMENT);
    }

    protected void mockNoEndCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(END_COMMIT_STATEMENT);
    }

    protected void mockGapCommit(String sha1) {
        mockCommitPosition(sha1, GAP_COMMIT_STATEMENT);
    }

    private void mockCommitPosition(String sha1, String statement) {
        ArrayList<Object> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("sha1", sha1);
        result.add(map);
        doReturn(result).when(persistence).query(statement);
    }

    protected void mockNoGapCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(GAP_COMMIT_STATEMENT);
    }

    protected Smell mockSmellRenamed(Commit renamingCommit, Smell renamed, Smell parent) {
        doReturn(parent).when(duplicationChecker).original(renamed, renamingCommit);
        Smell expectedRenamedSmell = new Smell(renamed.type, renamed.instance, renamed.file);
        expectedRenamedSmell.parent = parent;
        return expectedRenamedSmell;
    }

    protected void debugSmellInsertions() {
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
