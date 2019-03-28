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

public interface DeveloperQueries {
    /**
     * Generate a statement inserting the developer into the persistence.
     *
     * @param developerName The developer name
     * @return The generated insertion statement.
     */
    String developerInsertStatement(String developerName);

    /**
     * Generate a statement binding the developer to the project into the persistence.
     *
     * @param projectId     The project identifier.
     * @param developerName The developer name (must be in developer table).
     * @return The generated insertion statement.
     */
    String projectDeveloperInsertStatement(int projectId, String developerName);

    /**
     * Query the identifier of a developer.
     *
     * @param email Developer email.
     * @return The generated query statement.
     */
    String idFromEmailQuery(String email);

    /**
     * Query the identifier of a project_developer.
     *
     * @param projectId Project to look into.
     * @param email     Developer email.
     * @return The generated query statement.
     */
    String projectDeveloperQuery(int projectId, String email);
}
