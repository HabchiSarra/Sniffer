package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.SQLitePersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SmellDeduplicationQueryTest {

    public static final String TEST_DB_PATH = ".test.sqlite";

    @Before
    public void setUp() throws Exception {
        new File(TEST_DB_PATH).delete();
    }

    @After
    public void tearDown() throws Exception {
        new File(TEST_DB_PATH).delete();
    }

    @Test
    @Ignore("This method is not used anymore! (and never worked anyway)")
    public void testDeduplicationOutput() throws QueryException {
        File file = new File(getClass().getResource("/databases/smell_duplicate-sqlite.sql").getFile());
        SQLitePersistence persistence = new SQLitePersistence(TEST_DB_PATH, "/databases/smell_duplicate-sqlite.sql");
        SmellDeduplicationQuery query = new SmellDeduplicationQuery(persistence);

        persistence.initialize();
        checkNumberOfSmells(persistence, 8);
        // The first smell is present 3 times
        checkSmellPresenceMergedOccurrences(persistence, 1, 2);

        query.query();

        checkNumberOfSmells(persistence, 7);
        checkSmellPresenceMergedOccurrences(persistence, 3, 0);
    }

    private void checkSmellPresenceMergedOccurrences(SQLitePersistence persistence, int mainSmell, int unmergedSmells) {
        // 1 is the ID of our duplicate smell in the test database
        List<Map<String, Object>> smells = persistence.query("SELECT id FROM smellPresence where id = 1");
        assertEquals(smells.size(), mainSmell); // We merged 2 duplicate smells as the first one

        smells = persistence.query("SELECT id FROM smellPresence where id in (3, 6)");
        assertEquals(smells.size(), unmergedSmells); // We merged 2 duplicate smells as the first one
    }

    private void checkNumberOfSmells(SQLitePersistence persistence, int number) {
        List<Map<String, Object>> smells = persistence.query("SELECT id FROM smell");
        assertEquals(smells.size(), number); // We removed 2 duplicate smell
    }
}