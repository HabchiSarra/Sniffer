package fr.inria.tandoori.analysis.query.smell;

public class BranchNotFoundException extends Throwable {
    public BranchNotFoundException(int projectId, String sha) {
        super("[" + projectId + "] ==> Unable to find branch for commit: " + sha);
    }
}
