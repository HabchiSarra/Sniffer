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

import fr.inria.sniffer.tracker.analysis.model.Tag;

public class JDBCTagQueries implements TagQueries {

    private CommitQueries commitQueries;

    public JDBCTagQueries(CommitQueries commitQueries) {
        this.commitQueries = commitQueries;
    }

    @Override
    public String tagInsertionStatement(int projectId, Tag tag) {
        String commitId = "(" + commitQueries.idFromShaQuery(projectId, tag.getSha()) + ")";
        return "INSERT INTO tag (project_id, commit_id, name, date) " +
                "VALUES " +
                "(" + projectId + ", " + commitId + ",  '"
                + tag.getName() + "', '" + tag.getDate() + "') " +
                "ON CONFLICT DO NOTHING";
    }
}
