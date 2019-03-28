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

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    public final GitDiff diff;
    public final List<GitRename> renames;
    public final List<GitChangedFile> changedFiles;

    public CommitDetails(GitDiff diff, List<GitRename> renames, List<GitChangedFile> changedFiles) {
        this.diff = diff;
        this.renames = renames;
        this.changedFiles = changedFiles;
    }

    public CommitDetails(GitDiff diff, List<GitRename> renames) {
        this(diff, renames, new ArrayList<>());
    }

    @Override
    public String toString() {
        return "CommitDetails{" +
                "diff=" + diff +
                ", renames=" + renames +
                '}';
    }
}
