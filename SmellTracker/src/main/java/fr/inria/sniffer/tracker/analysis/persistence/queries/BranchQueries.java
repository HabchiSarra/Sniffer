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

public interface BranchQueries {
    /**
     * Create a Branch insertion query.
     *
     * @param projectId    Current project.
     * @param ordinal      Branch ordinal.
     * @param parentCommit The {@link Commit} from which this branch forks.
     * @param mergedInto   The last {@link Commit} into which this branch is merged.
     * @return The generated insertion statement.
     */
    String branchInsertionStatement(int projectId, int ordinal, Commit parentCommit, Commit mergedInto);

    /**
     * Create a BranchCommit insertion query.
     *
     * @param projectId     Current project.
     * @param branchOrdinal Branch ordinal.
     * @param commitSha     Sha1 of the commit to insert.
     * @param ordinal       {@link Commit} ordinal in the branch.
     * @return The generated insertion statement.
     */
    String branchCommitInsertionQuery(int projectId, int branchOrdinal, String commitSha, int ordinal);

    /**
     * Query the identifier of a Branch.
     *
     * @param projectId     Current project.
     * @param branchOrdinal Branch ordinal.
     * @return The generated insertion statement.
     */
    String idFromOrdinalQueryStatement(int projectId, int branchOrdinal);

    /**
     * Return the statement to query branch id for the given project in which the commit is located.
     *
     * @param projectId The project identifier.
     * @param commit    The commit identifier.
     * @return The generated query statement.
     */
    String idFromCommitQueryStatement(int projectId, Commit commit);

    /**
     * Retrieve the identifier of the given branch's parent commit.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @return The generated query statement.
     */
    String parentCommitIdQuery(int projectId, int branchId);

    /**
     * Retrieve the sha of the given branch's parent commit.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @return The generated query statement.
     */
    String parentCommitShaQuery(int projectId, int branchId);

    /**
     * Return a list of {@link Smell} definitions extracted from the SmellPresence of the commit previous to the
     * given branch's first commit.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @param smellType Filter the type of smells to retrieve.
     * @return The generated query statement.
     */
    String parentCommitSmellsQuery(int projectId, int branchId, String smellType);

    /**
     * Retrieve the id of the branch last commit.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch on which we look for the last commit sha.
     * @return The generated query statement.
     */
    String lastCommitIdQuery(int projectId, int branchId);

    /**
     * Retrieve the sha of the branch last commit.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch on which we look for the last commit sha.
     * @return The generated query statement.
     */
    String lastCommitShaQuery(int projectId, int branchId);

    /**
     * Retrieve the smells present on the last commit of the branch merged in the given commit.
     *
     * @param projectId The project identifier.
     * @param merge     The {@link Commit} in which the branch is merged.
     * @param smellType Filter the type of smells to retrieve.
     * @return The generated query statement.
     */
    String lastCommitSmellsQuery(int projectId, Commit merge, String smellType);

    /**
     * Retrieve the commit ordinal in the given branch.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch on which we look for the last commit sha.
     * @param commit    The {@link Commit} to look for.
     * @return The generated query statement.
     */
    String commitOrdinalQuery(int projectId, int branchId, Commit commit);

    /**
     * Return the identifier of the second branch this commit is merging, if any.
     *
     * @param projectId The project identifier.
     * @param commit    The {@link Commit} to find a merged branch onto.
     * @return The generated query statement.
     */
    String mergedBranchIdQuery(int projectId, Commit commit);

    /**
     * Return the sha1 from the commit in the given ordinal of the given branch ID.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch on which we look for the commit sha.
     * @param ordinal   {@link Commit} ordinal in the branch.
     * @return The generated query statement.
     */
    String shaFromOrdinalQuery(int projectId, int branchId, int ordinal);

    /**
     * Return the sha1 from the commit in the given ordinal of the given branch ID.
     *
     * @param projectId   The project identifier.
     * @param branchId    The branch on which we look for the commit sha.
     * @param ordinal     {@link Commit} ordinal in the branch.
     * @param paprikaOnly Only return a commit analyzed by paprika.
     * @return The generated query statement.
     */
    String shaFromOrdinalQuery(int projectId, int branchId, int ordinal, boolean paprikaOnly);
}
