package fr.inria.tandoori.analysis.query;

public interface Query {

    /**
     * Execute the query.
     */
    void query() throws QueryException;
}
