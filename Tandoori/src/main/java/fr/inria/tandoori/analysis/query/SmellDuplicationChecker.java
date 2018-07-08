package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmellDuplicationChecker {
    private final List<FileRenameEntry> fileRenamings;
    /**
     * Binding renamed smells to their original ones.
     */
    private final Map<Smell, Smell> renamedSmells;

    SmellDuplicationChecker(int projectId, Persistence persistence) {
        fileRenamings = loadFileRename(projectId, persistence);
        renamedSmells = new HashMap<>();
    }

    private static String getFileRenameStatement(int projectId) {
        return "select  subquery.sha1, FileRename.oldFile as oldFile, FileRename.newFile as newFile from FileRename \n" +
                "Inner join (select CommitEntry.sha1 as sha1, CommitEntry.id as commitEntryId from CommitEntry ) AS subquery \n" +
                "On FileRename.commitId= 'subquery.commitEntryId'" +
                "WHERE FileRename.projectId = '" + projectId + "'";
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
     * @return The original instance, null if not a renamed instance.
     */
    public Smell original(Smell instance) {
        // If the smell is already a known renaming, return it instantly
        Smell mergedSmell = renamedSmells.get(instance);
        if (mergedSmell != null) {
            return mergedSmell;
        }

        // If we find a renaming of the smell file in this specific commit, try to guess the original smell.
        int index = fileRenamings.indexOf(FileRenameEntry.fromSmell(instance));
        if (index > -1) {
            return guessOriginalSmell(instance, fileRenamings.get(index));
        }

        return null;
    }

    private Smell guessOriginalSmell(Smell instance, FileRenameEntry renaming) {
        String guessOldInstance = guessInstanceName(instance.instance, renaming.newFile, renaming.oldFile);
        Smell original = new Smell(instance.type, instance.commitSha, guessOldInstance, instance.file);

        // Cache the original smell into renamedSmells to find it for the next instances.
        renamedSmells.put(instance, original);
        return original;
    }

    /**
     * @param instance identifier of the current smell instance.
     * @param newFile  New smell file.
     * @param oldFile  Old smell file.
     * @return Replace the current smell instance by an instance guessed from the file name.
     */
    private String guessInstanceName(String instance, String newFile, String oldFile) {
        String packagePath = instance.split("[$#]")[0];
        String ending = extractIdentifierEnding(instance);
        List<String> packageParts = Arrays.asList(packagePath.split("."));

        for (String pathPart : newFile.split("/")) {
            int index = packageParts.indexOf(pathPart);
            if (index > -1) {
                packageParts.set(index, pathPart);
            }
        }

        String newInstance = String.join(".", packageParts);
        return newInstance + ending;
    }

    private String extractIdentifierEnding(String instance) {
        String[] innerClasses = instance.split("$");
        String result = "$" + String.join("$", innerClasses);

        String[] methodSplit = instance.split("#");
        if (methodSplit.length > 1) {
            result = result + "#" + methodSplit[1];
        }
        return result;
    }

    /**
     * This class is used for determining if a smell is a renamed version of another smell.
     */
    private static class BoundSmell {
        private final String identifier;
        private final String file;

        BoundSmell(String identifier, String file) {
            this.identifier = identifier;
            this.file = file;
        }

        static BoundSmell fromSmell(Map<String, Object> smell) {
            String identifier = (String) smell.get("instance");
            String file = (String) smell.get("filePath");
            return new BoundSmell(identifier, file);

        }
    }

    /**
     * This class is used to determine if a file has been renamed in a specific commit.
     */
    private static class FileRenameEntry {
        private final String sha1;
        private final String oldFile;
        private final String newFile;

        private FileRenameEntry(String sha1, String oldFile, String newFile) {
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
        static FileRenameEntry fromSmell(Smell smell) {
            return new FileRenameEntry(smell.commitSha, "", smell.file);
        }

        /**
         * Create a new {@link FileRenameEntry} from a database entry.
         *
         * @param renameEntry The database entry to create a {@link FileRenameEntry} for.
         * @return The created {@link FileRenameEntry}
         */
        static FileRenameEntry fromDBEntry(Map<String, Object> renameEntry) {
            String sha1 = (String) renameEntry.get("sha1");
            String oldFile = (String) renameEntry.get("oldFile");
            String newFile = (String) renameEntry.get("newFile");
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
    }
}
