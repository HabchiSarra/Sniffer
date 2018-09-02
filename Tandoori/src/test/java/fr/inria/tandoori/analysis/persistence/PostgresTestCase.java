package fr.inria.tandoori.analysis.persistence;

import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.sql.Connection;
import java.sql.DriverManager;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V9_6;

public abstract class PostgresTestCase {
    protected Persistence persistence;
    private Connection connection;
    private EmbeddedPostgres postgres;

    @Before
    public void setUp() throws Exception {
        postgres = new EmbeddedPostgres(V9_6);
        final String url = postgres.start("localhost", Network.getFreeServerPort(),
                "tandoori-tests", "tandoori", "tandoori");

        connection = DriverManager.getConnection(url);
        persistence = new PostgresqlPersistence(connection);
        persistence.initialize();
    }

    @After
    public void tearDown() throws Exception {
        persistence.execute("DROP SCHEMA tandoori CASCADE;");
        connection.close();
        postgres.stop();
    }
}
