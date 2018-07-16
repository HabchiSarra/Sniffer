package fr.inria.tandoori.analysis.query.smell;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a commit.
 */
public class Commit {

    public static final Commit EMPTY = new Commit("", 0);
    public final String sha;
    public final int ordinal;

    public Commit(String sha, int ordinal) {
        this.sha = sha;
        this.ordinal = ordinal;
    }


    public static Commit fromInstance(Map<String, Object> smell) {
        String sha1 = (String) smell.get("key");
        Integer ordinal = (Integer) smell.get("commit_number");
        return new Commit(sha1, ordinal);
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

    @Override
    public String toString() {
        return "Commit{" +
                "sha='" + sha + '\'' +
                ", ordinal=" + ordinal +
                '}';
    }
}
