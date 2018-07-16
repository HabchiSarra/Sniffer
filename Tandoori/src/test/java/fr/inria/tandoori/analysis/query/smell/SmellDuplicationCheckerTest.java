package fr.inria.tandoori.analysis.query.smell;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.query.smell.Smell;
import fr.inria.tandoori.analysis.query.smell.SmellDuplicationChecker;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SmellDuplicationCheckerTest {

    private SmellDuplicationChecker checker;
    private static SmellDuplicationChecker.FileRenameEntry fileRename;
    private static SmellDuplicationChecker.FileRenameEntry sameCommit;
    private static SmellDuplicationChecker.FileRenameEntry sameOldFile;
    private final static List<Map<String, Object>> FILE_RENAMES = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        fileRename = new SmellDuplicationChecker.FileRenameEntry("commit", "app/src/main/java/a/b/c.java", "app/src/main/java/a/b/d.java");
        sameCommit = new SmellDuplicationChecker.FileRenameEntry("commit", "java/d/e/f.java", "java/g/h/i.java");
        sameOldFile = new SmellDuplicationChecker.FileRenameEntry("commit2", "java/a/b/c.java", "java/d/e/f.java");
        // No java directory should go until root path
        sameOldFile = new SmellDuplicationChecker.FileRenameEntry("commit2", "a/b/c.java", "d/e/f.java");
        Map<String, Object> rename = new HashMap<>();
        rename.put("sha1", fileRename.sha1);
        rename.put("oldfile", fileRename.oldFile);
        rename.put("newfile", fileRename.newFile);
        FILE_RENAMES.add(rename);

        rename = new HashMap<>();
        rename.put("sha1", sameCommit.sha1);
        rename.put("oldfile", sameCommit.oldFile);
        rename.put("newfile", sameCommit.newFile);
        FILE_RENAMES.add(rename);

        rename = new HashMap<>();
        rename.put("sha1", sameOldFile.sha1);
        rename.put("oldfile", sameOldFile.oldFile);
        rename.put("newfile", sameOldFile.newFile);
        FILE_RENAMES.add(rename);
    }

    @Before
    public void setUp() throws Exception {
        Persistence persistence = mock(Persistence.class);
        when(persistence.query(anyString())).thenReturn(FILE_RENAMES);
        checker = new SmellDuplicationChecker(1, persistence);
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

        Smell original = checker.original(instance, commit);

        assertNull(original);
    }

    @Test
    public void wrongFileWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "d.e.f", "anyFile");
        Commit commit = new Commit(sameCommit.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNull(original);
    }

    @Test
    public void parsingErrorWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass", "anyOtherFile");
        Commit commit = new Commit(sameCommit.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNull(original);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedBothInnerClassAndMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#a.b.d$myInnerClass$AnotherInnerClass", fileRename.newFile);
        Commit commit = new Commit(sameCommit.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNotNull(original);
        assertEquals("method#a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(fileRename.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "method#d.e.f", sameOldFile.newFile);
        Commit commit = new Commit(sameOldFile.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNotNull(original);
        assertEquals("method#a.b.c", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "d.e.f$myInnerClass$AnotherInnerClass", sameOldFile.newFile);
        Commit commit = new Commit(sameOldFile.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNotNull(original);
        assertEquals("a.b.c$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethodNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", "g.h.i", sameCommit.newFile);
        Commit commit = new Commit(sameCommit.sha1, 1);

        Smell original = checker.original(instance, commit);

        assertNotNull(original);
        assertEquals("d.e.f", original.instance);
        assertEquals(sameCommit.oldFile, original.file);
    }

    @Test
    public void sameRenamingInAnotherCommitWillBeFound() {
        Smell instance = new Smell("MIM", "d.e.f", sameCommit.newFile);
        Smell newInstanceFurtherCommit = new Smell(instance.type, instance.instance, instance.file);
        Commit commit = new Commit(sameCommit.sha1, 1);

        // The original guess will create a cache that is used between commits
        Smell original = checker.original(instance, commit);

        commit = new Commit("newSha", 2);
        Smell secondOriginal = checker.original(newInstanceFurtherCommit, commit);

        assertNotNull(secondOriginal);
        assertEquals(original, secondOriginal);
    }

    @Test
    public void sameRenamingInAnotherIsTypeDependant() {
        Smell instance = new Smell("MIM","d.e.f", sameCommit.newFile);
        Smell newInstanceFurtherCommit = new Smell("HMU", instance.instance, instance.file);
        Commit commit = new Commit(sameCommit.sha1, 1);

        // The original guess will create a cache that is used between commits
        Smell original = checker.original(instance, commit);

        commit = new Commit("newSha", 2);
        Smell secondOriginal = checker.original(newInstanceFurtherCommit, commit);

        assertNotNull(original);
        assertNull(secondOriginal);
    }
}