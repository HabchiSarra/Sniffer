package fr.inria.tandoori.analysis.model;

import java.util.ArrayList;
import java.util.List;

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

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public boolean contains(Commit commit) {
        return commits.contains(commit);
    }

    public int getOrdinal() {
        return ordinal;
    }
}
