package fr.inria.tandoori.analysis.writer;

import fr.inria.tandoori.analysis.persistence.Persistence;

public interface Writer {

    /**
     *
     * @param persistence
     */
    void writeResults(Persistence persistence);
}
