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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiff {
    private static final Logger logger = LoggerFactory.getLogger(GitDiff.class.getName());
    public static final GitDiff EMPTY = new GitDiff(0, 0, 0); // TODO: See if we set to -1 ?
    private static final Pattern MODIF_PATTERN = Pattern.compile("(?<number>\\d+)\\s(insertion[s]?|deletion[s]?)\\((?<sign>[+-])\\)");
    private static final Pattern FILE_PATTERN = Pattern.compile("(?<number>\\d+)\\sfile[s]?\\schanged");

    private final int addition;
    private final int deletion;
    private final int changedFiles;

    public GitDiff(int addition, int deletion, int changedFiles) {
        this.addition = addition;
        this.deletion = deletion;
        this.changedFiles = changedFiles;
    }

    /**
     * Syntax example:
     * 3 files changed, 65 insertions(+), 5 deletions(-)
     *
     * @param line Line to parse.
     * @return found {@link GitDiff}, null if could not parse.
     */
    public static GitDiff parse(String line) throws Exception {
        line = line.trim();
        String[] split = line.split(",");
        Map<String, Integer> modifications = new HashMap<>();

        Matcher matcher = FILE_PATTERN.matcher(split[0]);
        if (!matcher.find()) {
            throw new Exception("Unable to parse diff line: " + line);
        }
        int changedFiles = Integer.valueOf(matcher.group("number"));

        // We should be greater than 1
        String input;
        if (split.length > 1) {
            input = split[1];
            matcher = MODIF_PATTERN.matcher(input);
            if (matcher.find()) {
                modifications.put(matcher.group("sign"), Integer.valueOf(matcher.group("number")));
            }
        }
        if (split.length > 2) {
            input = split[2];
            matcher = MODIF_PATTERN.matcher(input);
            if (matcher.find()) {
                modifications.put(matcher.group("sign"), Integer.valueOf(matcher.group("number")));
            }
        }

        int addition = modifications.getOrDefault("+", 0);
        int deletion = modifications.getOrDefault("-", 0);
        return new GitDiff(addition, deletion, changedFiles);
    }

    public int getAddition() {
        return addition;
    }

    public int getDeletion() {
        return deletion;
    }

    public int getChangedFiles() {
        return changedFiles;
    }

    @Override
    public String toString() {
        return "GitDiff{" +
                "addition=" + addition +
                ", deletion=" + deletion +
                ", changedFiles=" + changedFiles +
                '}';
    }
}
