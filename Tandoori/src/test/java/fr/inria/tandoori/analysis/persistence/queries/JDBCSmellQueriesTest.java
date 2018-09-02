package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Smell;
import fr.inria.tandoori.analysis.persistence.PostgresTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JDBCSmellQueriesTest extends PostgresTestCase {
    private SmellQueries queries;
    private int projectId;
    private static final String COUNT_SMELLS = "SELECT count(*) as cnt from smell;";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        JDBCDeveloperQueries developerQueries = new JDBCDeveloperQueries();
        JDBCCommitQueries commitQueries = new JDBCCommitQueries(developerQueries);
        queries = new JDBCSmellQueries(commitQueries);

        ProjectQueries projectQueries = new JDBCProjectQueries();
        String projectName = "anyProjectName";
        persistence.execute(projectQueries.projectInsertStatement(projectName, "url"));
        String idQuery = projectQueries.idFromNameQuery(projectName);
        List<Map<String, Object>> result = persistence.query(idQuery);
        this.projectId = (int) result.get(0).get("id");
    }

    private long getSmellCount() {
        List<Map<String, Object>> result = persistence.query(COUNT_SMELLS);
        return (long) result.get(0).get("cnt");
    }


    @Test
    public void testInsertSmell() {
        long count = 0;

        // We can insert any smell.
        Smell smell = new Smell("LIC", "instance", "file");
        persistence.execute(queries.smellInsertionStatement(projectId, smell));
        assertEquals(++count, getSmellCount());

        // We can insert another smell type
        Smell anotherSmellType = new Smell("MIM", smell.instance, smell.file);
        persistence.execute(queries.smellInsertionStatement(projectId, anotherSmellType));
        assertEquals(++count, getSmellCount());

        // We can insert another smell instance
        Smell anotherSmellInstance = new Smell(smell.type, "anotherinstance", smell.file);
        persistence.execute(queries.smellInsertionStatement(projectId, anotherSmellInstance));
        assertEquals(++count, getSmellCount());

        // We can insert another smell file
        Smell anotherSmellFile = new Smell(smell.type, smell.instance, "anotherfile");
        persistence.execute(queries.smellInsertionStatement(projectId, anotherSmellFile));
        assertEquals(++count, getSmellCount());

        // We don't insert the same smell
        Smell sameSmell = new Smell(smell.type, smell.instance, smell.file);
        persistence.execute(queries.smellInsertionStatement(projectId, sameSmell));
        assertEquals(count, getSmellCount());

        // We insert a smell with parent even if the same
        Smell childSmell = new Smell(smell.type, smell.instance, smell.file);
        childSmell.parent = smell;
        persistence.execute(queries.smellInsertionStatement(projectId, childSmell));
        assertEquals(++count, getSmellCount());
    }
}