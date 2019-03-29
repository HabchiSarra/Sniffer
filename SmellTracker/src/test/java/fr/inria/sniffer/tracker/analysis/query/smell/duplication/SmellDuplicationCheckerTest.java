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
package fr.inria.sniffer.tracker.analysis.query.smell.duplication;

import fr.inria.sniffer.detector.neo4j.QueryEngine;
import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SmellDuplicationCheckerTest {

    public static final String SHA_A = "commit";
    public static final String SHA_B = "commit2";
    private static SmellDuplicationChecker.FileRenameEntry fileRename;
    private static SmellDuplicationChecker.FileRenameEntry sameCommit;
    private static SmellDuplicationChecker.FileRenameEntry sameOldFile;
    private static SmellDuplicationChecker.FileRenameEntry j2gRename;
    private static SmellDuplicationChecker.FileRenameEntry g2jRename;
    private List<Map<String, Object>> filesRenames;
    private Commit parent;
    private QueryEngine queryEngine;

    @BeforeClass
    public static void setUpClass() throws Exception {
        fileRename = new SmellDuplicationChecker.FileRenameEntry(SHA_A, "app/src/main/java/a/b/c.java", "app/src/main/java/a/b/d.java");
        sameCommit = new SmellDuplicationChecker.FileRenameEntry(SHA_A, "java/d/e/f.java", "java/g/h/i.java");
        // No java directory should go until root path
        sameOldFile = new SmellDuplicationChecker.FileRenameEntry(SHA_B, "a/b/c.java", "d/e/f.java");
        // Groovy and Java directories
        j2gRename = new SmellDuplicationChecker.FileRenameEntry(SHA_B, "app/src/main/java/a/b/c.java", "app/src/main/groovy/a/b/d.java");
        g2jRename = new SmellDuplicationChecker.FileRenameEntry(SHA_B, "app/src/main/groovy/a/b/c.java", "app/src/main/java/a/b/d.java");
    }

    private void addRenameEntry(SmellDuplicationChecker.FileRenameEntry... entries) {
        for (SmellDuplicationChecker.FileRenameEntry entry : entries) {
            Map<String, Object> rename = new HashMap<>();
            rename.put(SmellDuplicationChecker.SHA1_COLUMN, entry.sha1);
            rename.put(SmellDuplicationChecker.OLD_FILE_COLUMN, entry.oldFile);
            rename.put(SmellDuplicationChecker.NEW_FILE_COLUMN, entry.newFile);
            filesRenames.add(rename);

        }
    }

    @Before
    public void setUp() {
        filesRenames = new ArrayList<>();
        addRenameEntry(fileRename, sameCommit, sameOldFile, j2gRename, g2jRename);
        this.parent = new Commit(sameCommit.sha1, 0);
        this.queryEngine = mock(QueryEngine.class);
        GraphDatabaseService graphDatabaseService = mock(GraphDatabaseService.class);
        doReturn(graphDatabaseService).when(queryEngine).getGraphDatabaseService();
        doReturn(mock(Result.class)).when(graphDatabaseService).execute(anyString());
        doReturn(mock(Transaction.class)).when(graphDatabaseService).beginTx();
    }

    private SmellDuplicationChecker getDuplicationChecker() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.query(anyString())).thenReturn(filesRenames);
        return new SmellDuplicationChecker(1, persistence, queryEngine);
    }

    private void mockPreviousQualifiedName(String qualifiedName) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(SmellDuplicationChecker.QUALIFIED_NAME, qualifiedName);
        doReturn(Collections.singletonList(resultMap)).when(queryEngine).toMap(any(Result.class));
    }

    @Test
    public void fileRenameEntryEqualsWithSameNewFileAndCommit() {
        SmellDuplicationChecker.FileRenameEntry sameEntry =
                new SmellDuplicationChecker.FileRenameEntry(fileRename.sha1, "anyFile", fileRename.newFile);

        assertEquals(fileRename, sameEntry);
        assertNotEquals(fileRename, sameOldFile);
        assertNotEquals(fileRename, sameCommit);
    }

    @Test
    public void wrongCommitWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "d.e.f", sameCommit.newFile);
        Commit commit = new Commit("anyothercommit", 1);

        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNull(original);
    }

    @Test
    public void wrongFileWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "d.e.f", "anyFile");
        Commit commit = new Commit(sameCommit.sha1, 1);

        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNull(original);
    }

    @Test
    public void parsingErrorWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass", "anyOtherFile");
        Commit commit = new Commit(sameCommit.sha1, 1);

        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNull(original);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedBothInnerClassAndMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass", fileRename.newFile);
        Commit commit = new Commit(sameCommit.sha1, 1);

        mockPreviousQualifiedName("a.b.c");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("method#a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(fileRename.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#d.e.f", sameOldFile.newFile);
        Commit commit = new Commit(sameOldFile.sha1, 1);

        mockPreviousQualifiedName("a.b.c");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("method#a.b.c", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "d.e.f$myInnerClass$AnotherInnerClass", sameOldFile.newFile);
        Commit commit = new Commit(sameOldFile.sha1, 1);

        mockPreviousQualifiedName("a.b.c");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethodNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "g.h.i", sameCommit.newFile);
        Commit commit = new Commit(sameCommit.sha1, 1);

        mockPreviousQualifiedName("d.e.f");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("d.e.f", original.instance);
        assertEquals(sameCommit.oldFile, original.file);
    }

    @Test
    public void sameRenamingInAnotherIsTypeDependant() {
        Smell instance = new Smell("MIM", "d.e.f", sameCommit.newFile);
        Smell newInstanceFurtherCommit = new Smell("HMU", instance.instance, instance.file);
        Commit commit = new Commit(sameCommit.sha1, 1);

        SmellDuplicationChecker checker = getDuplicationChecker();
        // The original guess will create a cache that is used between commits
        Smell original = checker.original(instance, commit, parent);

        Commit new_commit = new Commit("newSha", 2);
        Smell secondOriginal = checker.original(newInstanceFurtherCommit, new_commit, commit);

        assertNotNull(original);
        assertNull(secondOriginal);
    }

    @Test
    public void sameRenamingBackAndForth() {
        String anotherSha = "commit_3";
        addRenameEntry(new SmellDuplicationChecker.FileRenameEntry(anotherSha,
                sameCommit.newFile, sameCommit.oldFile));
        Smell original = new Smell("MIM", "d.e.f", sameCommit.oldFile);
        Smell firstRename = new Smell("MIM", "g.h.i", sameCommit.newFile);
        Smell renameEqualToFirst = new Smell("MIM", original.instance, original.file);
        Commit commit = new Commit(sameCommit.sha1, 1);

        mockPreviousQualifiedName("d.e.f");
        SmellDuplicationChecker checker = getDuplicationChecker();
        // The original guess will create a cache that is used between commits
        Smell expectedOriginal = checker.original(firstRename, commit, parent);

        mockPreviousQualifiedName("g.h.i");
        Commit new_commit = new Commit(anotherSha, 2);
        Smell expectedFirstRename = checker.original(renameEqualToFirst, new_commit, commit);

        assertNotNull(expectedOriginal);
        assertNotNull(expectedFirstRename);
        assertEquals(expectedOriginal, original);
        assertEquals(expectedFirstRename, firstRename);
        assertEquals(original, renameEqualToFirst);
    }

    @Test
    public void java2groovyUpperFolder() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass",
                j2gRename.newFile);
        Commit commit = new Commit(j2gRename.sha1, 1);

        mockPreviousQualifiedName("a.b.c");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("method#a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(j2gRename.oldFile, original.file);
    }

    @Test
    public void groovy2JavaUpperFolder() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass",
                g2jRename.newFile);
        Commit commit = new Commit(g2jRename.sha1, 1);

        mockPreviousQualifiedName("a.b.c");
        SmellDuplicationChecker checker = getDuplicationChecker();
        Smell original = checker.original(instance, commit, parent);

        assertNotNull(original);
        assertEquals("method#a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(g2jRename.oldFile, original.file);
    }
}
