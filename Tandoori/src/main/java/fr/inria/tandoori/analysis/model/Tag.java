package fr.inria.tandoori.analysis.model;


import org.joda.time.DateTime;

public class Tag {
    private final String name;
    private final String sha;
    private DateTime date;

    /**
     * @param name Tag name
     * @param sha  Sha of the object on which this tag is set.
     * @param date Date of the commit linked to the tag.
     */
    public Tag(String name, String sha, DateTime date) {
        this.name = name;
        this.sha = sha;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getSha() {
        return sha;
    }

    public DateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", sha='" + sha + '\'' +
                ", date=" + date +
                '}';
    }
}
