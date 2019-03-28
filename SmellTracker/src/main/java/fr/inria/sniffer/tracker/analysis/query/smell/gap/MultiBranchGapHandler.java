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
import fr.inria.sniffer.tracker.analysis.persistence.queries.BranchQueries;

import java.util.List;
import java.util.Map;

public class MultiBranchGapHandler implements CommitGapHandler {
    private final Persistence persistence;
    private final BranchQueries branchQueries;
    private final int projectId;
    private final int branchId;

    public MultiBranchGapHandler(int projectId, int branchId, Persistence persistence, BranchQueries branchQueries) {
        this.persistence = persistence;
        this.branchQueries = branchQueries;
        this.projectId = projectId;
        this.branchId = branchId;
    }

    @Override
    public boolean hasGap(Commit first, Commit second) {
        return Math.abs(second.getBranchOrdinal() - first.getBranchOrdinal()) > 1;
    }

    @Override
    public Commit fetchNoSmellCommit(Commit previous) throws CommitNotFoundException {
        int branchOrdinal = previous.getBranchOrdinal() + 1;
        List<Map<String, Object>> result = persistence.query(branchQueries.shaFromOrdinalQuery(projectId, branchId, branchOrdinal, true));
        if (result.isEmpty() || result.get(0).get("sha1") == null) {
            throw new CommitNotFoundException(projectId, previous.getOrdinal() + 1);
        }
        Commit commit = new Commit(String.valueOf(result.get(0).get("sha1")), previous.getOrdinal());
        commit.setBranchOrdinal(branchOrdinal);
        return commit;
    }

}
