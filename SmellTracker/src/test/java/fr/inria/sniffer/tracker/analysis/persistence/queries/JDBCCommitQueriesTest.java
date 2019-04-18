/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.inria.sniffer.tracker.analysis.persistence.queries;

import fr.inria.sniffer.tracker.analysis.persistence.PostgresTestCase;
import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.GitDiff;
import fr.inria.sniffer.tracker.analysis.model.GitRename;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JDBCCommitQueriesTest extends PostgresTestCase {
    private DeveloperQueries developerQueries;
    private ProjectQueries projectQueries;
    private CommitQueries queries;

    private int projectId;
    private String mainDev;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        projectQueries = new JDBCProjectQueries();
        developerQueries = new JDBCDeveloperQueries();
        queries = new JDBCCommitQueries(developerQueries);

        this.projectId = createProject("whatever");
        this.mainDev = "author@email.com";
        createDev(mainDev);

    }

    private int createProject(String projectName) {
        executeSuccess(projectQueries.projectInsertStatement(projectName, "url"));
        String idQuery = projectQueries.idFromNameQuery(projectName);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }

    private void createDev(String devName) {
        executeSuccess(developerQueries.developerInsertStatement(devName));
    }

    private Commit generateCommit(String sha, int ordinal) {
        return generateCommit(sha, ordinal, "message");
    }

    private Commit generateCommit(String sha, int ordinal, String message) {
        return new Commit(sha, ordinal, new DateTime(), message, this.mainDev, new ArrayList<>());
    }

    private long getCommitCount() {
        return countElements("commit_entry");
    }

    private long getRenameCount() {
        return countElements("file_rename");
    }

    @Test
    public void testCommitInsertionStatement() {
        long count = 0;

        Commit commit = generateCommit("sha", 1);

        // We can insert any commit
        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        assertEquals(++count, getCommitCount());

        // We can any other same sha1 in another project
        int secondProjectID = createProject("anotherProject");
        executeSuccess(queries.commitInsertionStatement(secondProjectID, commit, GitDiff.EMPTY));
        assertEquals(++count, getCommitCount());

        // We can any other sha1 commit
        Commit anotherSha = generateCommit("anotherSha", commit.ordinal + 1);
        executeSuccess(queries.commitInsertionStatement(projectId, anotherSha, GitDiff.EMPTY));
        assertEquals(++count, getCommitCount());

        // We can insert any character sequence in the commit message
        Commit anotherMessage = generateCommit("thirdSha", anotherSha.ordinal + 1,
                "test $$ subString $$ ^$ should ' \" work any way $$ for sure @&é\"'(§è!çà' $");
        executeSuccess(queries.commitInsertionStatement(projectId, anotherMessage, GitDiff.EMPTY));
        assertEquals(++count, getCommitCount());

        // We can't insert the same sha1
        executeNothinhDone(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        assertEquals(count, getCommitCount());

    }

    @Test
    public void testFileRenameInsertionStatement() {
        long count = 0;

        Commit commit = generateCommit("sha", 1);
        Commit anotherSha = generateCommit("anotherSha", 1);
        GitRename rename = new GitRename("old", "new", 100);

        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        executeSuccess(queries.commitInsertionStatement(projectId, anotherSha, GitDiff.EMPTY));

        // We can insert any rename
        executeSuccess(queries.fileRenameInsertionStatement(projectId, commit.sha, rename));
        assertEquals(++count, getRenameCount());

        // We can insert the same rename in another project
        int secondProjectID = createProject("anotherProject");
        executeSuccess(queries.commitInsertionStatement(secondProjectID, commit, GitDiff.EMPTY));
        executeSuccess(queries.fileRenameInsertionStatement(secondProjectID, commit.sha, rename));
        assertEquals(++count, getRenameCount());

        // We can insert the same rename in another commit
        executeSuccess(queries.fileRenameInsertionStatement(projectId, anotherSha.sha, rename));
        assertEquals(++count, getRenameCount());

        // We can insert any other rename
        GitRename anotherRename = new GitRename("new", "old", 50);
        executeSuccess(queries.fileRenameInsertionStatement(projectId, commit.sha, anotherRename));
        assertEquals(++count, getRenameCount());

        // We can't insert the same rename in the same commit of the same project
        executeNothinhDone(queries.fileRenameInsertionStatement(projectId, commit.sha, rename));
        assertEquals(count, getRenameCount());
    }

    @Test
    public void testIdFromShaQuery() {
        List<Map<String, Object>> result;

        // No commit means no result
        result = persistence.query(queries.idFromShaQuery(projectId, "anySha"));
        assertTrue(result.isEmpty());

        Commit commit = generateCommit("sha", 1);
        Commit anotherSha = generateCommit("anotherSha", commit.ordinal + 1);
        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        executeSuccess(queries.commitInsertionStatement(projectId, anotherSha, GitDiff.EMPTY));

        // Wrong sha means no result
        result = persistence.query(queries.idFromShaQuery(projectId, "anySha"));
        assertTrue(result.isEmpty());

        // We can query our commit
        result = persistence.query(queries.idFromShaQuery(projectId, commit.sha));
        assertEquals(1, result.get(0).get("id"));
        result = persistence.query(queries.idFromShaQuery(projectId, anotherSha.sha));
        assertEquals(2, result.get(0).get("id"));
    }

    @Test
    public void testShaFromOrdinalQuery() {
        List<Map<String, Object>> result;

        // No commit means no result
        result = persistence.query(queries.shaFromOrdinalQuery(projectId, 1));
        assertTrue(result.isEmpty());

        Commit commit = generateCommit("sha", 1);
        Commit anotherSha = generateCommit("anotherSha", commit.ordinal + 1);
        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        executeSuccess(queries.commitInsertionStatement(projectId, anotherSha, GitDiff.EMPTY));

        // Wrong ordinal means no result
        result = persistence.query(queries.shaFromOrdinalQuery(projectId, 0));
        assertTrue(result.isEmpty());

        // We can query our commit
        result = persistence.query(queries.shaFromOrdinalQuery(projectId, commit.ordinal));
        assertEquals(commit.sha, result.get(0).get("sha1"));
        result = persistence.query(queries.shaFromOrdinalQuery(projectId, anotherSha.ordinal));
        assertEquals(anotherSha.sha, result.get(0).get("sha1"));
    }

    @Test
    public void testLastProjectCommitShaQuery() {
        List<Map<String, Object>> result;

        // No commit means no result
        result = persistence.query(queries.lastProjectCommitShaQuery(projectId));
        assertTrue(result.isEmpty());

        Commit commit = generateCommit("sha", 1);
        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));

        // Only commit is returned
        result = persistence.query(queries.lastProjectCommitShaQuery(projectId));
        assertEquals(commit.sha, result.get(0).get("sha1"));


        // Most advanced ordinal commit is returned
        Commit fifthOrdinal = generateCommit("anotherSha", 5);
        executeSuccess(queries.commitInsertionStatement(projectId, fifthOrdinal, GitDiff.EMPTY));

        result = persistence.query(queries.lastProjectCommitShaQuery(projectId));
        assertEquals(fifthOrdinal.sha, result.get(0).get("sha1"));

        // Most advanced ordinal commit is kept
        Commit fourthOrdinal = generateCommit("yetAnotherSha", 4);
        executeSuccess(queries.commitInsertionStatement(projectId, fourthOrdinal, GitDiff.EMPTY));

        result = persistence.query(queries.lastProjectCommitShaQuery(projectId));
        assertEquals(fifthOrdinal.sha, result.get(0).get("sha1"));
    }

    @Test
    public void testMergedCommitIdQuery() {
        List<Map<String, Object>> result;

        Commit commit = generateCommit("sha", 1);
        Commit secondCommit = generateCommit("anotherSha", 2);

        // No commit means no result
        result = persistence.query(queries.mergedCommitIdQuery(projectId, commit));
        assertTrue(result.isEmpty());

        executeSuccess(queries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        executeSuccess(queries.commitInsertionStatement(projectId, secondCommit, GitDiff.EMPTY));

        // Not a merge commit means no result in row
        result = persistence.query(queries.mergedCommitIdQuery(projectId, commit));
        assertFalse(result.isEmpty());
        assertNull("returned ID is null", result.get(0).get("id"));
        result = persistence.query(queries.mergedCommitIdQuery(projectId, secondCommit));
        assertFalse(result.isEmpty());
        assertNull("returned ID is null", result.get(0).get("id"));

        Commit mergeCommit = generateCommit("thirdSha", 3);
        mergeCommit.setParents(Arrays.asList(secondCommit, commit));
        executeSuccess(queries.commitInsertionStatement(projectId, mergeCommit, GitDiff.EMPTY));

        // Merge commit returns the ID of the second parent commit.
        result = persistence.query(queries.mergedCommitIdQuery(projectId, mergeCommit));
        assertEquals(1, result.get(0).get("id"));
    }
}
