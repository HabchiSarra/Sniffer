package fr.inria.tandoori.analysis.query.smell;

class BranchNotFoundException extends Throwable {
    BranchNotFoundException(int projectId, String sha) {
        super("[" + projectId + "] ==> Unable to find branch for commit: " + sha);
    }
}
