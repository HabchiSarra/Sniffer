package fr.inria.tandoori.analysis.persistence;

import fr.inria.tandoori.analysis.model.Commit;
import fr.inria.tandoori.analysis.model.GitDiff;
import fr.inria.tandoori.analysis.model.GitRename;
import fr.inria.tandoori.analysis.model.Smell;
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
        if (sqlStatement == null) {
            logger.debug("Nothing to commit, skipping");
            return;
        }
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

    /**
     * Escape the string to be compatible with double dollar String insertion.
     *
     * @param entry The string to escape.
     * @return The string with every occurences of "$$" replaced by "$'$".
     */
    private static String escapeStringEntry(String entry) {
        return entry.replace("$$", "$'$");
    }

    @Override
    public String projectQueryStatement(String name) {
        return "SELECT id FROM project WHERE name = '" + name + "'";
    }

    @Override
    public String commitInsertionStatement(int projectId, Commit commit, GitDiff diff, int ordinal) {
        logger.trace("[" + projectId + "] Inserting commit: " + commit.sha
                + " - ordinal: " + ordinal + " - diff: " + diff + " - time: " + commit.date);

        // Escaping double dollars to avoid exiting dollar quoted string too soon.
        String commitMessage = escapeStringEntry(commit.message);

        String developerQuery = developerQueryStatement(commit.authorEmail);
        return "INSERT INTO commit_entry (project_id, developer_id, sha1, ordinal, date, additions, deletions, files_changed, message) VALUES ('" +
                projectId + "', (" + developerQuery + "), '" + commit.sha + "', " + ordinal + ", '" + commit.date.toString() +
                "', " + diff.getAddition() + ", " + diff.getDeletion() + ", " + diff.getChangedFiles() +
                ", $$" + commitMessage + "$$) ON CONFLICT DO NOTHING;";
    }

    @Override
    public String commitIdQueryStatement(int projectId, String sha) {
        return "SELECT id FROM commit_entry WHERE sha1 = '" + sha + "' AND project_id = " + projectId;
    }

    @Override
    public String commitSha1QueryStatement(int projectId, int ordinal) {
        return "SELECT sha1 FROM commit_entry WHERE ordinal = '" + ordinal + "' AND project_id = " + projectId;
    }

    @Override
    public String developerInsertStatement(String developerName) {
        return "INSERT INTO developer (username) VALUES ($$" + escapeStringEntry(developerName) + "$$) ON CONFLICT DO NOTHING;";
    }

    @Override
    public String projectDeveloperInsertStatement(int projectId, String developerName) {
        return "INSERT INTO project_developer (developer_id, project_id) VALUES (" +
                "(" + developerQueryStatement(developerName) + "), " + projectId + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String developerQueryStatement(String email) {
        return "SELECT id FROM developer WHERE username = $$" + escapeStringEntry(email) + "$$";
    }

    @Override
    public String smellInsertionStatement(int projectId, Smell smell) {
        // We know that the parent smell is the last inserted one.
        String parentSmellQuery = smellQueryStatement(projectId, smell.parentInstance, smell.type, true);
        String parentQueryOrNull = smell.parentInstance != null ? "(" + parentSmellQuery + ")" : null;

        return "INSERT INTO smell (project_id, instance, type, file, renamed_from) VALUES" +
                "(" + projectId + ", '" + smell.instance + "', '" + smell.type + "', '"
                + smell.file + "', " + parentQueryOrNull + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String smellCategoryInsertionStatement(int projectId, String sha1, Smell smell, SmellCategory category) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        return "INSERT INTO " + category.getName() + " (project_id, smell_id, commit_id) VALUES " +
                "(" + projectId + ", (" + smellQueryStatement(projectId, smell.instance, smell.type, true) + "), (" +
                commitIdQueryStatement(projectId, sha1) + "));";
    }

    @Override
    public String lostSmellCategoryInsertionStatement(int projectId, Smell smell, SmellCategory category, int since, int until) {
        // We fetch only the last matching inserted smell
        // This helps us handling the case of Gaps between commits
        String lostCategory = "lost_" + category.getName();
        return "INSERT INTO " + lostCategory + " (project_id, smell_id, since, until) VALUES " +
                "(" + projectId + ", (" + smellQueryStatement(projectId, smell.instance, smell.type, true) +
                "), " + "" + since + " , " + until + ");";
    }

    @Override
    public String smellQueryStatement(int projectId, String instance, String type, boolean onlyLast) {
        String statement = "SELECT id FROM smell WHERE instance = '" + instance + "' " +
                "AND type = '" + type + "' AND project_id = " + projectId;
        if (onlyLast) {
            statement += " ORDER BY id desc LIMIT 1";
        }
        return statement;
    }

    @Override
    public String projectDevQueryStatement(int projectId, String email) {
        String devQuery = developerQueryStatement(email);
        return "SELECT id FROM project_developer WHERE developer_id = (" + devQuery + ") AND project_id = " + projectId;
    }

    @Override
    public String lastProjectCommitSha1QueryStatement(int projectId) {
        return "SELECT sha1 FROM commit_entry WHERE project_id = '" + projectId + "' ORDER BY ordinal DESC LIMIT 1";
    }

    @Override
    public String fileRenameInsertionStatement(int projectId, String commitSha, GitRename rename) {
        return "INSERT INTO file_rename (project_id, commit_id, old_file, new_file, similarity) VALUES ('" +
                projectId + "', (" + commitIdQueryStatement(projectId, commitSha) + "), '" + rename.oldFile + "', '" +
                rename.newFile + "', " + rename.similarity + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String branchInsertionStatement(int projectId, int ordinal, Commit parentCommit, Commit mergedInto) {
        String parentCommitQuery = parentCommit == null ? null : "(" + commitIdQueryStatement(projectId, parentCommit.sha) + ")";
        String mergedIntoQuery = mergedInto == null ? null : "(" + commitIdQueryStatement(projectId, mergedInto.sha) + ")";
        return "INSERT INTO branch (project_id, ordinal, parent_commit, merged_into) VALUES ('"
                + projectId + "', '" + ordinal + "', " + parentCommitQuery + ", " + mergedIntoQuery
                + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String branchCommitInsertionQuery(int projectId, int branchOrdinal, String commitSha, int ordinal) {
        return "INSERT INTO branch_commit (branch_id, commit_id, ordinal) VALUES (" +
                "(" + branchIdQueryStatement(projectId, branchOrdinal) + "), " +
                "(" + commitIdQueryStatement(projectId, commitSha) + "), " + ordinal + ") ON CONFLICT DO NOTHING;";
    }

    @Override
    public String branchIdQueryStatement(int projectId, int branchOrdinal) {
        return "SELECT id FROM branch WHERE project_id='" + projectId + "' AND ordinal=" + branchOrdinal;
    }

    public String branchIdQueryStatement(int projectId, Commit commit) {
        return "SELECT branch.id FROM branch " +
                "RIGHT JOIN branch_commit ON branch.id = branch_commit.branch_id " +
                "RIGHT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE commit_entry.sha1 = '" + commit.sha + "' AND commit_entry.project_id = '" + projectId + "'";
    }

    @Override
    public String branchParentCommitSmellsQuery(int projectId, int branchId) {
        return commitSmellsQuery(projectId, branchParentCommitIdQuery(projectId, branchId));
    }

    @Override
    public String branchLastCommitSmellsQuery(int projectId, Commit merge) {
        String branchId = "(" + mergedBranchIdQuery(projectId, merge) + ")";
        return commitSmellsQuery(projectId, branchLastCommitQuery(projectId, branchId, "id"));
    }

    /**
     * Helper method to fetch {@link Smell} instances for a specific commit identifier.
     *
     * @param projectId     The project identifier.
     * @param commitIdQuery Query returning the commit identifier.
     * @return The generated query statement.
     */
    private String commitSmellsQuery(int projectId, String commitIdQuery) {
        return "SELECT type, instance, file FROM smell " +
                "RIGHT JOIN smell_presence ON smell_presence.smell_id = smell.id " +
                "WHERE smell_presence.commit_id = (" + commitIdQuery + ") ";
    }

    @Override
    public String branchParentCommitIdQuery(int projectId, int branchId) {
        return "SELECT parent_commit FROM branch where id = " + branchId + " AND project_id = " + projectId;
    }

    @Override
    public String branchLastCommitShaQuery(int projectId, int currentBranch) {
        return branchLastCommitQuery(projectId, currentBranch, "sha1");
    }

    @Override
    public String branchLastCommitIdQuery(int projectId, int currentBranch) {
        return branchLastCommitQuery(projectId, currentBranch, "id");
    }

    /**
     * Helper method to fetch a last branch commit's commit_entry specific field.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @param field     The commit_entry field to retrieve. can be multiple coma separated fields.
     * @return The generated query statement.
     */
    private String branchLastCommitQuery(int projectId, int branchId, String field) {
        return branchLastCommitQuery(projectId, String.valueOf(branchId), field);
    }

    @Override
    public String branchCommitOrdinalQuery(int projectId, int currentBranch, Commit commit) {
        return "SELECT branch_commit.ordinal FROM branch_commit " +
                "LEFT JOIN commit_entry ON commit_entry.id = branch_commit.commit_id " +
                "WHERE branch_commit.branch_id = " + currentBranch + " " +
                "AND commit_entry.sha1 = '" + commit.sha + "'";
    }

    /**
     * Helper method to fetch a last branch commit's commit_entry specific field.
     *
     * @param projectId The project identifier.
     * @param branchId  The branch identifier.
     * @param field     The commit_entry field to retrieve.
     * @return The generated query statement.
     */
    private String branchLastCommitQuery(int projectId, String branchId, String field) {
        return "SELECT commit_entry." + field + " FROM commit_entry " +
                "JOIN branch_commit " +
                "ON branch_commit.branch_id =  " + branchId + " " +
                "AND branch_commit.commit_id = commit_entry.id " +
                "WHERE commit_entry.project_id = " + projectId + " " +
                "ORDER BY commit_entry.ordinal DESC LIMIT 1";
    }

    @Override
    public String mergedBranchIdQuery(int projectId, Commit commit) {
        return "SELECT id FROM branch WHERE merged_into = (" + commitIdQueryStatement(projectId, commit.sha) + ")";
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
