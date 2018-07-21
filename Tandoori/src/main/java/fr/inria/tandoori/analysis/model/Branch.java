package fr.inria.tandoori.analysis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a branch in a Git repository.
 */
public class Branch {
    private final List<Commit> commits;
    private boolean isMaster;
    private final int ordinal;

    public static Branch fromMother(Branch mother, int ordinal) {
        if (mother == null) {
            return new Branch(ordinal, true);
        } else {
            return new Branch(ordinal, false);
        }
    }

    public Branch() {
        this(-1, false);
    }

    public Branch(int ordinal, boolean isMaster) {
        this.commits = new ArrayList<>();
        this.ordinal = ordinal;
        this.isMaster = isMaster;
    }

    /**
     * Add a commit to the current branch.
     *
     * @param commit The commit to add.
     */
    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Specify if the branch is the repository's principal branch.
     *
     * @return True if the branch is the master branch (principal), false otherwise.
     */
    public boolean isMaster() {
        return isMaster;
    }

    /**
     * Determine if the branch contains the given commit.
     *
     * @param commit The commit to check.
     * @return True if is contained, false otherwise.
     */
    public boolean contains(Commit commit) {
        return commits.contains(commit);
    }

    /**
     * Return the number identifier of the branch for the project.
     *
     * @return A number unique for each project.
     */
    public int getOrdinal() {
        return ordinal;
    }
}
