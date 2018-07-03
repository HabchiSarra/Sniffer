package fr.inria.tandoori.analysis.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class GitRenameParserTest {

    @Test
    public void parseEntryWithNoBrace() throws Exception {
        String line = "rename a.txt => b.txt (76%)";

        GitRenameParser.RenameParsingResult result = GitRenameParser.parseRenamed(line);

        assertEquals("a.txt", result.oldFile);
        assertEquals("b.txt", result.newFile);
        assertEquals(76, result.similarity);
    }

    @Test
    public void parseEntryStartingWithBraces() throws Exception {
        String line = "rename {a => f}/b/c/d/e.txt (100%)";

        GitRenameParser.RenameParsingResult result = GitRenameParser.parseRenamed(line);

        assertEquals("a/b/c/d/e.txt", result.oldFile);
        assertEquals("f/b/c/d/e.txt", result.newFile);
        assertEquals(100, result.similarity);
    }

    @Test
    public void parseEntryWithBracesInTheMiddle() throws Exception {
        String line = "rename a/b/c/d/{z.txt => c.txt} (100%)";

        GitRenameParser.RenameParsingResult result = GitRenameParser.parseRenamed(line);

        assertEquals("a/b/c/d/z.txt", result.oldFile);
        assertEquals("a/b/c/d/c.txt", result.newFile);
        assertEquals(100, result.similarity);
    }

    @Test(expected = Exception.class)
    public void parseWrongEntry() throws Exception {
        String line = "Diff a/b/c/d/{z.txt => c.txt} (100%)";

        GitRenameParser.parseRenamed(line);
    }
}