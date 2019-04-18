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
package fr.inria.sniffer.tracker.analysis.query.smell.duplication;

import fr.inria.sniffer.tracker.analysis.model.Commit;
import fr.inria.sniffer.tracker.analysis.model.Smell;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.detector.neo4j.QualifiedNameFromFileQuery;
import fr.inria.sniffer.detector.neo4j.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tries to generate parent {@link Smell} instances from project's files
 * renamings.
 * TODO: Add interface.
 */
public class SmellDuplicationChecker {
    static final String SHA1_COLUMN = "sha1";
    static final String OLD_FILE_COLUMN = "old_file";
    static final String NEW_FILE_COLUMN = "new_file";

    private static final Logger logger = LoggerFactory.getLogger(SmellDuplicationChecker.class.getName());
    public static final String QUALIFIED_NAME = "qualified_name";
    private final List<FileRenameEntry> fileRenamings;
    private final QueryEngine queryEngine;

    public SmellDuplicationChecker(int projectId, Persistence persistence, QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
        fileRenamings = loadFileRename(projectId, persistence);
    }

    /**
     * Generate a query to fetch all file renames for a given project.
     * TODO: Extract to {@link CommitQueries}
     *
     * @param projectId Project identifier.
     * @return The generated statement.
     */
    private static String getFileRenameStatement(int projectId) {
        return "SELECT subquery.sha1 AS " + SHA1_COLUMN
                + ", file_rename.old_file AS " + OLD_FILE_COLUMN
                + ", file_rename.new_file AS " + NEW_FILE_COLUMN
                + " FROM file_rename \n" +
                "INNER JOIN (SELECT commit_entry.sha1 AS sha1, commit_entry.id AS commit_entry_id FROM commit_entry) AS subquery \n" +
                "ON file_rename.commit_id = subquery.commit_entry_id " +
                "WHERE file_rename.project_id = '" + projectId + "'";
    }

    private List<FileRenameEntry> loadFileRename(int projectId, Persistence persistence) {
        List<FileRenameEntry> renaming = new ArrayList<>();
        List<Map<String, Object>> result = persistence.query(getFileRenameStatement(projectId));
        for (Map<String, Object> rename : result) {
            renaming.add(FileRenameEntry.fromDBEntry(rename));
        }
        return renaming;
    }

    /**
     * Retrieve the original smell if it is embedded in a renamed file.
     *
     * @param instance The instance to check.
     * @param commit   The current commit in which the renaming took place.
     * @param previous The previous commit, used for fetching the previous file's class name.
     * @return The original instance, null if not a renamed instance.
     */
    public Smell original(Smell instance, Commit commit, Commit previous) {
        logger.trace("==> Trying to guess original smell for: " + instance);

        // If we find a renaming of the smell file in this specific commit, try to guess the original smell.
        int index = fileRenamings.indexOf(FileRenameEntry.fromSmell(instance, commit));
        if (index > -1) {
            logger.trace("  ==> Guessing new original smell!");
            return guessOriginalSmell(instance, previous, fileRenamings.get(index));
        }

        logger.trace("  ==> No original smell found");

        return null;
    }

    /**
     * In this test we have a smell with its file directing to a newly renamed file.
     * We will have to guess the previous smell instance by rewriting its instance id with the file before being renamed.
     * This instance ID will then be compared with the list of instances from the previous smell to find a match.
     *
     * @param instance Current smell instance name.
     * @param commit   The commit holding the old file.
     * @param renaming Matching renaming entry.
     * @return The guessed original smell.
     */
    private Smell guessOriginalSmell(Smell instance, Commit commit, FileRenameEntry renaming) {
        String oldClass = fetchQualifiedName(commit.sha, renaming.oldFile);
        String ending = extractIdentifierEnding(instance.instance);
        String start = extractIdentifierStart(instance.instance);

        String guessOldInstance = start + oldClass + ending;
        return new Smell(instance.type, guessOldInstance, renaming.oldFile);
    }

    /**
     * Fetch the class fully qualified name from SmellDetector.
     *
     * @param sha  The commit to look into.
     * @param file The file containing the queried class.
     * @return The class fully qualified name if found, an empty String if not.
     */
    private String fetchQualifiedName(String sha, String file) {
        QualifiedNameFromFileQuery query = new QualifiedNameFromFileQuery(queryEngine, sha, "/" + file);
        List<Map<String, Object>> result = query.fetchResult(false);
        if (result.isEmpty() || !result.get(0).containsKey(QUALIFIED_NAME)) {
            logger.warn("Unable to query qualified name on Paprika for file " + file + " on commit " + sha);
            return "";
        }
        return (String) result.get(0).get(QUALIFIED_NAME);

    }


    /**
     * Extract the ending identifier (i.e. the method name) from our smell id:
     * open#eu.chainfire.libsuperuser.Shell$Interactive
     *
     * @param instance Instance to parse.
     * @return The inner class part ending with a #.
     */
    private String extractIdentifierStart(String instance) {
        String result = "";
        String[] methodSplit = instance.split("#");
        if (methodSplit.length > 1) {
            result = methodSplit[0] + "#";
        }
        return result;
    }

    /**
     * Extract the ending identifier (i.e. the inner class) from our smell id:
     * open#eu.chainfire.libsuperuser.Shell$Interactive
     *
     * @param instance Instance to parse.
     * @return The inner class part starting with a $.
     */
    private String extractIdentifierEnding(String instance) {
        StringBuilder result = new StringBuilder();
        String[] innerClasses = instance.split("\\$");
        if (innerClasses.length > 1) {
            for (int i = 1; i < innerClasses.length; i++) {
                result.append("$").append(innerClasses[i]);
            }
        }
        return result.toString();
    }

    /**
     * This class is used to determine if a file has been renamed in a specific commit.
     */
    static class FileRenameEntry {
        final String sha1;
        final String oldFile;
        final String newFile;

        FileRenameEntry(String sha1, String oldFile, String newFile) {
            this.sha1 = sha1;
            this.oldFile = oldFile;
            this.newFile = newFile;
        }

        /**
         * Create a new {@link FileRenameEntry} from a Smell.
         * This entry can be used to compare it with the other entries in the fileRenamings property, containing the old
         * file.
         *
         * @param smell The smell to create an entry for.
         * @return The created {@link FileRenameEntry}
         */
        static FileRenameEntry fromSmell(Smell smell, Commit commit) {
            return new FileRenameEntry(commit.sha, "", smell.file);
        }

        /**
         * Create a new {@link FileRenameEntry} from a database entry.
         *
         * @param renameEntry The database entry to create a {@link FileRenameEntry} for.
         * @return The created {@link FileRenameEntry}
         */
        static FileRenameEntry fromDBEntry(Map<String, Object> renameEntry) {
            // Fields returned  by postgresql are always lowercase!
            String sha1 = (String) renameEntry.get(SHA1_COLUMN);
            String oldFile = (String) renameEntry.get(OLD_FILE_COLUMN);
            String newFile = (String) renameEntry.get(NEW_FILE_COLUMN);
            return new FileRenameEntry(sha1, oldFile, newFile);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileRenameEntry that = (FileRenameEntry) o;
            return Objects.equals(sha1, that.sha1) &&
                    Objects.equals(newFile, that.newFile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sha1, newFile);
        }

        @Override
        public String toString() {
            return "FileRenameEntry{" +
                    "sha1='" + sha1 + '\'' +
                    ", oldFile='" + oldFile + '\'' +
                    ", newFile='" + newFile + '\'' +
                    '}';
        }
    }
}
