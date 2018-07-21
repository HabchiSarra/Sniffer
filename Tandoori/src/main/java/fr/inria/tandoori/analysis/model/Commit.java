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

    /**
     * Create a {@link Commit} from a Paprika Smell instance.
     *
     * @param smell The smell instance to transform.
     * @return A newly created {@link Commit}.
     */
    public static Commit fromInstance(Map<String, Object> smell) {
        String sha1 = (String) smell.get("key");
        Integer ordinal = (Integer) smell.get("commit_number");
        return new Commit(sha1, ordinal);
    }

    /**
     * Create a commit instance from a JGit {@link RevCommit}.
     * <p>
     * Warning: This creation does not handle ordinal since the information is not available.
     * It will return -1 if used.
     *
     * @param revCommit The commit to transform.
     * @return A newly created {@link Commit}.
     */
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


    /**
     * Tells if the commit is not consecutive with the other commit.
     *
     * @param other The commit to test against this.
     * @return True if the two commits ordinal are separated by more than 1, False otherwise.
     */
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

    /**
     * Returns the number of parent for this commit.
     *
     * @return The number of parent, 0 if no parents are referenced.
     */
    public int getParentCount() {
        return parents == null ? 0 : parents.size();
    }

    /**
     * Return the Nth parent of this commit.
     *
     * @param nth The parent index.
     * @return A {@link Commit}.
     * @throws IndexOutOfBoundsException If the nth argument is > Commit{@link #getParentCount()} - 1
     */
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
