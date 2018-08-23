package fr.inria.tandoori.analysis.model;

import org.eclipse.jgit.revwalk.RevCommit;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a commit.
 */
public class Commit {

    public static final Commit EMPTY = new Commit("", -1);
    public final String sha;
    public int ordinal;
    public final List<Commit> parents;
    public final String message;
    public final DateTime date;
    public final String authorEmail;


    /**
     * Minimal constructor for a commit, ordering it using an ordinal.
     *
     * @param sha     The commit sha1.
     * @param ordinal The commit ordinal.
     */
    public Commit(String sha, int ordinal) {
        this(sha, ordinal, Collections.emptyList());
    }

    /**
     * Constructor building a Commit with an empty date and message.
     *
     * @param sha     The commit sha1.
     * @param ordinal The commit ordinal.
     * @param parents The commit parents.
     */
    public Commit(String sha, int ordinal, List<Commit> parents) {
        this(sha, ordinal, new DateTime(0), "", "", parents);
    }

    /**
     * Full constructor for a {@link Commit}.
     *
     * @param sha         The commit sha1.
     * @param ordinal     The commit ordinal.
     * @param date        The commit date.
     * @param message     The commit message.
     * @param authorEmail The commit author.
     * @param parents     The commit parents.
     */
    public Commit(String sha, int ordinal, DateTime date, String message,
                  String authorEmail, List<Commit> parents) {
        this.sha = sha;
        this.ordinal = ordinal;
        this.date = date;
        this.message = message;
        this.authorEmail = authorEmail;
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
    public static Commit commitWithDetails(RevCommit revCommit) {
        return new Commit(
                revCommit.name(),
                -1,
                new DateTime(((long) revCommit.getCommitTime()) * 1000),
                revCommit.getFullMessage(),
                revCommit.getAuthorIdent().getEmailAddress(),
                new ArrayList<>());
    }

    /**
     * Create a commit instance from a JGit {@link RevCommit}.
     * <p>
     * Warning: This creation does not handle ordinal since the information is not available.
     * It will return -1 if used.
     * <p>
     * By default we will not retrieve the commit message and author
     * to avoid taking too much processing time.
     *
     * @param revCommit The commit to transform.
     * @return A newly created {@link Commit}.
     */
    public static Commit commitWithParents(RevCommit revCommit) {
        // JGit only returns 1 level of parent commits.
        List<Commit> parents = new ArrayList<>();

        if (revCommit.getParents() != null) {
            for (RevCommit commit : revCommit.getParents()) {
                parents.add(Commit.commitWithParents(commit));
            }
        }

        // We do not set the commit detailed information since:
        // 1. It saves processing time and memory
        // 2. We can't call RevCommit#getFullMessage on the parent commits.
        // 2. We can't call RevCommit#getAuthorIdent on the parent commits.
        return new Commit(revCommit.name(), -1, new DateTime(0), "", "", parents);
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

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "sha='" + sha + '\'' +
                ", ordinal=" + ordinal +
                '}';
    }

    public void setParents(Collection<Commit> newParents) {
        parents.clear();
        parents.addAll(newParents);
    }
}
