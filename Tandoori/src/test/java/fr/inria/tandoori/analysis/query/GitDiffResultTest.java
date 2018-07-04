package fr.inria.tandoori.analysis.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class GitDiffResultTest {

    @Test
    public void parseCorrectLine() throws Exception {
        String line = "3 files changed, 65 insertions(+), 5 deletions(-)";

        GitDiffResult parse = GitDiffResult.parse(line);

        assertEquals(65, parse.getAddition());
        assertEquals(5, parse.getDeletion());
        assertEquals(3, parse.getChangedFiles());
    }

    @Test(expected = Exception.class)
    public void parseIncorrectLineWillThrow() throws Exception {
        String line = "Tandoori/src/main/resources/schema/tandoori-sqlite.sql  |  2 ++";

        GitDiffResult.parse(line);
    }
}