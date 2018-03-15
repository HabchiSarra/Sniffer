package fr.inria.tandoori.analysis.query;

import neo4j.HashMapUsageQuery;
import neo4j.IGSQuery;
import neo4j.InitOnDrawQuery;
import neo4j.InvalidateWithoutRectQuery;
import neo4j.LICQuery;
import neo4j.MIMQuery;
import neo4j.NLMRQuery;
import neo4j.NoSmellsQuery;
import neo4j.OverdrawQuery;
import neo4j.QueryEngine;
import neo4j.UnsuitedLRUCacheSizeQuery;
import neo4j.UnsupportedHardwareAccelerationQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class SmellQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SmellQuery.class.getName());
    private final String db;

    public SmellQuery(String db) {
        this.db = db;
    }

    @Override
    public void query() {
        boolean showDetails = true;
        QueryEngine queryEngine = new QueryEngine(db);

        // TODO: Enable to write results in a database
        Calendar cal = new GregorianCalendar();
        String csvDate = String.valueOf(cal.get(Calendar.YEAR)) + "_" + String.valueOf(cal.get(Calendar.MONTH) + 1) + "_" + String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + "_" + String.valueOf(cal.get(Calendar.HOUR_OF_DAY)) + "_" + String.valueOf(cal.get(Calendar.MINUTE));
        String csvPrefix = "paprikaSmells-" + csvDate;
        logger.debug("Resulting csv file name will start with prefix " + csvPrefix);

        try {
            IGSQuery.createIGSQuery(queryEngine).execute(showDetails);
            MIMQuery.createMIMQuery(queryEngine).execute(showDetails);
            LICQuery.createLICQuery(queryEngine).execute(showDetails);
            NLMRQuery.createNLMRQuery(queryEngine).execute(showDetails);
            OverdrawQuery.createOverdrawQuery(queryEngine).execute(showDetails);
            UnsuitedLRUCacheSizeQuery.createUnsuitedLRUCacheSizeQuery(queryEngine).execute(showDetails);
            InitOnDrawQuery.createInitOnDrawQuery(queryEngine).execute(showDetails);
            UnsupportedHardwareAccelerationQuery.createUnsupportedHardwareAccelerationQuery(queryEngine).execute(showDetails);
            HashMapUsageQuery.createHashMapUsageQuery(queryEngine).execute(showDetails);
            InvalidateWithoutRectQuery.createInvalidateWithoutRectQuery(queryEngine).execute(showDetails);
            new NoSmellsQuery(queryEngine).execute(showDetails);
        } catch (IOException e) {
            logger.error("An exception occurred while looking for smells", e);
        }
    }
}
