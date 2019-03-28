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
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;

import java.util.List;
import java.util.Map;

public class SingleBranchGapHandler implements CommitGapHandler {
    private final Persistence persistence;
    private final CommitQueries commitQueries;
    private final int projectId;

    public SingleBranchGapHandler(int projectId, Persistence persistence, CommitQueries commitQueries) {
        this.persistence = persistence;
        this.commitQueries = commitQueries;
        this.projectId = projectId;
    }

    @Override
    public boolean hasGap(Commit first, Commit second) {
        return Math.abs(second.getOrdinal() - first.getOrdinal()) > 1;
    }

    @Override
    public Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException {
        int ordinal = previous.getOrdinal() + 1;
        String statement = commitQueries.shaFromOrdinalQuery(projectId, ordinal, true);
        List<Map<String, Object>> result = persistence.query(statement);
        if (result.isEmpty()) {
            throw new CommitNotFoundException(projectId, ordinal);
        }
        return new Commit(String.valueOf(result.get(0).get("sha1")), ordinal);
    }

}
