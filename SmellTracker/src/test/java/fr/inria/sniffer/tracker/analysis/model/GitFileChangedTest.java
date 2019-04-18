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

public class GitFileChangedTest {
    @Test
    public void parseOnlyInsertions() throws Exception {
        String line = "CHANGES.rst          | 5 +";

        GitChangedFile parse = GitChangedFile.parseFileChange(line);

        assertEquals("CHANGES.rst", parse.name);
        assertEquals(5, parse.changeSize);
    }

    @Test
    public void parseMixInsertionsDeletions() throws Exception {
        String line = "/any/path/to/file.java | 2 +-";

        GitChangedFile parse = GitChangedFile.parseFileChange(line);

        assertEquals("/any/path/to/file.java", parse.name);
        assertEquals(2, parse.changeSize);
    }

    @Test
    public void parseOnlyDeletions() throws Exception {
        String line = "CHANGES.rst          | 127 -----------";

        GitChangedFile parse = GitChangedFile.parseFileChange(line);

        assertEquals("CHANGES.rst", parse.name);
        assertEquals(127, parse.changeSize);
    }

}
