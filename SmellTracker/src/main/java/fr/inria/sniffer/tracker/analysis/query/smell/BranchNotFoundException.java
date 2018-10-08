package fr.inria.sniffer.tracker.analysis.query.smell;

class BranchNotFoundException extends Throwable {
    BranchNotFoundException(int projectId, String sha) {
        super("[" + projectId + "] ==> Unable to find branch for commit: " + sha);
    }
}
