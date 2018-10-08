package fr.inria.sniffer.tracker.analysis.query.commit;

import fr.inria.sniffer.tracker.analysis.persistence.queries.DeveloperQueries;
import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.CommitDetails;
import fr.inria.sniffer.tracker.analysis.model.GitDiff;
import fr.inria.sniffer.tracker.analysis.model.GitRename;
import fr.inria.sniffer.tracker.analysis.model.Repository;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class CommitsAnalysisTest {
    private final int projectId = 1;
    private final CommitDetails dummyDetails = new CommitDetails(GitDiff.EMPTY, Collections.emptyList());

    private Persistence persistence;
    private CommitQueries commitQueries;
    private DeveloperQueries developerQueries;
    private Repository repository;
    private CommitDetailsChecker detailsChecker;
    private List<Map<String, Object>> paprikaCommitsList;

    private final Commit A = new Commit("a", 0, new DateTime(1),
            "message1", "author@email.com", Collections.emptyList());
    private final Commit B = new Commit("b", 1, new DateTime(2),
            "message2", "author@email.com", Collections.emptyList());
    private final Commit C = new Commit("c", 2, new DateTime(3),
            "message3", "another_author@email.com", Collections.emptyList());
    private final Commit D = new Commit("d", 3, new DateTime(4),
            "message4", "another_author@email.com", Collections.emptyList());

    @Before
    public void setUp() throws Exception {
        repository = Mockito.mock(Repository.class);
        persistence = Mockito.mock(Persistence.class);
        commitQueries = Mockito.mock(CommitQueries.class);
        developerQueries = Mockito.mock(DeveloperQueries.class);
        detailsChecker = Mockito.mock(CommitDetailsChecker.class);

        paprikaCommitsList = new ArrayList<>();
        doReturn("CommitInsertion").when(commitQueries).commitInsertionStatement(
                eq(projectId), any(Commit.class), any(GitDiff.class));
        doReturn("DeveloperInsertion").when(developerQueries).developerInsertStatement(
                anyString());
        doReturn("DeveloperProjectInsertion").when(developerQueries).projectDeveloperInsertStatement(
                eq(projectId), anyString());
        doReturn("FileRenameInsertion").when(commitQueries).fileRenameInsertionStatement(
                eq(projectId), anyString(), any(GitRename.class));
    }

    private CommitsAnalysis getCommitsAnalysis() {
        return new CommitsAnalysis(projectId, persistence, repository, paprikaCommitsList.iterator(), detailsChecker, developerQueries, commitQueries);
    }

    private void addCommitInPaprika(Commit commit) {
        Map<String, Object> smellMap = new HashMap<>();
        smellMap.put("key", commit.sha);
        smellMap.put("commit_number", commit.ordinal);
        paprikaCommitsList.add(smellMap);
    }

    private void addCommitInRepository(Commit commit) throws IOException {
        addCommitInRepository(commit, dummyDetails);
    }

    private void addCommitInRepository(Commit commit, CommitDetails details) throws IOException {
        // Prepare repository to return this commit.
        doReturn(commit).when(repository).getCommitWithDetails(eq(commit.sha));
        doReturn(commit).when(repository).getCommitWithParents(commit.sha);
        doReturn(details).when(detailsChecker).fetch(commit.sha);
    }

    private void prepareGitLog(Commit... commits) throws IOException {
        List<String> log = new ArrayList<>();
        for (Commit commit : commits) {
            log.add(commit.sha);
        }
        when(repository.getLog()).thenReturn(log);
    }

    /**
     * <pre><code>
     *     | * A [merged]
     *     * | B [parent]
     *     |/
     *     *   C [merge]
     * </code></pre>
     *
     * @throws IOException
     * @throws QueryException
     */
    @Test
    public void testMergeCommitInserted() throws IOException, QueryException {
        Commit merged = new Commit("a", 51,
                DateTime.now(), "message", "first@email.com", new ArrayList<>());
        Commit parent = new Commit("b", 53,
                DateTime.now(), "message", "second@email.com", new ArrayList<>());
        ArrayList<Commit> parents = new ArrayList<>();
        parents.add(parent);
        parents.add(merged);
        Commit merge = new Commit("c", 54,
                DateTime.now(), "message", "author@email.com", parents);

        addCommitInPaprika(merged);
        addCommitInRepository(merged);
        addCommitInPaprika(parent);
        addCommitInRepository(parent);
        addCommitInPaprika(merge);
        addCommitInRepository(merge);
        prepareGitLog(merged, parent, merge);

        getCommitsAnalysis().query();

        verify(commitQueries, times(3)).commitInsertionStatement(anyInt(), any(Commit.class), any(GitDiff.class));
        verify(commitQueries).commitInsertionStatement(projectId, merged, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, parent, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, merge, dummyDetails.diff);

        // Author insertion is brainlessly done at each encounter
        verify(developerQueries, times(3)).developerInsertStatement(anyString());
        verify(developerQueries, times(1)).developerInsertStatement(merged.authorEmail);
        verify(developerQueries, times(1)).developerInsertStatement(parent.authorEmail);
        verify(developerQueries, times(1)).developerInsertStatement(merge.authorEmail);
        verify(developerQueries, times(3)).projectDeveloperInsertStatement(eq(projectId), anyString());
        verify(developerQueries, times(1)).projectDeveloperInsertStatement(projectId, merged.authorEmail);
        verify(developerQueries, times(1)).projectDeveloperInsertStatement(projectId, parent.authorEmail);
        verify(developerQueries, times(1)).projectDeveloperInsertStatement(projectId, merge.authorEmail);

        // No GitRename handled
        verify(commitQueries, times(0)).fileRenameInsertionStatement(eq(projectId), anyString(), any(GitRename.class));
    }

    /**
     * <pre><code>
     *     * A
     *     |
     *     * B
     *     |
     *     * C
     *     |
     *     * D
     * </code></pre>
     *
     * @throws IOException
     * @throws QueryException
     */
    @Test
    public void testInsertCommit() throws Exception {
        addCommitInPaprika(A);
        addCommitInRepository(A);
        addCommitInPaprika(B);
        addCommitInRepository(B);
        addCommitInPaprika(C);
        addCommitInRepository(C);
        addCommitInPaprika(D);
        addCommitInRepository(D);
        prepareGitLog(A, B, C, D);

        getCommitsAnalysis().query();

        verify(commitQueries, times(4)).commitInsertionStatement(anyInt(), any(Commit.class), any(GitDiff.class));
        verify(commitQueries).commitInsertionStatement(projectId, A, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, B, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, C, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, D, dummyDetails.diff);

        // Author insertion is brainlessly done at each encounter
        verify(developerQueries, times(4)).developerInsertStatement(anyString());
        verify(developerQueries, times(2)).developerInsertStatement(A.authorEmail);
        verify(developerQueries, times(2)).developerInsertStatement(C.authorEmail);
        verify(developerQueries, times(4)).projectDeveloperInsertStatement(eq(projectId), anyString());
        verify(developerQueries, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);
        verify(developerQueries, times(2)).projectDeveloperInsertStatement(projectId, C.authorEmail);

        // No GitRename handled
        verify(commitQueries, times(0)).fileRenameInsertionStatement(eq(projectId), anyString(), any(GitRename.class));
    }

    /**
     * <pre><code>
     *     * A
     *     |
     *     * B
     *     |
     *     x C [Not in Paprika]
     *     |
     *     * D
     * </code></pre>
     *
     * @throws IOException
     * @throws QueryException
     */
    @Test
    public void testCommitNotInPaprika() throws Exception {
        addCommitInPaprika(A);
        addCommitInRepository(A);
        addCommitInPaprika(B);
        addCommitInRepository(B);
        // Simulate a failed commit in Paprika
        addCommitInRepository(C);
        addCommitInPaprika(D);
        addCommitInRepository(D);
        prepareGitLog(A, B, C, D);

        getCommitsAnalysis().query();

        verify(commitQueries, times(4)).commitInsertionStatement(anyInt(), any(Commit.class), any(GitDiff.class));
        verify(commitQueries).commitInsertionStatement(projectId, A, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, B, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, C, dummyDetails.diff);
        verify(commitQueries).commitInsertionStatement(projectId, D, dummyDetails.diff);

        // Author insertion is brainlessly done at each encounter
        verify(developerQueries, times(4)).developerInsertStatement(anyString());
        verify(developerQueries, times(2)).developerInsertStatement(A.authorEmail);
        verify(developerQueries, times(2)).developerInsertStatement(C.authorEmail);
        verify(developerQueries, times(4)).projectDeveloperInsertStatement(eq(projectId), anyString());
        verify(developerQueries, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);
        verify(developerQueries, times(2)).projectDeveloperInsertStatement(projectId, C.authorEmail);

        // No GitRename handled
        verify(commitQueries, times(0)).fileRenameInsertionStatement(eq(projectId), anyString(), any(GitRename.class));
    }

    /**
     * <pre><code>
     *     * A
     *     |
     *     * B
     * </code></pre>
     *
     * @throws Exception
     */
    @Test
    public void testCommitWitDetails() throws Exception {
        GitRename notJavaRename = new GitRename("old", "new", 97);
        GitRename actualRename = new GitRename("another.java", "anotherNew.java", 100);
        GitRename renameB = new GitRename("oldB.java", "newB.java", 50);
        CommitDetails details = new CommitDetails(
                new GitDiff(1, 2, 4), Arrays.asList(notJavaRename, actualRename));
        CommitDetails otherDetails = new CommitDetails(
                new GitDiff(0, 135, 20), Collections.singletonList(renameB));

        addCommitInPaprika(A);
        addCommitInRepository(A, details);
        addCommitInPaprika(B);
        addCommitInRepository(B, otherDetails);
        prepareGitLog(A, B);

        getCommitsAnalysis().query();

        verify(commitQueries).commitInsertionStatement(projectId, A, details.diff);
        verify(commitQueries, times(0)).fileRenameInsertionStatement(projectId, A.sha, notJavaRename);
        verify(commitQueries).fileRenameInsertionStatement(projectId, A.sha, actualRename);

        verify(commitQueries).commitInsertionStatement(projectId, B, otherDetails.diff);
        verify(commitQueries).fileRenameInsertionStatement(projectId, B.sha, renameB);

        // A and B share the same author.
        verify(developerQueries, times(2)).developerInsertStatement(A.authorEmail);
        verify(developerQueries, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);

    }
}