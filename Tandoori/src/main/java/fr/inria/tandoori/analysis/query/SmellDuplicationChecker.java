package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SmellDuplicationChecker {
    private static final Logger logger = LoggerFactory.getLogger(SmellQuery.class.getName());
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
        return "select  subquery.sha1 as sha1, FileRename.oldFile as oldFile, FileRename.newFile as newFile from FileRename \n" +
                "Inner join (select CommitEntry.sha1 as sha1, CommitEntry.id as commitEntryId from CommitEntry ) AS subquery \n" +
                "On FileRename.commitId= subquery.commitEntryId " +
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
        logger.trace("==> Trying to guess original smell for: " + instance);

        // If the smell is already a known renaming, return it instantly
        Smell mergedSmell = renamedSmells.get(instance);
        if (mergedSmell != null) {
            logger.trace("  ==> Found an already guessed smell");
            return mergedSmell;
        }

        // If we find a renaming of the smell file in this specific commit, try to guess the original smell.
        int index = fileRenamings.indexOf(FileRenameEntry.fromSmell(instance));
        if (index > -1) {
            logger.trace("  ==> Guessed new original smell!");
            return guessOriginalSmell(instance, fileRenamings.get(index));
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
     * @param renaming Matching renaming entry.
     * @return The guessed original smell.
     */
    private Smell guessOriginalSmell(Smell instance, FileRenameEntry renaming) {
        String guessOldInstance = guessInstanceName(instance.instance, renaming.newFile);
        Smell original = new Smell(instance.type, instance.commitSha, guessOldInstance, renaming.oldFile);

        // Cache the original smell into renamedSmells to find it for the next instances.
        renamedSmells.put(instance, original);
        return original;
    }

    /**
     * @param instance identifier of the current smell instance.
     * @param newFile  New smell file.
     * @return Replace the current smell instance by an instance guessed from the file name.
     */
    private String guessInstanceName(String instance, String newFile) {
        String packagePath = extractPackageIdentifier(instance);
        String ending = extractIdentifierEnding(instance);
        String start = extractIdentifierStart(instance);

        String newPackagePath = transformPackageIdentifier(packagePath, newFile);

        return start + newPackagePath + ending;
    }

    /**
     * Replace the package path content with the newFile path.
     *
     * We iterate from the end of the packagePath (i.e. the class name) to its start
     * to only use the usefull part of the newFile path.
     *
     * @param packagePath Package identifier to replace.
     * @param newFile File containing the new package ID to set.
     * @return The newly created package identifier of the original smell.
     */
    private String transformPackageIdentifier(String packagePath, String newFile) {
        List<String> packageParts = Arrays.asList(packagePath.split("\\."));
        // Remove .java extension from file before using it as identifier
        newFile = newFile.split("\\.java$")[0];
        String[] pathPart = newFile.split("/");
        for (int i = packageParts.size() - 1; i >= 0; i--) {
            packageParts.set(i, pathPart[i]);
        }
        return String.join(".", packageParts);
    }

    /**
     * We cut an retrieve the package id of our instance smell.
     *
     * @param instance Instance to parse.
     * @return The package id and classname part of the smell identifier.
     */
    private String extractPackageIdentifier(String instance) {
        String[] split = instance.split("[$#]");
        if (instance.contains("#") && split.length >= 2) {
            return split[1];
        }
        return split[0];
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
