package fr.inria.sniffer.tracker.analysis.model;

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    public final GitDiff diff;
    public final List<GitRename> renames;
    public final List<GitChangedFile> changedFiles;

    public CommitDetails(GitDiff diff, List<GitRename> renames, List<GitChangedFile> changedFiles) {
        this.diff = diff;
        this.renames = renames;
        this.changedFiles = changedFiles;
    }

    public CommitDetails(GitDiff diff, List<GitRename> renames) {
        this(diff, renames, new ArrayList<>());
    }

    @Override
    public String toString() {
        return "CommitDetails{" +
                "diff=" + diff +
                ", renames=" + renames +
                '}';
    }
}
