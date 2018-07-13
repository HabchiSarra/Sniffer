package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JDBCPersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(JDBCPersistence.class.getName());
    private final Connection connection;
    private Statement sqlStatement;
    private final String path;
    private final String schemaResourcePath;

    public JDBCPersistence(String type, String path, String schemaResourcePath) {
        this.path = path;
        this.schemaResourcePath = schemaResourcePath;
        try {
            this.connection = DriverManager.getConnection("jdbc:" + type + ":" + path);
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            throw new RuntimeException("Unable to open connection to database: " + path, e);
        }
    }

    public JDBCPersistence(String type, String path, String schemaResourcePath, String username, String password) {
        this.path = path;
        this.schemaResourcePath = schemaResourcePath;
        try {
            this.connection = DriverManager.getConnection("jdbc:" + type + ":" + path, username, password);
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            throw new RuntimeException("Unable to open connection to database: " + path, e);
        }
    }

    public JDBCPersistence(Connection connection, String schemaResourcePath) {
        this.connection = connection;
        this.schemaResourcePath = schemaResourcePath;
        this.path = connection.toString();
    }

    @Override
    public void addStatements(String... statements) {
        try {
            if (sqlStatement == null) {
                sqlStatement = connection.createStatement();
            }
            for (String statement : statements) {
                logger.trace("Adding new statement: " + statement);
                sqlStatement.addBatch(statement);
            }
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.error("Unable to create statement for database: " + path, e);
        }
    }

    @Override
    public void commit() {
        logger.info("Committing transaction");
        try {
            sqlStatement.executeBatch();
            sqlStatement.clearBatch();
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.warn("Unable to commit transaction into database: " + path, e);
        } finally {
            closeStatement();
        }
    }

    @Override
    public List<Map<String, Object>> query(String statement) {
        logger.debug("Querying database: " + statement);

        try (Statement queryStatement = connection.createStatement()) {
            ResultSet resultSet = queryStatement.executeQuery(statement);
            return resultSetToArrayList(resultSet);
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.error("Unable to query database: " + path, e);
        }
        return new ArrayList<>();
    }

    /**
     * Transform the query result to a {@link List} of {@link Map} containing {@link String} as key and {@link Object}
     * as value.
     *
     * @param rs the {@link ResultSet} to transform.
     * @return The result list.
     * @throws SQLException If anything goes wrong while fetching data.
     */
    private List<Map<String, Object>> resultSetToArrayList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String, Object>> list = new ArrayList<>(50);
        Map<String, Object> row;
        while (rs.next()) {
            row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }


    private void closeStatement() {
        logger.trace("Closing statement");
        try {
            sqlStatement.close();
            sqlStatement = null;
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.warn("Unable to close statement from database: " + path, e);
        }
    }

    @Override
    public void close() {
        logger.info("Closing database connection");
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                SQLException nextException = e.getNextException();
                if (nextException != null) {
                    e = nextException;
                }
                logger.warn("Unable to close connection to database: " + path, e);
            }
        }
    }

    @Override
    public void initialize() {
        logger.info("Initializing database: " + this.path);
        try (Statement initStatement = connection.createStatement()) {
            List<String> initialization = loadDatabaseSchema();
            for (String statement : initialization) {
                logger.trace("Adding initialization statement: " + statement);
                initStatement.addBatch(statement);
            }
            initStatement.executeBatch();
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.error("Unable to initialize database: " + path, e);
        }
    }


    @Override
    public int execute(String statement) {
        logger.debug("Executing on database: " + statement);
        try (Statement executeStatement = connection.createStatement()) {
            return executeStatement.executeUpdate(statement);
        } catch (SQLException e) {
            SQLException nextException = e.getNextException();
            if (nextException != null) {
                e = nextException;
            }
            logger.error("Unable to execute on database: " + path, e);
        }
        return -1;
    }

    @Override
    public String projectQueryStatement(String name) {
        return "SELECT id FROM Project WHERE name = '" + name + "'";
    }

    @Override
    public String commitQueryStatement(int projectId, String sha) {
        return "SELECT id FROM CommitEntry WHERE sha1 = '" + sha + "' AND projectId = " + projectId;
    }

    @Override
    public String developerQueryStatement(int projectId, String email) {
        return "SELECT id FROM Developer WHERE username = '" + email + "'";
    }

    @Override
    public String smellQueryStatement(int projectId, String instance, String type, boolean onlyLast) {
        String statement = "SELECT id FROM Smell WHERE instance = '" + instance + "' " +
                "AND type = '" + type + "' AND projectId = " + projectId;
        if (onlyLast) {
            statement += " ORDER BY id desc LIMIT 1";
        }
        return statement;
    }

    @Override
    public String projectDevQueryStatement(int projectId, String email) {
        String devQuery = developerQueryStatement(projectId, email);
        return "SELECT id FROM ProjectDeveloper WHERE developerId = (" + devQuery + ") AND projectId = " + projectId;
    }


    /**
     * Load the database schema.
     *
     * @return The list of statements to execute.
     */
    private List<String> loadDatabaseSchema() {
        List<String> output = new ArrayList<>();
        StringBuilder statement = new StringBuilder();
        BufferedReader reader = loadInputFile();
        try {
            for (String line; (line = reader.readLine()) != null; ) {
                statement.append(line).append("\n");

                if (line.contains(";")) {
                    output.add(statement.toString());
                    statement = new StringBuilder();
                }
            }
            return output;
        } catch (IOException e) {
            logger.error("Unable to read SQL definition file: " + path);
        }
        return output;
    }

    /**
     * Load the actual file containing SQL statements.
     *
     * @return A {@link BufferedReader} to the {@link File}.
     */
    private BufferedReader loadInputFile() {
        InputStream resource;
        resource = getClass().getResourceAsStream(schemaResourcePath);
        InputStreamReader streamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
        return new BufferedReader(streamReader);
    }
}
