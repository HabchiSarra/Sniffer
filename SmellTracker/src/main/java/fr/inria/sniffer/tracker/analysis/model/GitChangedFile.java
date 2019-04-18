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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitChangedFile {
    public final String name;
    public final int changeSize;

    private static final Pattern FILE_CHANGED = Pattern.compile("^([^\\s]*)\\s*\\|\\s*(\\d+)\\s*[+-]*$");

    public GitChangedFile(String name, int changeSize) {
        this.name = name;
        this.changeSize = changeSize;
    }

    /**
     * Define if the given String is a git 'file change' statement.
     * Usually under the possible syntaxes:
     * <p>
     * <pre>
     * <code>
     * CHANGES.rst          | 5 +++++
     * any/file/__init__.py | 2 +-
     * 2 files changed, 6 insertions(+), 1 deletion(-)
     * </code>
     * </pre>
     *
     * @param line The line to parse.
     * @return a {@link GitChangedFile}
     */
    public static GitChangedFile parseFileChange(String line) throws Exception {
        line = line.trim();
        Matcher matcher = FILE_CHANGED.matcher(line);
        if (matcher.find()) {
            return new GitChangedFile(matcher.group(1), Integer.valueOf(matcher.group(2)));
        }
        throw new Exception("Unable to parse line: " + line);
    }


    @Override
    public String toString() {
        return "GitChangedFile{" +
                "name='" + name + '\'' +
                ", changeSize='" + changeSize + '\'' +
                '}';
    }
}
