package fr.inria.tandoori.analysis.persistence.queries;

import fr.inria.tandoori.analysis.model.Smell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JDBCQueriesHelper {
    protected static final Logger logger = LoggerFactory.getLogger(JDBCQueriesHelper.class.getName());

    /**
     * Escape the string to be compatible with double dollar String insertion.
     *
     * @param entry The string to escape.
     * @return The string with every occurences of "$$" replaced by "$'$".
     */
    static String escapeStringEntry(String entry) {
        return entry.replace("$$", "$'$");
    }
}
