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
package fr.inria.sniffer.tracker.analysis.model;


import org.joda.time.DateTime;

public class Tag {
    private final String TAG_PREFIX = "refs/tags/";
    private final String name;
    private final String sha;
    private DateTime date;

    /**
     * @param name Tag name
     * @param sha  Sha of the object on which this tag is set.
     * @param date Date of the commit linked to the tag.
     */
    public Tag(String name, String sha, DateTime date) {
        if (name.startsWith(TAG_PREFIX)) {
            this.name = name.substring(TAG_PREFIX.length());
        } else {
            this.name = name;
        }
        this.sha = sha;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getSha() {
        return sha;
    }

    public DateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", sha='" + sha + '\'' +
                ", date=" + date +
                '}';
    }
}
