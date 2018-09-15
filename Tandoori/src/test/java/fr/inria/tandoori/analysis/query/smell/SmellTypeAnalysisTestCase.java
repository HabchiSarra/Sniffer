package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.SmellCategory;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import fr.inria.tandoori.analysis.query.smell.duplication.SmellDuplicationChecker;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class SmellTypeAnalysisTestCase {
    public static final String END_COMMIT_STATEMENT = "EndCommitStatement";
    public static final String GAP_COMMIT_STATEMENT = "GapCommitStatement";

    protected final int projectId = 1;
    protected final String smellType = "TEST";
    protected static int currentSmellId = 1;

    protected Persistence persistence;
    protected CommitQueries commitQueries;
    protected BranchQueries branchQueries;
    protected SmellQueries smellQueries;
    protected SmellDuplicationChecker duplicationChecker;
    protected List<Map<String, Object>> smellList;

    protected Smell firstSmell;
    protected Smell secondSmell;
    protected Smell thirdSmell;

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
        when(smellQueries.smellIdQuery(anyInt(), any(Smell.class))).then((Answer<String>)
                invocation -> smellIdQueryStatement(invocation.getArgument(0),
                        invocation.getArgument(1)));
        doReturn(0).when(persistence).execute(anyString());

        firstSmell = new Smell(smellType, "instance", "/file");
        secondSmell = new Smell(smellType, "secondInstance", "/file");
        thirdSmell = new Smell(smellType, "thirdInstance", "/any/file.java");
        mockSmellId(firstSmell);
        mockSmellId(secondSmell);
        mockSmellId(thirdSmell);
        firstCommit = new Commit("A", 0);
        secondCommit = new Commit("B", 1);
        thirdCommit = new Commit("C", 2);
    }

    protected void mockSmellId(Smell smell) {
        mockSmellId(smell, currentSmellId++);
    }

    private void mockSmellId(Smell smell, int id) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        result.add(map);
        doReturn(result).when(persistence).query(smellIdQueryStatement(projectId, smell));
    }

    private String smellIdQueryStatement(int projectId, Smell smell) {
        StringBuilder query = new StringBuilder("smell_id_query-" + projectId + "-"
                + smell.instance + "-" + smell.file);
        Smell parent = smell.parent;
        while (parent != null){
            query.append("-").append(parent.instance).append("-").append(parent.file);
            parent = parent.parent;
        }
        return query.toString();
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

    protected void mockCommitPosition(String sha1, String statement) {
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
        printInstances();
        System.out.println("--------");

        printInstancesCategories();
        System.out.println("--------");

        printLostInstancesCategories();
        System.out.println("--------");
    }

    private void printInstances() {
        ArgumentCaptor<Smell> instancesCaptor = ArgumentCaptor.forClass(Smell.class);

        verify(smellQueries, atLeastOnce()).smellInsertionStatement(eq(projectId), instancesCaptor.capture());

        List<Smell> instances = instancesCaptor.getAllValues();
        System.out.println("--------");
        for (Smell instance : instances) {
            System.out.println("Call to insertSmellInstance: " + instance);
        }
    }

    private void printInstancesCategories() {
        ArgumentCaptor<Smell> instancesCateCaptor = ArgumentCaptor.forClass(Smell.class);
        ArgumentCaptor<String> shaCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SmellCategory> typeCaptor = ArgumentCaptor.forClass(SmellCategory.class);

        verify(smellQueries, atLeastOnce()).smellCategoryInsertionStatement(eq(projectId),
                shaCaptor.capture(), instancesCateCaptor.capture(), typeCaptor.capture());

        List<Smell> instancesCategory = instancesCateCaptor.getAllValues();
        List<String> shas = shaCaptor.getAllValues();
        List<SmellCategory> categories = typeCaptor.getAllValues();
        for (int i = 0; i < instancesCategory.size(); i++) {
            System.out.println("Call to insertSmellCategory: " + shas.get(i) + " - " + categories.get(i) + " - " +
                    instancesCategory.get(i).instance
            );
        }
    }

    private void printLostInstancesCategories() {
        ArgumentCaptor<Smell> instancesLostCateCaptor = ArgumentCaptor.forClass(Smell.class);
        ArgumentCaptor<SmellCategory> lostTypeCaptor = ArgumentCaptor.forClass(SmellCategory.class);
        ArgumentCaptor<Integer> sinceCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> untilCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(smellQueries, atLeast(0)).lostSmellCategoryInsertionStatement(eq(projectId),
                instancesLostCateCaptor.capture(), lostTypeCaptor.capture(), sinceCaptor.capture(), untilCaptor.capture());

        List<Smell> lostInstancesCategory = instancesLostCateCaptor.getAllValues();
        List<Integer> sinces = sinceCaptor.getAllValues();
        List<Integer> untils = untilCaptor.getAllValues();
        List<SmellCategory> lostCategories = lostTypeCaptor.getAllValues();

        for (int i = 0; i < lostInstancesCategory.size(); i++) {
            System.out.println("Call to insertLostSmellCategory: " + lostCategories.get(i) + " - " + sinces.get(i) + " - " + untils.get(i) + " - " +
                    lostInstancesCategory.get(i).instance
            );
        }
    }

}
