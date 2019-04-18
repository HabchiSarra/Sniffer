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
package fr.inria.sniffer.tracker.analysis.query.commit;

import fr.inria.sniffer.tracker.analysis.model.CommitDetails;
import fr.inria.sniffer.tracker.analysis.model.GitChangedFile;
import fr.inria.sniffer.tracker.analysis.model.GitDiff;
import fr.inria.sniffer.tracker.analysis.model.GitRename;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieve all details of a commit using a runtime execution of the local Git program.
 * <p>
 * Version 2.13 is required, since we use the '-C' parameter.
 * <p>
 * This class will build a {@link CommitDetails} class, holding the fetched data.
 */
class CommitDetailsChecker {
    private static final Logger logger = LoggerFactory.getLogger(CommitDetails.class.getName());
    private final String repository;

    CommitDetailsChecker(String repository) {
        this.repository = repository;
    }

    public CommitDetails fetch(String sha1) {
        List<GitRename> renames = new ArrayList<>();
        List<GitChangedFile> changedFiles = new ArrayList<>();
        GitDiff diff = GitDiff.EMPTY;

        List<String> lines = GitExecution.commitSummary(repository, sha1);
        for (String line : lines) {
            try {
                renames.add(GitRename.parseRenamed(line));
            } catch (Exception e) {
                // This is an expected behavior
                logger.trace("[Rename] " + e.getMessage(), e);
            }

            try {
                diff = GitDiff.parse(line);
            } catch (Exception e) {
                // This is an expected behavior
                logger.trace("[Diff] " + e.getMessage(), e);
            }

            try {
                changedFiles.add(GitChangedFile.parseFileChange(line));
            } catch (Exception e) {
                // This is an expected behavior
                logger.trace("[FileChanged] " + e.getMessage(), e);
            }

        }
        return new CommitDetails(diff, renames, changedFiles);
    }
}
