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