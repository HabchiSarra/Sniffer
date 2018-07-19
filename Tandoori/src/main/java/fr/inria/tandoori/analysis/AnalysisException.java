package fr.inria.tandoori.analysis;

public class AnalysisException extends Throwable {
    public AnalysisException(String message, Throwable e) {
        super(message, e);
    }
}
