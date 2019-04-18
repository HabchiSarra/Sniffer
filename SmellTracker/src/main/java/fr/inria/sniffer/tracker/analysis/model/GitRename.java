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

public final class GitRename {
    public final String oldFile;
    public final String newFile;
    public final int similarity;

    public GitRename(String oldFile, String newFile, int similarity) {
        this.oldFile = oldFile;
        this.newFile = newFile;
        this.similarity = similarity;
    }

    private static final Pattern RENAME_WITH_BRACKETS = Pattern.compile("^rename\\s([^{]*)\\{(.*)\\s=>\\s([^}]*)\\}(.*)\\s\\((\\d+)%\\)$");
    private static final Pattern RENAME_WITHOUT_BRACKETS = Pattern.compile("^rename\\s(.*)\\s=>\\s(.*)\\s\\((\\d+)%\\)$");


    /**
     * Define if the given String is a git 'rename' statement.
     * Usually under the possible syntaxes:
     * <p>
     * rename a/b/c/d/{z.txt => c.txt} (100%)
     * rename {a => f}/b/c/d/e.txt (100%)
     * rename a.txt => b.txt (76%)
     * TODO: Handle rename toto/{/{test/a.txt => b.txt} (100%)
     *
     * @param line The line to parse.
     * @return a {@link GitRename}
     */
    public static GitRename parseRenamed(String line) throws Exception {
        line = line.trim();
        if (line.startsWith("rename")) {
            GitRename rename;
            if (line.contains("{")) {
                rename = parseWithBraces(line);
                if (rename != null) {
                    return rename;
                }
            } else {
                rename = parseWithoutBraces(line);
                if (rename != null) {
                    return rename;
                }
            }
        }
        throw new Exception("Unable to parse line: " + line);
    }

    private static GitRename parseWithoutBraces(String line) {
        Matcher matcher = RENAME_WITHOUT_BRACKETS.matcher(line);

        if (matcher.find()) {
            return new GitRename(matcher.group(1), matcher.group(2), Integer.valueOf(matcher.group(3)));
        }
        return null;
    }

    private static GitRename parseWithBraces(String line) throws Exception {
        Matcher matcher = RENAME_WITH_BRACKETS.matcher(line);
        if (matcher.find()) {
            // Handle edge case where the entry is like 'a/b/{ => d}/C.java', or the other way around
            String oldfourthGroup = matcher.group(2).isEmpty() ? matcher.group(4).substring(1) : matcher.group(4);
            String newfourthGroup = matcher.group(3).isEmpty() ? matcher.group(4).substring(1) : matcher.group(4);

            // Taking respectively the left and right arguments in the braces for old and new file.
            String oldFile = matcher.group(1) + matcher.group(2) + oldfourthGroup;
            String newFile = matcher.group(1) + matcher.group(3) + newfourthGroup;
            return new GitRename(oldFile, newFile, Integer.valueOf(matcher.group(5)));
        }
        return null;
    }

    @Override
    public String toString() {
        return "GitRename{" +
                "oldFile='" + oldFile + '\'' +
                ", newFile='" + newFile + '\'' +
                ", similarity=" + similarity +
                '}';
    }
}
