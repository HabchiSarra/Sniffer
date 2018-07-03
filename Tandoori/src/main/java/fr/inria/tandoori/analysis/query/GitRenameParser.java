package fr.inria.tandoori.analysis.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GitRenameParser {

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
     * @return a {@link RenameParsingResult}
     */
    static RenameParsingResult parseRenamed(String line) throws Exception {
        if (line.contains("{")) {
            Matcher matcher = RENAME_WITH_BRACKETS.matcher(line);

            if (matcher.find()) {
                // Taking respectively the left and right arguments in the braces for old and new file.
                String oldFile = matcher.group(1) + matcher.group(2) + matcher.group(4);
                String newFile = matcher.group(1) + matcher.group(3) + matcher.group(4);
                return new RenameParsingResult(oldFile, newFile, Integer.valueOf(matcher.group(5)));
            }
        } else {
            Matcher matcher = RENAME_WITHOUT_BRACKETS.matcher(line);

            if (matcher.find()) {
                return new RenameParsingResult(matcher.group(1), matcher.group(2), Integer.valueOf(matcher.group(3)));
            }
        }
        throw new Exception("Unable to parse line: " + line);
    }

    static final class RenameParsingResult {
        final String oldFile;
        final String newFile;
        final int similarity;

        private RenameParsingResult(String oldFile, String newFile, int similarity) {
            this.oldFile = oldFile;
            this.newFile = newFile;
            this.similarity = similarity;
        }
    }
}
