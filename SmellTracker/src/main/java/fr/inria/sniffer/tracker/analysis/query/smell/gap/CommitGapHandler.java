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
package fr.inria.sniffer.tracker.analysis.query.smell.gap;

import fr.inria.sniffer.tracker.analysis.model.Commit;

/**
 * Manage the gaps between analyzed commits.
 */
public interface CommitGapHandler {

    /**
     * Tells if the commit is not consecutive with the other commit.
     *
     * @param first  The first commit to test.
     * @param second The commit to test against the first.
     * @return True if the two commits ordinal are separated by more than 1, False otherwise.
     */
    boolean hasGap(Commit first, Commit second);

    /**
     * Try to retrieve the next commit in our database in case of a {@link Commit}
     * with no smell at all in Paprika.
     * <p>
     * If the commit is found, this means that all smells of the current type has been refactored.
     * If the commit is NOt found, this means that we had an error in the Paprika analysis.
     *
     * @param previous The previous commit.
     * @throws CommitNotFoundException if no commit exists for the given ordinal and project.
     */
    Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException;

}
