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
package fr.inria.sniffer.tracker.analysis.query.smell;

import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.SmellCategory;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrdinalSmellTypeAnalysisTest extends SmellTypeAnalysisTestCase {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private OrdinalSmellTypeAnalysis getAnalysis() {
        return new OrdinalSmellTypeAnalysis(projectId, persistence, smellList.iterator(), smellType, duplicationChecker,
                commitQueries, smellQueries);
    }

    @Test(expected = QueryException.class)
    public void testNoEndCommitFoundWillThrow() throws QueryException {
        addSmell(firstCommit, firstSmell);

        mockNoEndCommit();
        getAnalysis().query();
        debugSmellInsertions();
    }

    /**
     * <pre><code>
     * * A (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handlePresenceAndIntroductionOnSingleCommitAlsoLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);

        mockEndCommit(firstCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(2)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B ()
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handlePresenceAndIntroductionOnSingleCommitNotLastCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        String lastCommitSha = "another";

        mockGapCommit(lastCommitSha);
        mockEndCommit(lastCommitSha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, lastCommitSha, firstSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1   )
     * |
     * * B (   2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleIntroductionAndRefactoring() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(5)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B (1, 2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleIntroductionOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);
        addSmell(secondCommit, secondSmell);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(5)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1, 2)
     * |
     * * B (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleRefactorOnly() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(secondCommit, firstSmell);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();



        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(6)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, secondSmell, SmellCategory.REFACTOR);
    }

    /**
     * <pre><code>
     * * A (1)
     * |
     * * B (1)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleNoChange() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, firstSmell);

        mockEndCommit(secondCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, firstSmell, SmellCategory.PRESENCE);
    }

    /**
     * <pre><code>
     * * A (1, 2)
     * |
     * x Missing commit B
     * |
     * * C (  2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleCommitGap() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);

        String gapCommitSha = "someSha";
        mockGapCommit(gapCommitSha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();


        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries, times(2)).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and 3rd commits will insert the secondSmell since 3rd has no idea it existed.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // The missing Ordinal 1 will be replaced by an empty commit
        verify(smellQueries).smellCategoryInsertionStatement(projectId, gapCommitSha, firstSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, gapCommitSha, secondSmell, SmellCategory.REFACTOR);

        // 3rd commit is counted as introducing the commit back.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1  )
     * |
     * * B (  2)
     * |
     * x Missing commit B
     * |
     * ...
     * |
     * x Missing commit N
     * |
     * * M (  2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleMultipleMissingCommits() throws QueryException {
        Commit anotherCommit = new Commit("thirdSha", 24);
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(anotherCommit, secondSmell);

        String gapCommitSha = "someSha";
        mockGapCommit(gapCommitSha);
        mockEndCommit(anotherCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries, times(2)).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and Nth commits will insert the secondSmell since 3rd has no idea it existed.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.INTRODUCTION);

        // The missing Ordinal 1 will be replaced by an empty commit
        verify(smellQueries).smellCategoryInsertionStatement(projectId, gapCommitSha, firstSmell, SmellCategory.REFACTOR);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, gapCommitSha, secondSmell, SmellCategory.REFACTOR);

        // Nth commit is counted as introducing the commit back.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, anotherCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, anotherCommit.sha, secondSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * * A (1, 2)
     * |
     * * Missing B
     * |
     * * C (   2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleMissingGapCommit() throws QueryException {
        addSmell(firstCommit, firstSmell);
        addSmell(firstCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);

        mockNoGapCommit(); // We pray for this not to happen, but we have to be prepared for the worst.
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, secondSmell);

        verify(persistence, times(6)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
        // The 1st and Nth commits will insert the secondSmell since 3rd has no idea it existed.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, secondSmell, SmellCategory.PRESENCE);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, secondSmell, SmellCategory.PRESENCE);

        // Since we couldn't find the missing commit, we put the smells in LostRefactor and no presence.
        verify(smellQueries).lostSmellCategoryInsertionStatement(projectId, firstSmell, SmellCategory.REFACTOR, secondCommit.ordinal, thirdCommit.ordinal);
    }

    /**
     * <pre><code>
     * * A (1   )
     * |
     * * B (   2) [1 -> 2]
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleRenamedSmell() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        Smell expectedSecondSmell = mockSmellRenamed(secondCommit, secondSmell, firstSmell);
        mockSmellId(expectedSecondSmell);
        mockEndCommit(secondCommit.sha);
        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        // Check that the renamed commit has a set parent
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(expectedSecondSmell, renamed);
        assertEquals(firstSmell, renamed.parent);

        verify(persistence, times(3)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);

    }

    /**
     * <pre><code>
     * * A (1   )
     * |
     * * B (   2) [1 -> 2]
     * |
     * * C (   2)
     * </code></pre>
     *
     * @throws QueryException
     */
    @Test
    public void handleRenamedSmellMultipleCommits() throws QueryException {
        ArgumentCaptor<Smell> smellCaptor = ArgumentCaptor.forClass(Smell.class);
        addSmell(firstCommit, firstSmell);
        addSmell(secondCommit, secondSmell);
        addSmell(thirdCommit, secondSmell);

        // This means that the firstSmell instance has been renamed to second smell in the secondCommit
        Smell expectedSecondSmell = mockSmellRenamed(secondCommit, secondSmell, firstSmell);
        mockSmellId(expectedSecondSmell);
        mockSmellRenamed(thirdCommit, secondSmell, firstSmell); // The smell should still be recognized as renamed
        mockEndCommit(thirdCommit.sha);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(2)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries, times(2)).smellInsertionStatement(eq(projectId), smellCaptor.capture());
        // Check that the renamed commit has a set parent
        Smell renamed = smellCaptor.getAllValues().get(1);
        assertEquals(expectedSecondSmell, renamed);
        assertEquals(firstSmell, renamed.parent);

        verify(persistence, times(4)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, firstCommit.sha, firstSmell, SmellCategory.INTRODUCTION);

        // We introduce the new smell instance definition with renamed_from filled in.
        // Since we use a captor we have to check all invocations of smellInsertionStatement...
        verify(smellQueries).smellCategoryInsertionStatement(projectId, secondCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);

        // We won't introduce the same rename multiple times, as before.
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, expectedSecondSmell, SmellCategory.PRESENCE);

    }

    @Test
    public void handleFirstCommitIsNotTheFirstOrdinal() throws QueryException {
        addSmell(thirdCommit, firstSmell);

        mockGapCommit(firstCommit.sha);
        mockEndCommit(thirdCommit.sha);
        getAnalysis().query();

        verify(persistence, times(1)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);

        verify(persistence, times(2)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, thirdCommit.sha, firstSmell, SmellCategory.INTRODUCTION);
    }

    /**
     * <pre><code>
     * .  A (1,     )
     * |
     * .  B (   2,  ) [1 -> 2]
     * |
     * .  C (   2,  )
     * |
     * .  D (1,     ) [2 -> 1]
     * |
     * .  E (1,     )
     * |
     * .  F (1,     )
     * |
     * .  G (1,     )
     * </pre></code>
     *
     * @throws QueryException
     */
    @Test
    public void testRenamedSmellsBackToOriginal() throws QueryException {
        Commit A = new Commit("0-A", 0);
        Commit B = new Commit("0-B", 1);
        Commit C = new Commit("0-C", 2);
        Commit D = new Commit("0-D", 3);
        Commit E = new Commit("0-E", 4);
        Commit F = new Commit("0-F", 5);
        Commit G = new Commit("0-G", 6);

        // This define the input order
        addSmell(A, firstSmell);
        addSmell(B, secondSmell);
        addSmell(C, secondSmell);
        addSmell(D, firstSmell);
        addSmell(E, firstSmell);
        addSmell(F, firstSmell);
        addSmell(G, firstSmell);

        mockEndCommit(G.sha);

        Smell expectedSecondSmell = mockSmellRenamed(B, secondSmell, firstSmell);
        mockSmellRenamed(C, secondSmell, firstSmell);
        Smell expectedRenamedFirstSmell = mockSmellRenamed(D, firstSmell, secondSmell);
        mockSmellRenamed(E, firstSmell, secondSmell);
        mockSmellRenamed(F, firstSmell, secondSmell);
        mockSmellRenamed(G, firstSmell, secondSmell);

        mockSmellId(expectedSecondSmell);
        expectedRenamedFirstSmell.parent = expectedSecondSmell;
        mockSmellId(expectedRenamedFirstSmell);

        getAnalysis().query();
        debugSmellInsertions();

        verify(persistence, times(3)).execute(any());
        verify(smellQueries).smellInsertionStatement(projectId, firstSmell);
        verify(smellQueries).smellInsertionStatement(projectId, expectedSecondSmell);
        verify(smellQueries).smellInsertionStatement(projectId, expectedRenamedFirstSmell);

        verify(persistence, times(8)).addStatements(any());
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, A.sha, firstSmell, SmellCategory.INTRODUCTION);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, B.sha, expectedSecondSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, C.sha, expectedSecondSmell, SmellCategory.PRESENCE);

        verify(smellQueries).smellCategoryInsertionStatement(projectId, D.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, E.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, F.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
        verify(smellQueries).smellCategoryInsertionStatement(projectId, G.sha, expectedRenamedFirstSmell, SmellCategory.PRESENCE);
    }

}
