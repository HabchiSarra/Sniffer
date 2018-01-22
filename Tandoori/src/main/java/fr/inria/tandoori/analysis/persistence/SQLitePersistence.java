package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLitePersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private final Connection connection;
    private Statement sqlStatement;
    private String path;

    public SQLitePersistence(String path) {
        this.path = path;
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to open connection to database: " + path, e);
        }
    }

    @Override
    public void addStatements(String... statements) {
        try {
            if (sqlStatement == null) {
                sqlStatement = connection.createStatement();
            }
            for (String statement : statements) {
                sqlStatement.addBatch(statement);
            }
        } catch (SQLException e) {
            logger.error("Unable to create statement for database: " + path, e);
        }
    }

    @Override
    public void commit() {
        try {
            sqlStatement.executeBatch();
            sqlStatement.clearBatch();
        } catch (SQLException e) {
            logger.error("Unable to commit transaction into database: " + path, e);
        } finally {
            closeStatement();
        }
    }


    private void closeStatement() {
        try {
            sqlStatement.close();
        } catch (SQLException e) {
            logger.warn("Unable to close statement from database: " + path, e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Unable to close connection to database: " + path, e);
            }
        }
    }

    @Override
    public void initialize() {

    }
}
