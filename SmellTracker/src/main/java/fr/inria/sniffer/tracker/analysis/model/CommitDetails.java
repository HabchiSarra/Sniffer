package fr.inria.sniffer.tracker.analysis.model;

import java.util.List;

public class CommitDetails {
    public final GitDiff diff;
    public final List<GitRename> renames;

    public CommitDetails(GitDiff diff, List<GitRename> renames) {
        this.diff = diff;
        this.renames = renames;
    }

    @Override
    public String toString() {
        return "CommitDetails{" +
                "diff=" + diff +
                ", renames=" + renames +
                '}';
    }
}
