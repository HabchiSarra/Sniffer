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
