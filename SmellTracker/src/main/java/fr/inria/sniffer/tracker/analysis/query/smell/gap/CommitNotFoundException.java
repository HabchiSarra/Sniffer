package fr.inria.sniffer.tracker.analysis.query.smell.gap;

public class CommitNotFoundException extends Throwable {
    private final int ordinal;

    public CommitNotFoundException(int projectId, int ordinal) {
        super("[" + projectId + "] ==> Unable to fetch commit n°: " + ordinal);
        this.ordinal = ordinal;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
