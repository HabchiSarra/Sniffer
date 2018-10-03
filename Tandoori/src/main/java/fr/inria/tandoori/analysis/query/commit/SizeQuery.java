package fr.inria.tandoori.analysis.query.commit;

import fr.inria.tandoori.analysis.persistence.Persistence;
import fr.inria.tandoori.analysis.persistence.queries.CommitQueries;
import fr.inria.tandoori.analysis.query.Query;
import fr.inria.tandoori.analysis.query.QueryException;
import neo4j.CommitSizeQuery;
import neo4j.QueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class SizeQuery implements Query {
    private static final Logger logger = LoggerFactory.getLogger(CommitsQuery.class.getName());
    private static final String TMP_TABLE = "tmp_x";
    private static final String CREATE_TMP_TABLE = "CREATE TEMP TABLE " + TMP_TABLE +
            " (sha1 text, number_of_classes int, number_of_methods int );";
    private static final String DROP_TMP_TABLE = "DROP TABLE " + TMP_TABLE;

    private final int projectId;
    private final String paprikaDB;
    private final Persistence persistence;
    private CommitQueries commitQueries;

    private final static String TMP_DIR = System.getProperty("java.io.tmpdir");

    public SizeQuery(int projectId, String paprikaDB, Persistence persistence, CommitQueries commitQueries) {
        this.projectId = projectId;
        this.paprikaDB = paprikaDB;
        this.persistence = persistence;
        this.commitQueries = commitQueries;
    }

    @Override
    public void query() throws QueryException {
        logger.info("[" + projectId + "] Starting Size insertion ");


        QueryEngine engine = new QueryEngine(paprikaDB);

        engine.setCsvPrefix(csvFilePrefix());
        generateCommitSize(engine);
        engine.shutDown();

        persistence.execute(addCommitEntryColumn("number_of_classes"));
        persistence.execute(addCommitEntryColumn("number_of_methods"));
        persistence.execute(CREATE_TMP_TABLE);
        persistence.copyFile(csvFilePath(), TMP_TABLE);
        persistence.execute(commitQueries.updateCommitSizeQuery(projectId, TMP_TABLE));
        persistence.execute(DROP_TMP_TABLE);
    }

    private static String addCommitEntryColumn(String columnName) {
        return "ALTER TABLE commit_entry ADD COLUMN IF NOT EXISTS " + columnName + " INTEGER";
    }

    private String csvFilePrefix() {
        return Paths.get(TMP_DIR, String.valueOf(projectId)).toString();
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


}
