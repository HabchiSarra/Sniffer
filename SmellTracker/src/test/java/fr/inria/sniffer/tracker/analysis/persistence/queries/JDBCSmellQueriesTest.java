/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.inria.sniffer.tracker.analysis.persistence.queries;

import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.PostgresTestCase;
import fr.inria.sniffer.tracker.analysis.persistence.SmellCategory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JDBCSmellQueriesTest extends PostgresTestCase {
    private SmellQueries queries;

    private int projectId;
    private Smell smell;
    private ProjectQueries projectQueries;
    private JDBCDeveloperQueries developerQueries;
    private JDBCCommitQueries commitQueries;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        projectQueries = new JDBCProjectQueries();
        developerQueries = new JDBCDeveloperQueries();
        commitQueries = new JDBCCommitQueries(developerQueries);
        queries = new JDBCSmellQueries(commitQueries);

        this.projectId = createProject("anyProjectName", projectQueries);

        smell = new Smell("LIC", "instance", "file");
    }

    private Commit prepareCommit() {
        return prepareCommit("sha");
    }

    private Commit prepareCommit(String sha) {
        return prepareCommit(projectId, sha, developerQueries, commitQueries);
    }

    private long getSmellCount() {
        return countElements("smell");
    }

    private long getSmellCount(SmellCategory category) {
        return countElements(category.getName());
    }

    private long getLostSmellCount(SmellCategory category) {
        return countElements("lost_" + category.getName());
    }

    @Test
    public void testInsertSmell() {
        long count = 0;

        // We can insert any smell.
        executeSuccess(queries.smellInsertionStatement(projectId, smell));
        String idQuery = queries.lastSmellIdQuery(projectId);
        List<Map<String, Object>> result = persistence.query(idQuery);
        smell.id = (int) result.get(0).get("id");
        assertEquals(++count, getSmellCount());

        // We can insert another smell type
        Smell anotherSmellType = new Smell("MIM", smell.instance, smell.file);
        executeSuccess(queries.smellInsertionStatement(projectId, anotherSmellType));
        assertEquals(++count, getSmellCount());

        // We can insert another smell instance
        Smell anotherSmellInstance = new Smell(smell.type, "anotherinstance", smell.file);
        executeSuccess(queries.smellInsertionStatement(projectId, anotherSmellInstance));
        assertEquals(++count, getSmellCount());

        // We can insert another smell file
        Smell anotherSmellFile = new Smell(smell.type, smell.instance, "anotherfile");
        executeSuccess(queries.smellInsertionStatement(projectId, anotherSmellFile));
        assertEquals(++count, getSmellCount());

        // We don't insert the same smell
        Smell sameSmell = new Smell(smell.type, smell.instance, smell.file);
        executeNothinhDone(queries.smellInsertionStatement(projectId, sameSmell));
        assertEquals(count, getSmellCount());

        // We insert a smell with parent even if the same
        Smell childSmell = new Smell(smell.type, smell.instance, smell.file);
        childSmell.parent = smell;
        executeSuccess(queries.smellInsertionStatement(projectId, childSmell));
        assertEquals(++count, getSmellCount());
    }

    @Test
    public void testInsertSmellCategory() {
        smell.id = createSmell(projectId, smell, queries);
        Commit commit = prepareCommit();

        // Insert will fail if commit does not exists
        executeFailure(
                queries.smellCategoryInsertionStatement(projectId, "any", smell, SmellCategory.INTRODUCTION)
        );

        // Insert will go to the right category
        executeSuccess(
                queries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.INTRODUCTION)
        );
        assertEquals(1, getSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(0, getSmellCount(SmellCategory.REFACTOR));
        assertEquals(0, getSmellCount(SmellCategory.PRESENCE));

        executeSuccess(
                queries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.REFACTOR)
        );
        assertEquals(1, getSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(1, getSmellCount(SmellCategory.REFACTOR));
        assertEquals(0, getSmellCount(SmellCategory.PRESENCE));

        executeSuccess(
                queries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.PRESENCE)
        );
        assertEquals(1, getSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(1, getSmellCount(SmellCategory.REFACTOR));
        assertEquals(1, getSmellCount(SmellCategory.PRESENCE));

        // Two inserts in same category won't update category count.
        executeFailure(
                queries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.PRESENCE)
        );
        assertEquals(1, getSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(1, getSmellCount(SmellCategory.REFACTOR));
        assertEquals(1, getSmellCount(SmellCategory.PRESENCE));
    }

    @Test
    public void testInsertLostSmellCategory() {
        persistence.execute(queries.smellInsertionStatement(projectId, smell));
        Commit commit = prepareCommit();

        // Insert will go to the right category
        executeSuccess(
                queries.lostSmellCategoryInsertionStatement(projectId, smell, SmellCategory.INTRODUCTION, 0, 4)
        );
        assertEquals(1, getLostSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(0, getLostSmellCount(SmellCategory.REFACTOR));

        executeSuccess(
                queries.lostSmellCategoryInsertionStatement(projectId, smell, SmellCategory.REFACTOR, 0, 4)
        );
        assertEquals(1, getLostSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(1, getLostSmellCount(SmellCategory.REFACTOR));

        // We don't have a lost_smell_presence, that doesn't make sense
        executeFailure(
                queries.lostSmellCategoryInsertionStatement(projectId, smell, SmellCategory.PRESENCE, 0, 4)
        );

        // Two inserts in same category will update category count since we can't check unicity here.
        executeSuccess(
                queries.lostSmellCategoryInsertionStatement(projectId, smell, SmellCategory.INTRODUCTION, 0, 4)
        );
        assertEquals(2, getLostSmellCount(SmellCategory.INTRODUCTION));
        assertEquals(1, getLostSmellCount(SmellCategory.REFACTOR));
    }

    @Test
    public void testSmellIdQuery() {
        List<Map<String, Object>> result;

        // No smell means no result
        result = persistence.query(queries.smellIdQuery(projectId, smell));
        assertTrue(result.isEmpty());

        Smell anotherSmellType = new Smell("MIM", smell.instance, smell.file);
        executeSuccess(queries.smellInsertionStatement(projectId, smell));

        // Another smell means no result
        result = persistence.query(queries.smellIdQuery(projectId, anotherSmellType));
        assertTrue(result.isEmpty());

        // We can query our smell
        result = persistence.query(queries.smellIdQuery(projectId, smell));
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).get("id"));

        // The same smell with parent is not returned
        smell.parent = anotherSmellType;
        result = persistence.query(queries.smellIdQuery(projectId, smell));
        assertTrue(result.isEmpty());

        anotherSmellType.id = createSmell(projectId, anotherSmellType, queries);

        executeSuccess(queries.smellInsertionStatement(projectId, smell));

        // Our parent smell is now inserted
        result = persistence.query(queries.smellIdQuery(projectId, anotherSmellType));
        assertFalse(result.isEmpty());
        assertEquals(2, result.get(0).get("id"));

        // The smell with parent is now inserted
        result = persistence.query(queries.smellIdQuery(projectId, smell));
        assertFalse(result.isEmpty());
        assertEquals(3, result.get(0).get("id"));

        // We can't insert two times the same smell
        executeNothinhDone(queries.smellInsertionStatement(projectId, smell));
    }

    @Test
    public void testCommitSmellsQuery() {
        List<Map<String, Object>> result;
        Commit commit = prepareCommit("sha");
        Commit second_commit = prepareCommit("another_sha");
        String commitIdQuery = commitQueries.idFromShaQuery(projectId, commit.sha);
        int commitId = (int) persistence.query(commitIdQuery).get(0).get("id");

        // No smell means no result
        result = persistence.query(queries.commitSmellsQuery(projectId, String.valueOf(commitId), smell.type));
        assertTrue(result.isEmpty());

        Smell anotherSmellType = new Smell("MIM", smell.instance, "yetanotherfile");
        Smell anotherSmell = new Smell(smell.type, "anotherinstance", "anotherfile");
        Smell smellOtherCommit = new Smell(smell.type, "my.smell.instance", "anotherfile");
        smell.id = createSmell(projectId, smell, queries);
        anotherSmell.id = createSmell(projectId, anotherSmell, queries);
        anotherSmellType.id = createSmell(projectId, anotherSmellType, queries);
        smellOtherCommit.id = createSmell(projectId, smellOtherCommit, queries);
        // Presences in first commit
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, commit.sha, smell, SmellCategory.PRESENCE));
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, commit.sha, anotherSmell, SmellCategory.PRESENCE));
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, commit.sha, anotherSmellType, SmellCategory.PRESENCE));
        // Presences in second commit
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, second_commit.sha, anotherSmell, SmellCategory.PRESENCE));
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, second_commit.sha, smellOtherCommit, SmellCategory.PRESENCE));

        // We can use the raw commit ID
        result = persistence.query(queries.commitSmellsQuery(projectId, String.valueOf(commitId), null));
        assertEquals(3, result.size());
        checkContainsSmells(result, Arrays.asList(smell, anotherSmellType, anotherSmell));

        // We can use a query to retrieve commitId
        result = persistence.query(queries.commitSmellsQuery(projectId, "(" + commitIdQuery + ")", null));
        assertEquals(3, result.size());
        checkContainsSmells(result, Arrays.asList(smell, anotherSmellType, anotherSmell));

        // We can filter by smell type
        result = persistence.query(queries.commitSmellsQuery(projectId, String.valueOf(commitId), smell.type));
        assertEquals(2, result.size());
        checkContainsSmells(result, Arrays.asList(smell, anotherSmell));

        // We can return the smell parent's values
        Smell withParent = new Smell(smell.type, "withParentInstance", "withParentFile");
        withParent.parent = smell;
        withParent.id = createSmell(projectId, withParent, queries);
        executeSuccess(queries.smellCategoryInsertionStatement(projectId, commit.sha, withParent, SmellCategory.PRESENCE));
        result = persistence.query(queries.commitSmellsQuery(projectId, String.valueOf(commitId), smell.type));
        assertEquals(3, result.size());
        checkContainsSmells(result, Arrays.asList(smell, anotherSmell, withParent));

        // We filter by commit
        result = persistence.query(queries.commitSmellsQuery(projectId, "(" + commitQueries.idFromShaQuery(projectId, second_commit.sha) + ")", smell.type));
        assertEquals(2, result.size());
        checkContainsSmells(result, Arrays.asList(anotherSmell, smellOtherCommit));
    }

}