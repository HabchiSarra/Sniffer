package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.CommitDetails;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;
import fr.inria.tandoori.analysis.model.Repository;
import fr.inria.tandoori.analysis.persistence.Persistence;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class CommitsAnalysisTest {
    private final int projectId = 1;
    private final CommitDetails dummyDetails = new CommitDetails(GitDiff.EMPTY, Collections.emptyList());

    private Persistence persistence;
    private Repository repository;
    private CommitDetailsChecker detailsChecker;
    private List<Map<String, Object>> commitsList;

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
        detailsChecker = Mockito.mock(CommitDetailsChecker.class);

        commitsList = new ArrayList<>();
        doReturn("CommitInsertion").when(persistence).commitInsertionStatement(
                eq(projectId), any(Commit.class), any(GitDiff.class), anyInt());
        doReturn("DeveloperInsertion").when(persistence).developerInsertStatement(
                anyString());
        doReturn("DeveloperProjectInsertion").when(persistence).projectDeveloperInsertStatement(
                eq(projectId), anyString());
        doReturn("FileRenameInsertion").when(persistence).fileRenameInsertionStatement(
                eq(projectId), anyString(), any(GitRename.class));
    }

    private CommitsAnalysis getCommitsAnalysis() {
        return new CommitsAnalysis(projectId, persistence, repository, commitsList.iterator(), detailsChecker);
    }

    private void addCommit(Commit commit, CommitDetails details) throws IOException {
        Map<String, Object> smellMap = new HashMap<>();
        smellMap.put("key", commit.sha);
        smellMap.put("commit_number", commit.ordinal);
        commitsList.add(smellMap);

        // Prepare repository to return this commit.
        doReturn(commit).when(repository).getCommitWithDetails(eq(commit.sha));
        doReturn(details).when(detailsChecker).fetch(commit.sha);
    }

    private void addCommit(Commit commit) throws IOException {
        addCommit(commit, dummyDetails);
    }

    @Test
    public void testInsertCommit() throws Exception {
        addCommit(A);
        addCommit(B);
        addCommit(C);
        addCommit(D);

        getCommitsAnalysis().query();

        verify(persistence, times(4)).commitInsertionStatement(anyInt(), any(Commit.class), any(GitDiff.class), anyInt());
        verify(persistence).commitInsertionStatement(projectId, A, dummyDetails.diff, A.ordinal);
        verify(persistence).commitInsertionStatement(projectId, B, dummyDetails.diff, B.ordinal);
        verify(persistence).commitInsertionStatement(projectId, C, dummyDetails.diff, C.ordinal);
        verify(persistence).commitInsertionStatement(projectId, D, dummyDetails.diff, D.ordinal);

        // Author insertion is brainlessly done at each encounter
        verify(persistence, times(4)).developerInsertStatement(anyString());
        verify(persistence, times(2)).developerInsertStatement(A.authorEmail);
        verify(persistence, times(2)).developerInsertStatement(C.authorEmail);
        verify(persistence, times(4)).projectDeveloperInsertStatement(eq(projectId), anyString());
        verify(persistence, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);
        verify(persistence, times(2)).projectDeveloperInsertStatement(projectId, C.authorEmail);

        // No GitRename handled
        verify(persistence, times(0)).fileRenameInsertionStatement(eq(projectId), anyString(), any(GitRename.class));
    }

    @Test
    public void testCommitNotInPaprika() throws Exception {
        addCommit(A);
        addCommit(B);
        // Even if the repository knows the missing commit, it will not be added.
        doReturn(C).when(repository).getCommitWithDetails(eq(C.sha));
        addCommit(D);

        getCommitsAnalysis().query();

        verify(persistence, times(3)).commitInsertionStatement(anyInt(), any(Commit.class), any(GitDiff.class), anyInt());
        verify(persistence).commitInsertionStatement(projectId, A, dummyDetails.diff, A.ordinal);
        verify(persistence).commitInsertionStatement(projectId, B, dummyDetails.diff, B.ordinal);
        verify(persistence).commitInsertionStatement(projectId, D, dummyDetails.diff, D.ordinal);

        // Author insertion is brainlessly done at each encounter
        verify(persistence, times(3)).developerInsertStatement(anyString());
        verify(persistence, times(2)).developerInsertStatement(A.authorEmail);
        verify(persistence, times(1)).developerInsertStatement(C.authorEmail);
        verify(persistence, times(3)).projectDeveloperInsertStatement(eq(projectId), anyString());
        verify(persistence, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);
        verify(persistence, times(1)).projectDeveloperInsertStatement(projectId, C.authorEmail);

        // No GitRename handled
        verify(persistence, times(0)).fileRenameInsertionStatement(eq(projectId), anyString(), any(GitRename.class));
    }

    @Test
    public void testCommitWitDetails() throws Exception {
        GitRename notJavaRename = new GitRename("old", "new", 97);
        GitRename actualRename = new GitRename("another.java", "anotherNew.java", 100);
        GitRename renameB = new GitRename("oldB.java", "newB.java", 50);
        CommitDetails details = new CommitDetails(
                new GitDiff(1, 2, 4), Arrays.asList(notJavaRename, actualRename));
        addCommit(A, details);
        CommitDetails otherDetails = new CommitDetails(
                new GitDiff(0, 135, 20), Collections.singletonList(renameB));
        addCommit(B, otherDetails);

        getCommitsAnalysis().query();

        verify(persistence).commitInsertionStatement(projectId, A, details.diff, A.ordinal);
        verify(persistence, times(0)).fileRenameInsertionStatement(projectId, A.sha, notJavaRename);
        verify(persistence).fileRenameInsertionStatement(projectId, A.sha, actualRename);

        verify(persistence).commitInsertionStatement(projectId, B, otherDetails.diff, B.ordinal);
        verify(persistence).fileRenameInsertionStatement(projectId, B.sha, renameB);

        // A and B share the same author.
        verify(persistence, times(2)).developerInsertStatement(A.authorEmail);
        verify(persistence, times(2)).projectDeveloperInsertStatement(projectId, A.authorEmail);

    }
}