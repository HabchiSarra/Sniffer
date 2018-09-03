package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.persistence.PostgresTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JDBCProjectQueriesTest extends PostgresTestCase {
    private ProjectQueries queries;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        queries = new JDBCProjectQueries();
    }

    private long getProjectsCount() {
        return countElements("project");
    }

    @Test
    public void testInsertProject() {
        long count = 0;
        String name = "projectName";
        String url = "my/url";

        // We can insert any project.
        executeSuccess(queries.projectInsertStatement(name, url));
        assertEquals(++count, getProjectsCount());

        // We can insert another project with empty url
        executeSuccess(queries.projectInsertStatement("anotherProject", null));
        assertEquals(++count, getProjectsCount());

        // We can insert another any url two times
        executeSuccess(queries.projectInsertStatement("thirdProject", url));
        assertEquals(++count, getProjectsCount());

        // We can't insert the same name two times, whichever the url
        executeNothinhDone(queries.projectInsertStatement(name, url));
        executeNothinhDone(queries.projectInsertStatement(name, null));
        assertEquals(count, getProjectsCount());
    }

    @Test
    public void testIdFromNameQuery() {
        List<Map<String, Object>> result;
        String name = "projectName";
        String anotherProject = "anotherProject";
        String url = "my/url";

        result = persistence.query(queries.idFromNameQuery(name));
        assertTrue(result.isEmpty());

        executeSuccess(queries.projectInsertStatement(name, url));

        // Another project name means no result
        result = persistence.query(queries.idFromNameQuery(anotherProject));
        assertTrue(result.isEmpty());

        // We can query our project
        result = persistence.query(queries.idFromNameQuery(name));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).get("id"));

        // We can insert any other project.
        executeSuccess(queries.projectInsertStatement(anotherProject, null));
        result = persistence.query(queries.idFromNameQuery(anotherProject));
        assertFalse(result.isEmpty());
        assertEquals(2, result.get(0).get("id"));
    }
}