package fr.inria.tandoori.analysis.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class Branch {
    private final List<RevCommit> commits;
    private boolean isMaster;
    private final int ordinal;

    public static Branch fromMother(Branch mother) {
        if (mother == null) {
            return new Branch(0, true);
        } else {
            return new Branch(mother.getOrdinal() + 1, false);
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

    public void addCommit(RevCommit commit) {
        this.commits.add(commit);
    }

    public List<RevCommit> getCommits() {
        return commits;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public boolean contains(RevCommit parent) {
        return false;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
