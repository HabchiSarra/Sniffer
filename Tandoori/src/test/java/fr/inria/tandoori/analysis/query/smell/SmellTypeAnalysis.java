package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import org.junit.Before;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public abstract class SmellTypeAnalysis {
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

    protected final Smell firstSmell = new Smell(smellType, "instance", "/file");
    protected final Smell secondSmell = new Smell(smellType, "secondInstance", "/file");

    protected final Commit firstCommit = new Commit("A", 0);
    protected final Commit secondCommit = new Commit("B", 1);
    protected final Commit thirdCommit = new Commit("C", 2);

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
        ArrayList<Object> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("sha1", sha1);
        result.add(map);
        doReturn(result).when(persistence).query(END_COMMIT_STATEMENT);
    }

    protected void mockNoEndCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(END_COMMIT_STATEMENT);
    }

    protected void mockGapCommit(String sha1) {
        ArrayList<Object> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("sha1", sha1);
        result.add(map);
        doReturn(result).when(persistence).query(GAP_COMMIT_STATEMENT);
    }

    protected void mockNoGapCommit() {
        doReturn(Collections.emptyList()).when(persistence).query(GAP_COMMIT_STATEMENT);
    }

}
