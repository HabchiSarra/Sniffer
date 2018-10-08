package fr.inria.sniffer.tracker.analysis.persistence.queries;

import fr.inria.sniffer.tracker.analysis.persistence.PostgresTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JDBCDeveloperQueriesTest extends PostgresTestCase {
    private DeveloperQueries queries;
    private ProjectQueries projectQueries;

    private int projectId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        projectQueries = new JDBCProjectQueries();
        queries = new JDBCDeveloperQueries();

        this.projectId = createProject("whatever");
    }

    private int createProject(String projectName) {
        executeSuccess(projectQueries.projectInsertStatement(projectName, "url"));
        String idQuery = projectQueries.idFromNameQuery(projectName);
        List<Map<String, Object>> result = persistence.query(idQuery);
        return (int) result.get(0).get("id");
    }

    private long getDevelopersCount() {
        return countElements("developer");
    }

    private long getProjectDevelopersCount() {
        return countElements("project_developer");
    }

    @Test
    public void testInsertDeveloper() {
        long count = 0;
        String name = "dev@name.com";

        // We can insert any developer.
        executeSuccess(queries.developerInsertStatement(name));
        assertEquals(++count, getDevelopersCount());

        // We can insert another developer
        executeSuccess(queries.developerInsertStatement("another@dev.net"));
        assertEquals(++count, getDevelopersCount());

        // We can't insert the same name two times
        executeNothinhDone(queries.developerInsertStatement(name));
        assertEquals(count, getDevelopersCount());
    }

    @Test
    public void testInsertProjectDeveloper() {
        long count = 0;
        String name = "dev@name.com";

        // We can't insert project developer that does not exists
        executeFailure(queries.projectDeveloperInsertStatement(projectId, name));

        // We can insert any existing developer.
        executeSuccess(queries.developerInsertStatement(name));
        executeSuccess(queries.projectDeveloperInsertStatement(projectId, name));
        assertEquals(++count, getProjectDevelopersCount());

        // We can't insert a developer in a project that does not exists
        executeFailure(queries.projectDeveloperInsertStatement(projectId + 1, name));
        assertEquals(count, getProjectDevelopersCount());

        // We can insert another developer
        String anotherDev = "another@dev.net";
        executeSuccess(queries.developerInsertStatement(anotherDev));
        executeSuccess(queries.projectDeveloperInsertStatement(projectId, anotherDev));
        assertEquals(++count, getProjectDevelopersCount());

        // We can't insert the same name two times
        executeNothinhDone(queries.developerInsertStatement(name));
        assertEquals(count, getProjectDevelopersCount());
    }

    @Test
    public void testIdFroEmailQuery() {
        List<Map<String, Object>> result;
        String name = "developer@name.com";
        String anotherDev = "another@email.com";

        result = persistence.query(queries.idFromEmailQuery(name));
        assertTrue(result.isEmpty());

        executeSuccess(queries.developerInsertStatement(name));

        // Another developer name means no result
        result = persistence.query(queries.idFromEmailQuery(anotherDev));
        assertTrue(result.isEmpty());

        // We can query our project
        result = persistence.query(queries.idFromEmailQuery(name));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).get("id"));

        // We can insert any other project.
        executeSuccess(queries.developerInsertStatement(anotherDev));
        result = persistence.query(queries.idFromEmailQuery(anotherDev));
        assertFalse(result.isEmpty());
        assertEquals(2, result.get(0).get("id"));
    }

    @Test
    public void testProjectDeveloperId() {
        List<Map<String, Object>> result;
        String name = "developer@name.com";
        String anotherDev = "another@email.com";

        result = persistence.query(queries.projectDeveloperQuery(projectId, name));
        assertTrue(result.isEmpty());

        executeSuccess(queries.developerInsertStatement(name));
        executeSuccess(queries.projectDeveloperInsertStatement(projectId, name));

        // Another developer name means no result
        result = persistence.query(queries.projectDeveloperQuery(projectId, anotherDev));
        assertTrue(result.isEmpty());

        // Another project means no result
        result = persistence.query(queries.projectDeveloperQuery(projectId + 1, name));
        assertTrue(result.isEmpty());

        // We can query our developer
        result = persistence.query(queries.projectDeveloperQuery(projectId, name));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).get("id"));

        // We can insert any other developer.
        executeSuccess(queries.developerInsertStatement(anotherDev));
        executeSuccess(queries.projectDeveloperInsertStatement(projectId, anotherDev));

        result = persistence.query(queries.projectDeveloperQuery(projectId, anotherDev));
        assertFalse(result.isEmpty());
        assertEquals(2, result.get(0).get("id"));
    }
}