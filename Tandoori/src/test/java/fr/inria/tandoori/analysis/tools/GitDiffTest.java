package fr.inria.tandoori.analysis.tools;

import fr.inria.tandoori.analysis.tools.GitDiff;
import org.junit.Test;

import static org.junit.Assert.*;

public class GitDiffTest {

    @Test
    public void parseCorrectLine() throws Exception {
        String line = "3 files changed, 65 insertions(+), 5 deletions(-)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(65, parse.getAddition());
        assertEquals(5, parse.getDeletion());
        assertEquals(3, parse.getChangedFiles());
    }
    @Test
    public void parseAnotherCorrectLine() throws Exception {
        String line = "8 files changed, 30 insertions(+), 417 deletions(-)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(30, parse.getAddition());
        assertEquals(417, parse.getDeletion());
        assertEquals(8, parse.getChangedFiles());
    }

    @Test(expected = Exception.class)
    public void parseIncorrectLineWillThrow() throws Exception {
        String line = "Tandoori/src/main/resources/schema/tandoori-sqlite.sql  |  2 ++";

        GitDiff.parse(line);
    }
}