package fr.inria.tandoori.analysis.query.smell;

public class CommitNotFoundException extends Throwable {
    public CommitNotFoundException(int projectId, int ordinal) {
        super("[" + projectId + "] ==> Unable to fetch commit n°: " + ordinal);
    }
}
