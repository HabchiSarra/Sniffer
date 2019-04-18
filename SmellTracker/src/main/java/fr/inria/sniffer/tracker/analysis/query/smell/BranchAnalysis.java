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
import fr.inria.sniffer.tracker.analysis.query.QueryException;

import java.util.List;

interface BranchAnalysis {
    /**
     * Add the {@link List} of {@link Smell} as already existing in the current branch.
     * This means that the smells will be considered as already introduced, and existing in the smell table.
     * For this we set them in both the previousCommitSmells and currentCommitSmells.
     *
     * @param smells The smells to add.
     */
    void addExistingSmells(List<Smell> smells);

    /**
     * Specifically add smells to the second branch of a merge commit.
     * This method is used for adding all smells before a merge commit occurs.
     *
     * @param smells The smells to add.
     */
    void addMergedSmells(List<Smell> smells);

    /**
     * Notify the current analyzed commit instance.
     *
     * @param commit The under analysis {@link Commit}, may be the same as before.
     */
    void notifyCommit(Commit commit);

    /**
     * Notify a new smell on the currently analyzed commit.
     *
     * @param smell The {@link Smell} instance.
     */
    void notifySmell(Smell smell);

    /**
     * Notify the end of smell analysis, the Branch fr.inria.sniffer.detector.analyzer should check for the last
     * commit sha and finalize the analysis.
     *
     * @throws QueryException If anything goes wrong during last commit retrieval.
     */
    void notifyEnd() throws QueryException;

    /**
     * Notify the end of smell analysis, using a specific commit sha.
     *
     * @param lastCommitSha1 The specific sha to use.
     */
    void notifyEnd(String lastCommitSha1);
}
