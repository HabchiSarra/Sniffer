package fr.inria.tandoori.analysis.query.branch;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.BranchQueries;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.query.QueryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BranchQueryTest {

    public static final String SELECT_HEAD = "select head";
    private final int projectId = 1;
    private Persistence persistence;
    private BranchQueries branchQueries;
    private CommitQueries commitQueries;
    private Repository repository;

    @Before
    public void setUp() throws Exception {
        repository = Mockito.mock(Repository.class);
        persistence = Mockito.mock(Persistence.class);
        branchQueries = Mockito.mock(BranchQueries.class);
        commitQueries = Mockito.mock(CommitQueries.class);
        doReturn("BranchInsertion").when(branchQueries).branchInsertionStatement(eq(projectId), anyInt(), any(Commit.class), any(Commit.class));
        doReturn("BranchInsertion").when(branchQueries).branchCommitInsertionQuery(eq(projectId), anyInt(), anyString(), anyInt());
    }

    private BranchQuery getQuery() {
        return new BranchQuery(projectId, repository, persistence, commitQueries, branchQueries);
    }

    private void initializeHead(Commit commit) throws IOException {
        HashMap<Object, Object> map = new HashMap<>();
        map.put("sha1", commit.sha);
        doReturn(SELECT_HEAD).when(commitQueries).lastProjectCommitShaQuery(projectId);
        doReturn(Collections.singletonList(map)).when(persistence).query(SELECT_HEAD);
    }

    /**
     * Register all the given commits to be returned by getCommit.
     *
     * @param commits Input order does not matter as they are referenced by their sha.
     * @throws IOException
     */
    private void initializeMocks(Commit... commits) throws IOException {
        for (Commit commit : commits) {
            doReturn(commit).when(repository).getCommitWithParents(commit.sha);
            doReturn(commit.sha).when(commitQueries).idFromShaQuery(projectId, commit.sha);
            HashMap<Object, Object> map = new HashMap<>();
            map.put("id", 1);
            doReturn(Collections.singletonList(map)).when(persistence).query(commit.sha);
        }
    }

    /**
     * Testing this kind of branching form (no branch):
     * <pre><code>
     * . A
     * |
     * . B
     * |
     * . C
     * </pre></code>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testNoMerge() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));

        initializeHead(C);
        initializeMocks(A, B, C);

        getQuery().query();

        verify(persistence, times(4)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
    }

    /**
     * Testing this kind of branching form (1 branch):
     * <pre><code>
     * .   A
     * |\
     * . | B
     * | . D
     * . | C
     * | . E
     * |/
     * .   F (merge)
     * </pre></code>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testSingleMergeCommit() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(D));
        Commit F = new Commit("f", 6, Arrays.asList(C, E));

        initializeHead(F);
        initializeMocks(A, B, C, D, E, F);

        getQuery().query();

        verify(persistence, times(8)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 3);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, F);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, E.sha, 1);
    }

    /**
     * Testing this kind of branching form (2 consecutive branches):
     * <pre><code>
     * .   A
     * |\
     * . | B
     * | . D
     * . | C
     * | . E
     * |/
     * .   F (merge)
     * |\
     * | . G
     * . | H
     * |/
     * .   I (merge)
     * </pre></code>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testSuccessiveBranches() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(D));
        Commit F = new Commit("f", 6, Arrays.asList(C, E));
        Commit G = new Commit("g", 7, Collections.singletonList(F));
        Commit H = new Commit("h", 8, Collections.singletonList(F));
        Commit I = new Commit("i", 9, Arrays.asList(H, G));

        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(12)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 4);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 5);

        verify(branchQueries).branchInsertionStatement(projectId, 1, F, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 0);

        verify(branchQueries).branchInsertionStatement(projectId, 2, A, F);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, E.sha, 1);
    }

    /**
     * Testing this kind of branching form (2 consecutive merges of 1 branch):
     * <pre><code>
     * .   A
     * |\
     * . | B
     * | . D
     * . | C
     * | . E
     * |/|
     * . |  F (merge)
     * | |
     * | . G
     * . | H
     * |/
     * .   I (merge)
     * </pre></code>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testContinuingBranches() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(D));
        Commit F = new Commit("f", 6, Arrays.asList(C, E));
        Commit G = new Commit("g", 7, Collections.singletonList(E));
        Commit H = new Commit("h", 8, Collections.singletonList(F));
        Commit I = new Commit("i", 9, Arrays.asList(H, G));

        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(11)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 4);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 5);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, E.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 2);


    }

    /**
     * Testing this kind of branching form (2 overlapping branches):
     * <pre><code>
     * .     A
     * |\
     * . |   B
     * | .   D
     * . |\  C
     * | | . E
     * | . | F
     * | |/
     * | .   G (merge)
     * . |   H
     * |/
     * .     I (merge)
     * </pre></code>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testOverlappingBranches() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(D));
        Commit F = new Commit("f", 6, Collections.singletonList(D));
        Commit G = new Commit("g", 7, Arrays.asList(F, E));
        Commit H = new Commit("h", 8, Collections.singletonList(C));
        Commit I = new Commit("i", 9, Arrays.asList(H, G));

        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(12)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 4);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, F.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 2);

        verify(branchQueries).branchInsertionStatement(projectId, 2, D, G);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, E.sha, 0);
    }

    /**
     * Testing this kind of branching form (2 parallel branches):
     * <pre><code>
     *   .   A
     *  /|\
     * | . | B
     * | | . D
     * | . | C
     * . | | E
     * | | . F
     * | | |
     * \ | . G
     *   . | H (merge)
     *   |/
     *   .   I (merge)
     * </code></pre>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testParallelBranches() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(A));
        Commit F = new Commit("f", 6, Collections.singletonList(D));
        Commit G = new Commit("g", 7, Collections.singletonList(F));
        Commit H = new Commit("h", 8, Arrays.asList(C, E));
        Commit I = new Commit("i", 9, Arrays.asList(H, G));

        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(12)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 4);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, F.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 2);

        verify(branchQueries).branchInsertionStatement(projectId, 2, A, H);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, E.sha, 0);
    }

    /**
     * Testing this kind of branching form (2 crossed branches):
     * <pre><code>
     *   .   A
     *   |\
     *   . | B
     *  /| . D
     * | . | C
     * . |/  E
     * | .   F (merge)
     * . |   G
     *  \|
     *   .   H (merge)
     * </code></pre>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testCrossedBranches() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(B));
        Commit F = new Commit("f", 6, Arrays.asList(C, D));
        Commit G = new Commit("g", 7, Collections.singletonList(E));
        Commit H = new Commit("h", 8, Arrays.asList(F, G));

        initializeHead(H);
        initializeMocks(A, B, C, D, E, F, G, H);

        getQuery().query();

        verify(persistence, times(11)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 4);

        verify(branchQueries).branchInsertionStatement(projectId, 1, B, H);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, E.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 1);

        verify(branchQueries).branchInsertionStatement(projectId, 2, A, F);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, D.sha, 0);
    }

    /**
     * Testing this kind of branching form (merge back and forth):
     * <pre><code>
     * .   A
     * |\
     * . | B
     * | . D
     * . | C
     * |\|
     * | . E (merge)
     * . | F
     * | |
     * | . G
     * . | H
     * |/
     * .   I (merge)
     * </code></pre>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testMergeBackAndForth() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Arrays.asList(D, C));
        Commit F = new Commit("f", 6, Collections.singletonList(C));
        Commit G = new Commit("g", 7, Collections.singletonList(E));
        Commit H = new Commit("h", 8, Collections.singletonList(F));
        Commit I = new Commit("i", 9, Arrays.asList(H, G));
        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(11)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, H.sha, 4);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 5);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, E.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, G.sha, 2);
    }

    /**
     * Testing this kind of branching form (merge back and forth):
     * <pre><code>
     * .     A
     * |\
     * . |   B
     * | .   D
     * . |\  C
     * | | . E
     * | . | F
     * | |/
     * |/|
     * . |   G (merge)
     * | .   H
     * |/
     * .     I (merge)
     * </code></pre>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testMergeNotInDirectParent() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Collections.singletonList(A));
        Commit E = new Commit("e", 4, Collections.singletonList(D));
        Commit F = new Commit("f", 6, Collections.singletonList(D));
        Commit G = new Commit("g", 7, Arrays.asList(C, E));
        Commit H = new Commit("h", 8, Collections.singletonList(F));
        Commit I = new Commit("i", 9, Arrays.asList(G, H));

        initializeHead(I);
        initializeMocks(A, B, C, D, E, F, G, H, I);

        getQuery().query();

        verify(persistence, times(12)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, B.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, C.sha, 2);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, G.sha, 3);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, I.sha, 4);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, I);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, D.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, F.sha, 1);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, H.sha, 2);

        verify(branchQueries).branchInsertionStatement(projectId, 2, D, G);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, E.sha, 0);
    }

    /**
     * Testing this kind of branching form (merge lonely in a branch):
     * <pre><code>
     * .       A
     * |\
     * | .     B
     * | |\
     * | | |\
     * | | | . C
     * | | |/
     * | | .   D (merge)
     * | |/
     * | .     E (merge)
     * |/
     * .       F (merge)
     * </code></pre>
     *
     * @throws QueryException
     * @throws IOException
     */
    @Test
    public void testMergeIsLastBranchCommit() throws QueryException, IOException {
        Commit A = new Commit("a", 1);
        Commit B = new Commit("b", 2, Collections.singletonList(A));
        Commit C = new Commit("c", 3, Collections.singletonList(B));
        Commit D = new Commit("d", 5, Arrays.asList(B, C));
        Commit E = new Commit("e", 4, Arrays.asList(B, D));
        Commit F = new Commit("f", 6, Arrays.asList(A, E));

        initializeHead(F);
        initializeMocks(A, B, C, D, E, F);

        getQuery().query();

        verify(persistence, times(10)).addStatements(any());
        verify(branchQueries).branchInsertionStatement(projectId, 0, null, null);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, A.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 0, F.sha, 1);

        verify(branchQueries).branchInsertionStatement(projectId, 1, A, F);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, B.sha, 0);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 1, E.sha, 1);

        verify(branchQueries).branchInsertionStatement(projectId, 2, B, E);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 2, D.sha, 0);

        verify(branchQueries).branchInsertionStatement(projectId, 3, B, D);
        verify(branchQueries).branchCommitInsertionQuery(projectId, 3, C.sha, 0);
    }

    private void debugBranchCommitInsertions() {
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> ordinalCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(branchQueries, atLeastOnce()).branchCommitInsertionQuery(eq(projectId),
                intCaptor.capture(), stringCaptor.capture(), ordinalCaptor.capture());
        List<Integer> ints = intCaptor.getAllValues();
        List<String> strs = stringCaptor.getAllValues();
        List<Integer> ordinals = ordinalCaptor.getAllValues();

        for (int i = 0; i < ints.size(); i++) {
            System.out.println("Call to branchCommitInsertionQuery: " + ints.get(i) + " - " + strs.get(i) + " - " + ordinals.get(i));
        }
    }
}
