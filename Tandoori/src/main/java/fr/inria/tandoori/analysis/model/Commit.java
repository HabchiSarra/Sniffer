package fr.inria.tandoori.analysis.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a commit.
 */
public class Commit {

    public static final Commit EMPTY = new Commit("", -1);
    public final String sha;
    public final int ordinal;
    public final List<Commit> parents;

    public Commit(String sha, int ordinal) {
        this.sha = sha;
        this.ordinal = ordinal;
        this.parents = new ArrayList<>();
    }

    public Commit(String sha, int ordinal, List<Commit> parents) {
        this.sha = sha;
        this.ordinal = ordinal;
        this.parents = parents;
    }

    public static Commit fromInstance(Map<String, Object> smell) {
        String sha1 = (String) smell.get("key");
        Integer ordinal = (Integer) smell.get("commit_number");
        return new Commit(sha1, ordinal);
    }

    public static Commit fromRevCommit(RevCommit revCommit) {
        // JGit only returns 1 level of parent commits.
        List<Commit> parents = new ArrayList<>();
        if (revCommit.getParents() != null) {
            for (RevCommit commit : revCommit.getParents()) {
                parents.add(Commit.fromRevCommit(commit));
            }
        }
        return new Commit(revCommit.name(), -1, parents);
    }

    public boolean hasGap(Commit other) {
        return Math.abs(other.ordinal - this.ordinal) > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return ordinal == commit.ordinal &&
                Objects.equals(sha, commit.sha);
    }

    @Override
    public int hashCode() {

        return Objects.hash(sha, ordinal);
    }

    public int getParentCount() {
        return parents == null ? 0 : parents.size();
    }

    public Commit getParent(int nth) {
        return parents == null ? null : parents.get(nth);
    }

    @Override
    public String toString() {
        return "Commit{" +
                "sha='" + sha + '\'' +
                ", ordinal=" + ordinal +
                '}';
    }
}
