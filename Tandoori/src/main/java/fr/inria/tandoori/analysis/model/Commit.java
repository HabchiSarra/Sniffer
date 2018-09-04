package fr.inria.tandoori.analysis.model;

import org.eclipse.jgit.revwalk.RevCommit;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a commit.
 */
public class Commit {
    public final String sha;
    public int ordinal;
    public final List<Commit> parents;
    public final String message;
    public final DateTime date;
    public final String authorEmail;

    private final List<Smell> smells;
    private final List<Smell> mergedSmells;
    private final Map<Smell, Smell> renamedSmells;

    /**
     * Create a new, empty commit with an empty sha and an invalid ordinal.
     *
     * @return A new {@link Commit}.
     */
    public static Commit empty() {
        return new Commit("", -1);
    }

    /**
     * Minimal constructor for a commit, ordering it using an ordinal.
     *
     * @param sha     The commit sha1.
     * @param ordinal The commit ordinal.
     */
    public Commit(String sha, int ordinal) {
        this(sha, ordinal, new ArrayList<>());
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
        this.smells = new ArrayList<>();
        this.renamedSmells = new HashMap<>();
        this.mergedSmells = new ArrayList<>();
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

    /**
     * Tells if the commit is a merge commit.
     *
     * @return true in case of a merge commit, false otherwise.
     */
    public boolean isMerge() {
        return this.mergedSmells != null && !this.mergedSmells.isEmpty();
    }

    public void addSmell(Smell smell) {
        addSmells(Collections.singleton(smell));
    }

    public void addSmells(Collection<Smell> smells) {
        this.smells.addAll(smells);
    }

    public Collection<Smell> getSmells() {
        return this.smells;
    }

    public void addMergedSmell(Smell smell) {
        addSmells(Collections.singleton(smell));
    }

    public void addMergedSmells(Collection<Smell> smells) {
        this.mergedSmells.addAll(smells);
    }

    public Collection<Smell> getMergedSmells() {
        return mergedSmells;
    }

    public void setRenamedSmell(Smell origin, Smell renamed) {
        this.renamedSmells.put(origin, renamed);
    }

    public Collection<Smell> getRenamedSmellsOrigins() {
        return this.renamedSmells.keySet();
    }

    public Collection<Smell> getRenamedSmells() {
        return this.renamedSmells.values();
    }

    /**
     * Retrieve the list of introduced commits from the list of smell presence of the previous and current commit.
     * <p>
     * In case of a merge commit, we remove all existing {@link Smell}s from both branches.
     *
     * @param previous The previous {@link Commit}.
     * @return The list of {@link Smell} introduced in the current commit.
     */
    public List<Smell> getIntroduced(Commit previous) {
        List<Smell> introduction = new ArrayList<>(this.getSmells());
        introduction.removeAll(previous.getSmells());
        if (this.isMerge()) {
            introduction.removeAll(this.getMergedSmells());
        }
        introduction.removeAll(this.getRenamedSmells());
        return introduction;
    }

    /**
     * Retrieve the list of refactored commits from the list of smell presence of the previous and current commit.
     * <p>
     * In case of a merge commit, we keep the intersection of the {@link Smell}s coming from both branches
     * before removing all current commit's smells.
     *
     * @param previous The previous {@link Commit}.
     * @return The list of {@link Smell} refactored in the current commit.
     */
    public List<Smell> getRefactored(Commit previous) {
        List<Smell> refactoring = new ArrayList<>(previous.getSmells());
        if (this.isMerge()) {
            refactoring.retainAll(this.getMergedSmells());
        }

        refactoring.removeAll(this.getSmells());
        refactoring.removeAll(this.getRenamedSmellsOrigins());
        return refactoring;
    }
}
