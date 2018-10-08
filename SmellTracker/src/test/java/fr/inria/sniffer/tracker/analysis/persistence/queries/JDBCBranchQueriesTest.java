package fr.inria.sniffer.tracker.analysis.persistence.queries;

import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.GitDiff;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.PostgresTestCase;
import fr.inria.sniffer.tracker.analysis.persistence.SmellCategory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class JDBCBranchQueriesTest extends PostgresTestCase {
    private DeveloperQueries developerQueries;
    private ProjectQueries projectQueries;
    private BranchQueries queries;

    private int projectId;
    private String mainDev;
    private SmellQueries smellQueries;
    private CommitQueries commitQueries;
    private Commit originCommit;
    private Commit mergedIntoCommit;
    private int mergedIntoCommitId;
    private int originCommitId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        projectQueries = new JDBCProjectQueries();
        developerQueries = new JDBCDeveloperQueries();
        commitQueries = new JDBCCommitQueries(developerQueries);
        smellQueries = new JDBCSmellQueries(commitQueries);
        queries = new JDBCBranchQueries(commitQueries, smellQueries);

        this.projectId = createProject("whatever", projectQueries);
        this.mainDev = "author@email.com";
        createDev(mainDev);

        originCommit = prepareCommit("origin", 0);
        mergedIntoCommit = prepareCommit("merged", 2);
        List<Map<String, Object>> result = persistence.query(commitQueries.idFromShaQuery(projectId, originCommit.sha));
        originCommitId = (int) result.get(0).get("id");
        result = persistence.query(commitQueries.idFromShaQuery(projectId, mergedIntoCommit.sha));
        mergedIntoCommitId = (int) result.get(0).get("id");

    }

    private void createDev(String devName) {
        persistence.execute(developerQueries.developerInsertStatement(devName));
    }

    private Commit generateCommit(String sha, int ordinal) {
        return new Commit(sha, ordinal, new DateTime(), "message", this.mainDev, new ArrayList<>());
    }

    private Commit prepareCommit(String sha, int ordinal) {
        Commit commit = generateCommit(sha, ordinal);
        executeSuccess(commitQueries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        return commit;
    }

    private int insertCommitReturnId(int projectId, Commit commit) {
        executeSuccess(commitQueries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        String idQuery = commitQueries.idFromShaQuery(projectId, commit.sha);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }

    private int insertBranch(int projectId, int ordinal, Commit originCommit, Commit mergedIntoCommit) {
        executeSuccess(queries.branchInsertionStatement(projectId, ordinal, originCommit, mergedIntoCommit));
        List<Map<String, Object>> result = persistence.query(queries.idFromOrdinalQueryStatement(projectId, ordinal));
        return (int) result.get(0).get("id");
    }

    private void addSmells(Commit commit, Smell... smells) {
        for (Smell smell : smells) {
            smell.id = createSmell(projectId, smell, smellQueries);
            executeSuccess(smellQueries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.PRESENCE));
        }
    }

    private long getBranchCount() {
        return countElements("branch");
    }

    private long getBranchCommitCount() {
        return countElements("branch_commit");
    }

    @Test
    public void testBranchInsertionStatement() {
        long count = 0;

        // We can call on not persisted commits.
        // Warning: This will return null for both commit, which should be limited to the master branch
        Commit notPersistedOriginCommit = generateCommit("origin", 0);
        Commit notPersistedMergedCommit = generateCommit("merged", 42);
        executeSuccess(queries.branchInsertionStatement(projectId, 9,
                notPersistedOriginCommit, notPersistedMergedCommit));
        assertEquals(++count, getBranchCount());

        // We can insert any branch
        Commit anotherCommit = prepareCommit("another", 4);
        executeSuccess(queries.branchInsertionStatement(projectId, 0, originCommit, mergedIntoCommit));
        assertEquals(++count, getBranchCount());

        // We can insert another branch ordinal
        executeSuccess(queries.branchInsertionStatement(projectId, 1, originCommit, mergedIntoCommit));
        assertEquals(++count, getBranchCount());
        executeSuccess(queries.branchInsertionStatement(projectId, 2, anotherCommit, mergedIntoCommit));
        assertEquals(++count, getBranchCount());
        executeSuccess(queries.branchInsertionStatement(projectId, 3, mergedIntoCommit, anotherCommit));
        assertEquals(++count, getBranchCount());

        // We can insert the same branch ordinal in another project
        int secondProjectID = createProject("anotherProject", projectQueries);
        executeSuccess(queries.branchInsertionStatement(secondProjectID, 0, originCommit, mergedIntoCommit));
        assertEquals(++count, getBranchCount());

        // We don't insert the same ordinal, whichever the commit
        executeNothinhDone(queries.branchInsertionStatement(projectId, 0, originCommit, mergedIntoCommit));
        assertEquals(count, getBranchCount());
        executeNothinhDone(queries.branchInsertionStatement(projectId, 0, anotherCommit, mergedIntoCommit));
        assertEquals(count, getBranchCount());
        executeNothinhDone(queries.branchInsertionStatement(projectId, 0, mergedIntoCommit, anotherCommit));
        assertEquals(count, getBranchCount());
    }

    @Test
    public void testBranchCommitInsertionQuery() {
        long count = 0;

        executeSuccess(queries.branchInsertionStatement(projectId, 0, originCommit, mergedIntoCommit));

        // We can't call on not existing branch
        executeFailure(queries.branchCommitInsertionQuery(projectId, 1, "sha", 1));
        assertEquals(count, getBranchCommitCount());

        // We can't call on not existing commit
        executeFailure(queries.branchCommitInsertionQuery(projectId, 0, "sha", 1));
        assertEquals(count, getBranchCommitCount());

        // We can insert a commit in a branch
        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 0, branchCommit.sha, 1));
        assertEquals(++count, getBranchCommitCount());
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 0, anotherCommit.sha, 2));
        assertEquals(++count, getBranchCommitCount());

        // We can insert the same commit in another branch
        executeSuccess(queries.branchInsertionStatement(projectId, 1, originCommit, mergedIntoCommit));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 1, branchCommit.sha, 1));
        assertEquals(++count, getBranchCommitCount());
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 1, anotherCommit.sha, 4));
        assertEquals(++count, getBranchCommitCount());

        // We can insert the same branch ordinal in another project
        int secondProjectID = createProject("anotherProject", projectQueries);
        executeSuccess(commitQueries.commitInsertionStatement(secondProjectID, originCommit, GitDiff.EMPTY));
        executeSuccess(commitQueries.commitInsertionStatement(secondProjectID, mergedIntoCommit, GitDiff.EMPTY));
        executeSuccess(commitQueries.commitInsertionStatement(secondProjectID, branchCommit, GitDiff.EMPTY));
        executeSuccess(commitQueries.commitInsertionStatement(secondProjectID, anotherCommit, GitDiff.EMPTY));
        executeSuccess(queries.branchInsertionStatement(secondProjectID, 0, originCommit, mergedIntoCommit));
        executeSuccess(queries.branchCommitInsertionQuery(secondProjectID, 0, branchCommit.sha, 1));
        assertEquals(++count, getBranchCommitCount());
        executeSuccess(queries.branchCommitInsertionQuery(secondProjectID, 0, anotherCommit.sha, 3));
        assertEquals(++count, getBranchCommitCount());

        // We can't insert the same ordinal, whichever the commit
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, branchCommit.sha, 1));
        assertEquals(count, getBranchCommitCount());
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, anotherCommit.sha, 2));
        assertEquals(count, getBranchCommitCount());

        // We can't insert the same commit, whichever the ordinal
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, anotherCommit.sha, 1));
        assertEquals(count, getBranchCommitCount());
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, branchCommit.sha, 2));
        assertEquals(count, getBranchCommitCount());
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, anotherCommit.sha, 45));
        assertEquals(count, getBranchCommitCount());
        executeNothinhDone(queries.branchCommitInsertionQuery(projectId, 0, branchCommit.sha, 27));
        assertEquals(count, getBranchCommitCount());
    }

    @Test
    public void testIdFromOrdinalQueryStatement() {
        List<Map<String, Object>> result;

        // No branch means no result
        result = persistence.query(queries.idFromOrdinalQueryStatement(projectId, 0));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Wrong branch means no result
        result = persistence.query(queries.idFromOrdinalQueryStatement(projectId, 0));
        assertTrue(result.isEmpty());

        // Right branch ordinal returns ID
        result = persistence.query(queries.idFromOrdinalQueryStatement(projectId, 4));
        assertEquals(firstBranchID, result.get(0).get("id"));
        result = persistence.query(queries.idFromOrdinalQueryStatement(projectId, 5));
        assertEquals(secondBranchID, result.get(0).get("id"));
    }

    @Test
    public void testIdFromCommitQueryStatement() {
        List<Map<String, Object>> result;
        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No commit means no result
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, branchCommit));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Parent and merged commits are not in the branch
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, originCommit));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, mergedIntoCommit));
        assertTrue(result.isEmpty());

        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 0));
        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        // Right commit returns branch ID
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, branchCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, anotherCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));

        // TODO: Commit should not appear in 2 distinct branches
        persistence.execute(queries.branchCommitInsertionQuery(projectId, 5, branchCommit.sha, 2));
        result = persistence.query(queries.idFromCommitQueryStatement(projectId, branchCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
    }

    @Test
    public void testParentCommitIdQuery() {
        List<Map<String, Object>> result;
        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No branch means no result
        result = persistence.query(queries.parentCommitIdQuery(projectId, 0));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Right Parent id returned
        result = persistence.query(queries.parentCommitIdQuery(projectId, firstBranchID));
        assertEquals(originCommitId, result.get(0).get("id"));
        result = persistence.query(queries.parentCommitIdQuery(projectId, secondBranchID));
        assertEquals(mergedIntoCommitId, result.get(0).get("id"));

        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 0));
        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.parentCommitIdQuery(projectId, firstBranchID));
        assertEquals(originCommitId, result.get(0).get("id"));
    }

    @Test
    public void testMmergedBranchIdQuery() {
        List<Map<String, Object>> result;
        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No merged branch means no result
        result = persistence.query(queries.mergedBranchIdQuery(projectId, branchCommit));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Right branch id returned for each merge commit
        result = persistence.query(queries.mergedBranchIdQuery(projectId, mergedIntoCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
        result = persistence.query(queries.mergedBranchIdQuery(projectId, originCommit));
        assertEquals(secondBranchID, result.get(0).get("id"));

        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 0));
        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.mergedBranchIdQuery(projectId, mergedIntoCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
    }

    @Test
    public void testShaFromOrdinalQuery() {
        List<Map<String, Object>> result;
        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No merged branch means no result
        result = persistence.query(queries.shaFromOrdinalQuery(projectId, 0, 1));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Right branch id returned for each merge commit
        result = persistence.query(queries.mergedBranchIdQuery(projectId, mergedIntoCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
        result = persistence.query(queries.mergedBranchIdQuery(projectId, originCommit));
        assertEquals(secondBranchID, result.get(0).get("id"));

        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 0));
        persistence.execute(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.mergedBranchIdQuery(projectId, mergedIntoCommit));
        assertEquals(firstBranchID, result.get(0).get("id"));
    }

    @Test
    public void testLastCommitShaQuery() {
        List<Map<String, Object>> result;

        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No merged branch means no result
        result = persistence.query(queries.lastCommitShaQuery(projectId, 1));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Nothing returned if no commit in the branch
        result = persistence.query(queries.lastCommitShaQuery(projectId, firstBranchID));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.lastCommitShaQuery(projectId, secondBranchID));
        assertTrue(result.isEmpty());

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 5));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, branchCommit.sha, 0));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.lastCommitShaQuery(projectId, firstBranchID));
        assertEquals(branchCommit.sha, result.get(0).get("sha1"));
        result = persistence.query(queries.lastCommitShaQuery(projectId, secondBranchID));
        assertEquals(anotherCommit.sha, result.get(0).get("sha1"));
    }

    @Test
    public void testLastCommitIdQuery() {
        List<Map<String, Object>> result;

        Commit branchCommit = generateCommit("sha", 0);
        int branchCommitId = insertCommitReturnId(projectId, branchCommit);
        Commit anotherCommit = generateCommit("another", 4);
        int anotherCommitId = insertCommitReturnId(projectId, anotherCommit);

        // No merged branch means no result
        result = persistence.query(queries.lastCommitIdQuery(projectId, 1));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Nothing returned if no commit in the branch
        result = persistence.query(queries.lastCommitIdQuery(projectId, firstBranchID));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.lastCommitIdQuery(projectId, secondBranchID));
        assertTrue(result.isEmpty());

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 5));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, branchCommit.sha, 0));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.lastCommitIdQuery(projectId, firstBranchID));
        assertEquals(branchCommitId, result.get(0).get("id"));
        result = persistence.query(queries.lastCommitIdQuery(projectId, secondBranchID));
        assertEquals(anotherCommitId, result.get(0).get("id"));
    }

    @Test
    public void testCommitOrdinalQuery() {
        List<Map<String, Object>> result;

        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No merged branch means no result
        result = persistence.query(queries.commitOrdinalQuery(projectId, 1, branchCommit));
        assertTrue(result.isEmpty());

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Nothing returned if no commit in the branch
        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, branchCommit));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, mergedIntoCommit));
        assertTrue(result.isEmpty());

        // Parent and merged commits are not in the branch
        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, originCommit));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, mergedIntoCommit));
        assertTrue(result.isEmpty());

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 5));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, branchCommit.sha, 3));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, anotherCommit.sha, 4));

        // Adding commits to the branch will change our result
        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, branchCommit));
        assertEquals(5, result.get(0).get("ordinal"));

        result = persistence.query(queries.commitOrdinalQuery(projectId, firstBranchID, anotherCommit));
        assertEquals(1, result.get(0).get("ordinal"));

        result = persistence.query(queries.commitOrdinalQuery(projectId, secondBranchID, branchCommit));
        assertEquals(3, result.get(0).get("ordinal"));

        result = persistence.query(queries.commitOrdinalQuery(projectId, secondBranchID, anotherCommit));
        assertEquals(4, result.get(0).get("ordinal"));
    }

    @Test
    public void testParentCommitSmellsQuery() {
        List<Map<String, Object>> result;

        // No branch means no result
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, 0, null));
        assertTrue(result.isEmpty());
        String filtered_type = "MIM";
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, 0, filtered_type));
        assertTrue(result.isEmpty());

        Smell smell1 = new Smell(filtered_type, "instance1", "file1");
        Smell smell2 = new Smell(filtered_type, "instance2", "file2");
        Smell smell3 = new Smell("LIC", "instance3", "file3");
        addSmells(originCommit, smell1, smell2, smell3);
        addSmells(mergedIntoCommit, smell3);

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Right smells id returned depending on filters
        // - First branch, merged into MergedIntoCommit
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, firstBranchID, null));
        checkContainsSmells(result, Arrays.asList(smell1, smell2, smell3));
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, firstBranchID, filtered_type));
        checkContainsSmells(result, Arrays.asList(smell1, smell2));
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, firstBranchID, "LOL"));
        assertTrue(result.isEmpty());

        result = persistence.query(queries.parentCommitSmellsQuery(projectId, secondBranchID, null));
        checkContainsSmells(result, Collections.singletonList(smell3));
        result = persistence.query(queries.parentCommitSmellsQuery(projectId, secondBranchID, smell3.type));
        checkContainsSmells(result, Collections.singletonList(smell3));
    }

    @Test
    public void testLastCommitSmellsQuery() {
        List<Map<String, Object>> result;

        Commit branchCommit = prepareCommit("sha", 0);
        Commit anotherCommit = prepareCommit("another", 4);

        // No merged branch means no result
        result = persistence.query(queries.lastCommitIdQuery(projectId, 1));
        assertTrue(result.isEmpty());

        String filtered_type = "MIM";
        Smell smell1 = new Smell(filtered_type, "instance1", "file1");
        Smell smell2 = new Smell(filtered_type, "instance2", "file2");
        Smell smell3 = new Smell("LIC", "instance3", "file3");
        addSmells(originCommit, smell1, smell2, smell3);
        addSmells(mergedIntoCommit, smell3);

        int firstBranchID = insertBranch(projectId, 4, originCommit, mergedIntoCommit);
        int secondBranchID = insertBranch(projectId, 5, mergedIntoCommit, originCommit);

        // Nothing returned if no commit in the branch
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, mergedIntoCommit, null));
        assertTrue(result.isEmpty());
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, originCommit, null));
        assertTrue(result.isEmpty());

        addSmells(branchCommit, smell1, smell2, smell3);
        addSmells(anotherCommit, smell3);
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, branchCommit.sha, 5));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 4, anotherCommit.sha, 1));

        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, branchCommit.sha, 0));
        executeSuccess(queries.branchCommitInsertionQuery(projectId, 5, anotherCommit.sha, 1));

        // Adding commits to the branch will change our result
        // - First branch, merged into MergedIntoCommit
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, mergedIntoCommit, null));
        checkContainsSmells(result, Arrays.asList(smell1, smell2, smell3));
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, mergedIntoCommit, filtered_type));
        checkContainsSmells(result, Arrays.asList(smell1, smell2));
        // - Second branch, merged into originCommit
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, originCommit, null));
        checkContainsSmells(result, Collections.singletonList(smell3));
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, originCommit, smell3.type));
        checkContainsSmells(result, Collections.singletonList(smell3));
        result = persistence.query(queries.lastCommitSmellsQuery(projectId, originCommit, filtered_type));
        assertTrue(result.isEmpty());
    }

}