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
 * Use this {@link CommitGapHandler} implementation if you don't need gap handling.
 */
public class DummyGapHandler implements CommitGapHandler {
    private final int projectId;

    public DummyGapHandler(int projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean hasGap(Commit first, Commit second) {
        return false;
    }

    @Override
    public Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException {
        throw new CommitNotFoundException(projectId, previous.ordinal + 1);
    }

}
