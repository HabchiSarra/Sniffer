package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.model.Smell;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public abstract class PersistenceTestCase {
    protected Persistence persistence;

    @Before
    public void setUp() throws Exception {
        persistence = initializePersistence();
        persistence.initialize();
    }

    @After
    protected void tearDown() throws Exception {
        persistence.execute("DROP SCHEMA tandoori CASCADE;");
    }

    protected abstract Persistence initializePersistence();

    protected void executeFailure(String query) {
        executeExpect(query, -1);
    }

    protected void executeSuccess(String query) {
        executeExpect(query, 1);
    }

    protected void executeNothinhDone(String query) {
        executeExpect(query, 0);
    }

    protected long countElements(String table) {
        List<Map<String, Object>> result = persistence.query("SELECT count(*) as cnt FROM " + table + ";");
        return (long) (result.isEmpty() ? -1L : result.get(0).get("cnt"));
    }

    protected void executeExpect(String query, int returned) {
        int ret = persistence.execute(query);
        assertEquals(returned, ret);
    }

    protected void checkContainsSmells(List<Map<String, Object>> result, List<Smell> smells) {
        Smell instance;
        for (Map<String, Object> mapping : result) {
            instance = Smell.fromTandooriInstance(mapping);
            assertTrue(smells.contains(instance));
        }
    }
}
