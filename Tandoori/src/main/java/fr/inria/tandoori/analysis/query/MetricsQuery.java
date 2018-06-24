package fr.inria.tandoori.analysis.query;

import fr.inria.tandoori.analysis.persistence.Persistence;
import org.tandoori.metrics.calculator.SmellsCalculator;

import java.io.File;

/**
 * Count smells introduces, refactored and deleted for each commits.
 */
public class MetricsQuery implements Query {
    private final Persistence persistence;

    public MetricsQuery(Persistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public void query() {
        SmellsCalculator calculator = new SmellsCalculator();
        calculator.commitFile = new File("");
        calculator.dbPath = new File("");
        calculator.inputDir = new File("");
        // TODO: ...
        calculator.generateReport();
    }
}
