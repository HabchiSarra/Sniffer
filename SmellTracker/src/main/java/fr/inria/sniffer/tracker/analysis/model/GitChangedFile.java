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
