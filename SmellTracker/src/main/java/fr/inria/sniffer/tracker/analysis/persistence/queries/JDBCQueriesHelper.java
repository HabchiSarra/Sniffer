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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JDBCQueriesHelper {
    protected static final Logger logger = LoggerFactory.getLogger(JDBCQueriesHelper.class.getName());

    /**
     * Escape the string to be compatible with double dollar String insertion.
     *
     * @param entry The string to escape.
     * @return The string with every occurences of "$$" replaced by "$'$".
     */
    static String escapeStringEntry(String entry) {
        return entry.replace("$$", "$'$");
    }
}
