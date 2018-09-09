package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.persistence.queries.DeveloperQueries;
import fr.inria.tandoori.analysis.persistence.queries.ProjectQueries;
import fr.inria.tandoori.analysis.persistence.queries.SmellQueries;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
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

    protected int createProject(String projectName, ProjectQueries projectQueries) {
        executeSuccess(projectQueries.projectInsertStatement(projectName, "url"));
        String idQuery = projectQueries.idFromNameQuery(projectName);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }

    protected Commit prepareCommit(int projectId, String sha, DeveloperQueries developerQueries, CommitQueries commitQueries) {
        String devName = "author@email.com";
        Commit commit = new Commit(sha, 1, new DateTime(), "message", devName, new ArrayList<>());
        createCommit(projectId, commit, developerQueries, commitQueries);
        return commit;
    }

    protected int createCommit(int projectId, Commit commit, DeveloperQueries developerQueries, CommitQueries commitQueries) {
        persistence.execute(developerQueries.developerInsertStatement(commit.authorEmail)); // May return 1 or 0
        executeSuccess(commitQueries.commitInsertionStatement(projectId, commit, GitDiff.EMPTY));
        String idQuery = commitQueries.idFromShaQuery(projectId, commit.sha);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }

    protected int createSmell(int projectId, Smell smell, SmellQueries smellQueries) {
        persistence.execute(smellQueries.smellInsertionStatement(projectId, smell));
        String idQuery = smellQueries.smellIdQuery(projectId, smell);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }
}
