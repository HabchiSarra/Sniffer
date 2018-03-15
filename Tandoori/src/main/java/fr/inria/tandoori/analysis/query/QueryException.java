package fr.inria.tandoori.analysis.query;

public class QueryException extends Exception {
    public QueryException(String queryName, Throwable cause) {
        super("Something got wrong during query ("+queryName+") execution!", cause);
    }
}
