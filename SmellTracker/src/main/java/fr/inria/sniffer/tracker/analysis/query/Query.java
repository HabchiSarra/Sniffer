package fr.inria.sniffer.tracker.analysis.query;

public interface Query {

    /**
     * Execute the query.
     */
    void query() throws QueryException;
}
