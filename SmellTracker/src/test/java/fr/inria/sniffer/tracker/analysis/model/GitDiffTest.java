package fr.inria.sniffer.tracker.analysis.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitDiffTest {

    @Test
    public void parseOnlyInsertions() throws Exception {
        String line = "3 files changed, 231 insertions(+)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(3, parse.getChangedFiles());
        assertEquals(231, parse.getAddition());
        assertEquals(0, parse.getDeletion());
    }

    @Test
    public void parseOnlyDeletions() throws Exception {
        String line = "3 files changed, 5 deletions(-)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(3, parse.getChangedFiles());
        assertEquals(0, parse.getAddition());
        assertEquals(5, parse.getDeletion());
    }
    @Test
    public void parseAnotherLine() throws Exception {
        String line = " 1 file changed, 3 insertions(+), 2 deletions(-)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(1, parse.getChangedFiles());
        assertEquals(3, parse.getAddition());
        assertEquals(2, parse.getDeletion());
    }

    @Test
    public void parseOnlyInsertionSingular() throws Exception {
        String line = "1 file changed, 1 insertion(+)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(1, parse.getChangedFiles());
        assertEquals(1, parse.getAddition());
        assertEquals(0, parse.getDeletion());
    }

    @Test
    public void parseAnotherCorrectLine() throws Exception {
        String line = "8 files changed, 30 insertions(+), 417 deletions(-)";

        GitDiff parse = GitDiff.parse(line);

        assertEquals(8, parse.getChangedFiles());
        assertEquals(30, parse.getAddition());
        assertEquals(417, parse.getDeletion());
    }

    @Test(expected = Exception.class)
    public void parseIncorrectLineWillThrow() throws Exception {
        String line = "SmellTracker/src/main/resources/schema/tracker-sqlite.sql  |  2 ++";

        GitDiff.parse(line);
    }
}