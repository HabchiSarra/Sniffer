package fr.inria.sniffer.tracker.analysis.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitRenameTest {

    @Test
    public void parseEntryWithNoBrace() throws Exception {
        String line = "rename a.txt => b.txt (76%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("a.txt", result.oldFile);
        assertEquals("b.txt", result.newFile);
        assertEquals(76, result.similarity);
    }

    @Test
    public void parseEntryStartingWithBraces() throws Exception {
        String line = "rename {a => f}/b/c/d/e.txt (100%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("a/b/c/d/e.txt", result.oldFile);
        assertEquals("f/b/c/d/e.txt", result.newFile);
        assertEquals(100, result.similarity);
    }

    @Test
    public void parseEntryEndingingWithBraces() throws Exception {
        String line = "rename aFWall/src/main/java/dev/ukanth/ufirewall/util/{CustomRule.java => CustomRuleOld.java} (90%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("aFWall/src/main/java/dev/ukanth/ufirewall/util/CustomRule.java", result.oldFile);
        assertEquals("aFWall/src/main/java/dev/ukanth/ufirewall/util/CustomRuleOld.java", result.newFile);
        assertEquals(90, result.similarity);
    }

    @Test
    public void parseEntryWithEmptyOldBody() throws Exception {
        String line = "rename app/src/main/java/com/nbossard/packlist/{ => dui}/AboutActivity.java (94%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("app/src/main/java/com/nbossard/packlist/AboutActivity.java", result.oldFile);
        assertEquals("app/src/main/java/com/nbossard/packlist/dui/AboutActivity.java", result.newFile);
        assertEquals(94, result.similarity);
    }
    @Test
    public void parseEntryWithEmptyNewBody() throws Exception {
        String line = "rename app/src/main/java/com/nbossard/packlist/{dui => }/AboutActivity.java (94%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("app/src/main/java/com/nbossard/packlist/dui/AboutActivity.java", result.oldFile);
        assertEquals("app/src/main/java/com/nbossard/packlist/AboutActivity.java", result.newFile);
        assertEquals(94, result.similarity);
    }

    @Test
    public void parseEntryWithBracesInTheMiddle() throws Exception {
        String line = "rename app/src/main/{groovy => java}/com/nbossard/packlist/gui/NewTripFragment.java (100%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("app/src/main/groovy/com/nbossard/packlist/gui/NewTripFragment.java", result.oldFile);
        assertEquals("app/src/main/java/com/nbossard/packlist/gui/NewTripFragment.java", result.newFile);
        assertEquals(100, result.similarity);
    }

    @Test
    public void parsePathInBraces() throws Exception {
        String line = "rename app/src/main/{groovy/com/nbossard/packlist/gui/AboutActivity.groovy => java/com/nbossard/packlist/gui/AboutActivity.java} (59%)";

        GitRename result = GitRename.parseRenamed(line);

        assertEquals("app/src/main/groovy/com/nbossard/packlist/gui/AboutActivity.groovy", result.oldFile);
        assertEquals("app/src/main/java/com/nbossard/packlist/gui/AboutActivity.java", result.newFile);
        assertEquals(59, result.similarity);
    }

    @Test(expected = Exception.class)
    public void parseWrongEntry() throws Exception {
        String line = "Diff a/b/c/d/{z.txt => c.txt} (100%)";

        GitRename.parseRenamed(line);
    }


}