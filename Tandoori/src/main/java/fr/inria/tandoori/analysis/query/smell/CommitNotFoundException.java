package fr.inria.tandoori.analysis.query.smell;

class CommitNotFoundException extends Throwable {
    CommitNotFoundException(int projectId, int ordinal) {
        super("[" + projectId + "] ==> Unable to fetch commit n°: " + ordinal);
    }
}
