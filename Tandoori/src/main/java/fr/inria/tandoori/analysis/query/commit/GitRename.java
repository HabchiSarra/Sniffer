package fr.inria.tandoori.analysis.query.commit;

public final class GitRename {
    public final String oldFile;
    public final String newFile;
    public final int similarity;

    public GitRename(String oldFile, String newFile, int similarity) {
        this.oldFile = oldFile;
        this.newFile = newFile;
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return "GitRename{" +
                "oldFile='" + oldFile + '\'' +
                ", newFile='" + newFile + '\'' +
                ", similarity=" + similarity +
                '}';
    }
}
