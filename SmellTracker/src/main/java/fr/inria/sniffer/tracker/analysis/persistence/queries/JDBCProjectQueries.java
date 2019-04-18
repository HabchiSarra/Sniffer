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

public class JDBCProjectQueries implements ProjectQueries {

    @Override
    public String projectInsertStatement(String projectName, String url) {
        return "INSERT INTO Project (name, url) " +
                "VALUES ('" + projectName + "', '" + url + "') ON CONFLICT DO NOTHING;";
    }

    @Override
    public String idFromNameQuery(String name) {
        return "SELECT id FROM project WHERE name = '" + name + "'";
    }

}
