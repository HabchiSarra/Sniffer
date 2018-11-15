package fr.inria.sniffer.tracker.analysis.query.commit;

import fr.inria.sniffer.detector.neo4j.CommitSizeQuery;
import fr.inria.sniffer.tracker.analysis.query.Query;
import fr.inria.sniffer.tracker.analysis.query.QueryException;
import fr.inria.sniffer.tracker.analysis.persistence.Persistence;
import fr.inria.sniffer.tracker.analysis.persistence.queries.CommitQueries;
import fr.inria.sniffer.detector.neo4j.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SizeQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(SizeQuery.class.getName());

    private final int appId;
    private final String paprikaDB;
    private final Persistence persistence;
    private CommitQueries commitQueries;

    private final static String TMP_DIR = System.getProperty("java.io.tmpdir");

    public SizeQuery(int appId, String paprikaDB, Persistence persistence, CommitQueries commitQueries) {
        this.appId = appId;
        this.paprikaDB = paprikaDB;
        this.persistence = persistence;
        this.commitQueries = commitQueries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + appId + "] Starting Size insertion");
        String file = csvFilePath();
        logger.debug("[" + appId + "] Using temporary file: " + file);
        String table = tmpTableName();
        logger.debug("[" + appId + "] Using temporary table: " + table);

        generateCsv();

        persistence.execute(addCommitEntryColumn("number_of_classes"));
        persistence.execute(addCommitEntryColumn("number_of_methods"));
        persistence.execute(addCommitEntryColumn("number_of_views"));
        persistence.execute(addCommitEntryColumn("number_of_activities"));
        persistence.execute(addCommitEntryColumn("number_of_inner_classes"));
        persistence.execute(createTmpTable(table));
        long affectedRows = persistence.copyFile(file, table, fetchHeader(file));
        if (affectedRows <= 0) {
            throw new QueryException(logger.getName(), "[" + appId + "] No data copied to temp table");
        }
        persistence.execute(commitQueries.updateCommitSizeQuery(appId, table));

        try {
            Files.delete(Paths.get(file));
        } catch (IOException e) {
            logger.warn("Unable to remove csv file", e);
        }
    }

    /**
     *
     * @param file
     * @return
     */
    private String fetchHeader(String file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            return br.readLine();
        } catch (IOException e) {
            logger.warn("[" + appId + "] Unable to read first line of file: " + file);
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.warn("[" + appId + "] Unable to close reader for file: " + file);
                }
            }
        }
    }

    private void generateCsv() throws QueryException {
        QueryEngine engine = new QueryEngine(paprikaDB);
        engine.setCsvPrefix(csvFilePrefix());
        generateCommitSize(engine);
        engine.shutDown();
    }

    private static String addCommitEntryColumn(String columnName) {
        return "ALTER TABLE commit_entry ADD COLUMN IF NOT EXISTS " + columnName + " INTEGER";
    }

    private String csvFilePrefix() {
        return Paths.get(TMP_DIR, String.valueOf(appId)).toString();
    }

    private String csvFilePath() {
        return csvFilePrefix() + "_CommitSizeQuery.csv";
    }

    private static void generateCommitSize(QueryEngine engine) throws QueryException {
        try {
            //the output file will be in the current folder with the name _CommitSizeQuery.csv
            CommitSizeQuery.createCommitSize(engine).execute(false);
        } catch (IOException ioException) {
            throw new QueryException(logger.getName(), ioException);
        }

    }

    private String createTmpTable(String name) {
        return "CREATE TEMP TABLE " + name + " (" +
                "sha1 TEXT, " +
                "number_of_classes INT, " +
                "number_of_methods INT, " +
                "number_of_interfaces INT, " +
                "number_of_broadcast_receivers INT, " +
                "number_of_services INT, " +
                "number_of_content_providers INT, " +
                "number_of_activities INT, " +
                "number_of_variables INT, " +
                "number_of_inner_classes INT, " +
                "number_of_async_tasks INT, " +
                "number_of_views INT" +
                ");";
    }

    private String tmpTableName() {
        return "tmp_size_" + appId;
    }

}
