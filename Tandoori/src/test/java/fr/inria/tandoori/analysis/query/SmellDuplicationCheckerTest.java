package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
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
        fileRename = new SmellDuplicationChecker.FileRenameEntry("commit", "a/b/c.java", "a/b/d.java");
        sameCommit = new SmellDuplicationChecker.FileRenameEntry("commit", "d/e/f.java", "g/h/i.java");
        sameOldFile = new SmellDuplicationChecker.FileRenameEntry("commit2", "a/b/c.java", "d/e/f.java");
        Map<String, Object> rename = new HashMap<>();
        rename.put("sha1", fileRename.sha1);
        rename.put("oldFile", fileRename.oldFile);
        rename.put("newFile", fileRename.newFile);
        FILE_RENAMES.add(rename);

        rename = new HashMap<>();
        rename.put("sha1", sameCommit.sha1);
        rename.put("oldFile", sameCommit.oldFile);
        rename.put("newFile", sameCommit.newFile);
        FILE_RENAMES.add(rename);

        rename = new HashMap<>();
        rename.put("sha1", sameOldFile.sha1);
        rename.put("oldFile", sameOldFile.oldFile);
        rename.put("newFile", sameOldFile.newFile);
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
        Smell instance = new Smell("MIM", "anyothercommit", "d.e.f", sameCommit.newFile);

        Smell original = checker.original(instance);

        assertNull(original);
    }

    @Test
    public void wrongFileWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", sameCommit.sha1, "d.e.f", "anyFile");

        Smell original = checker.original(instance);

        assertNull(original);
    }

    @Test
    public void parsingErrorWillNotBeGuessed() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", sameOldFile.sha1, "method#a.b.c$myInnerClass$AnotherInnerClass", "anyOtherFile");

        Smell original = checker.original(instance);

        assertNull(original);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedBothInnerClassAndMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", fileRename.sha1, "method#a.b.c$myInnerClass$AnotherInnerClass", fileRename.newFile);

        Smell original = checker.original(instance);

        assertNotNull(original);
        assertEquals("method#a.b.d$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(fileRename.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", sameOldFile.sha1, "method#a.b.c", sameOldFile.newFile);

        Smell original = checker.original(instance);

        assertNotNull(original);
        assertEquals("method#d.e.f", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethod() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", sameOldFile.sha1, "a.b.c$myInnerClass$AnotherInnerClass", sameOldFile.newFile);

        Smell original = checker.original(instance);

        assertNotNull(original);
        assertEquals("d.e.f$myInnerClass$AnotherInnerClass", original.instance);
        assertEquals(sameOldFile.oldFile, original.file);
    }

    @Test
    public void oldInstanceNameIsCorrectlyGuessedNoMethodNoInnerClass() {
        // The instance name format is critical there.
        Smell instance = new Smell("MIM", sameCommit.sha1, "d.e.f", sameCommit.newFile);

        Smell original = checker.original(instance);

        assertNotNull(original);
        assertEquals("g.h.i", original.instance);
        assertEquals(sameCommit.oldFile, original.file);
    }

    @Test
    public void sameRenamingInAnotherCommitWillBeFound() {
        Smell instance = new Smell("MIM", sameCommit.sha1, "d.e.f", sameCommit.newFile);
        Smell newInstanceFurtherCommit = new Smell(instance.type, "newSha", instance.instance, instance.file);
        // The original guess will create a cache that is used between commits
        Smell original = checker.original(instance);

        Smell secondOriginal = checker.original(newInstanceFurtherCommit);

        assertNotNull(secondOriginal);
        assertEquals(original, secondOriginal);
    }

    @Test
    public void sameRenamingInAnotherIsTypeDependant() {
        Smell instance = new Smell("MIM", sameCommit.sha1, "d.e.f", sameCommit.newFile);
        Smell newInstanceFurtherCommit = new Smell("HMU", "newSha", instance.instance, instance.file);
        // The original guess will create a cache that is used between commits
        Smell original = checker.original(instance);

        Smell secondOriginal = checker.original(newInstanceFurtherCommit);

        assertNull(secondOriginal);
    }
}