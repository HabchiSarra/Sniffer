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

import java.util.Map;
import java.util.Objects;

public class Smell {
    // The smell identifier in SmellTracker persistence.
    public int id = -1;
    public final String type;
    public final String instance;
    public final String file;
    public Smell parent;

    /**
     * Create a new {@link Smell} instance.
     *
     * @param type     The instance type (e.g. MIM, LIC, NLMR, ...)
     * @param instance The instance name
     * @param file     The smell file.
     */
    public Smell(String type, String instance, String file) {
        this.type = type;
        this.instance = instance;
        this.file = file;
        this.parent = null;
    }

    /**
     * Use a Paprika persistence result to build a new {@link Smell}.
     * This will never set the parent {@link Smell} as the information is not
     * known by Paprika.
     * <p>
     * We also remove a trailing '/' from the files to keep a uniform path.
     *
     * @param smell The result to convert.
     * @return The new {@link Smell}.
     */
    public static Smell fromPaprikaInstance(Map<String, Object> smell, String type) {
        String identifier = (String) smell.get("instance");
        String file = (String) smell.get("file_path");
        // The files given by paprika will have a leading '/'
        if (file.startsWith("/")) {
            file = file.substring(1);
        }
        return new Smell(type, identifier, file);
    }

    /**
     * Use a SmellDetector persistence result to build a new {@link Smell}.
     * This will set the parent {@link Smell} if existing.
     *
     * @param smell The result to convert.
     * @return The new {@link Smell}.
     */
    public static Smell fromDetectorInstance(Map<String, Object> smell) {
        int id = (int) smell.get("id");
        String type = ((String) smell.get("type"));
        String identifier = (String) smell.get("instance");
        String file = (String) smell.get("file");
        Smell created = new Smell(type, identifier, file);
        created.id = id;
        created.parent = extractParent(smell);
        return created;
    }

    /**
     * Create a new {@link Smell} instance using the same value as the given
     * {@link Smell} but excluding its parent.
     *
     * @param smell The smell to copy.
     * @return The new {@link Smell}.
     */
    public static Smell copyWithoutParent(Smell smell) {
        return new Smell(smell.type, smell.instance, smell.file);
    }

    /**
     * Generates the parent smell instance.
     *
     * @param smell The smell {@link Map} to extract data from.
     * @return The new parent smell. null if not defined.
     */
    private static Smell extractParent(Map<String, Object> smell) {
        if (!hasParent(smell)) {
            return null;
        }
        String type = ((String) smell.get("parent_type"));
        String identifier = (String) smell.get("parent_instance");
        String file = (String) smell.get("parent_file");
        return new Smell(type, identifier, file);
    }

    private static boolean hasParent(Map<String, Object> smell) {
        return smell.containsKey("parent_type") && smell.get("parent_type") != null &&
                smell.containsKey("parent_instance") && smell.get("parent_instance") != null &&
                smell.containsKey("parent_file") && smell.get("parent_file") != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Smell smell = (Smell) o;
        return Objects.equals(type, smell.type) &&
                Objects.equals(instance, smell.instance) &&
                Objects.equals(file, smell.file) &&
                Objects.equals(parent, smell.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, instance, file, parent);
    }

    @Override
    public String toString() {
        return "Smell{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", instance='" + instance + '\'' +
                ", file='" + file + '\'' +
                ", parent=" + parent +
                '}';
    }
}
