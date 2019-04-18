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

public class JDBCDeveloperQueries extends JDBCQueriesHelper implements DeveloperQueries {
    @Override
    public String developerInsertStatement(String developerName) {
        return "INSERT INTO developer (username) VALUES ($$" + escapeStringEntry(developerName) + "$$) ON CONFLICT DO NOTHING;";
    }

    @Override
    public String projectDeveloperInsertStatement(int projectId, String developerName) {
        return "INSERT INTO project_developer (developer_id, project_id) VALUES (" +
                "(" + idFromEmailQuery(developerName) + "), " + projectId + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromEmailQuery(String email) {
        return "SELECT id FROM developer WHERE username = $$" + escapeStringEntry(email) + "$$";
    }

    @Override
    public String projectDeveloperQuery(int projectId, String email) {
        String devQuery = idFromEmailQuery(email);
        return "SELECT id FROM project_developer WHERE developer_id = (" + devQuery + ") AND project_id = " + projectId;
    }
}
