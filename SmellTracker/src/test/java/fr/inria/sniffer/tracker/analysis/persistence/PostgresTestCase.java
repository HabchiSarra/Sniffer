package fr.inria.sniffer.tracker.analysis.persistence;

import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.sql.Connection;
import java.sql.DriverManager;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V9_6;

public abstract class PostgresTestCase extends PersistenceTestCase {
    private Connection connection;
    private EmbeddedPostgres postgres;

    @Before
    public void setUp() throws Exception {
        postgres = new EmbeddedPostgres(V9_6);
        final String url = postgres.start("localhost", Network.getFreeServerPort(),
                "tracker-tests", "tracker", "tracker");

        connection = DriverManager.getConnection(url);
        super.setUp();
    }

    @Override
    protected Persistence initializePersistence() {
        return new PostgresqlPersistence(connection);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        connection.close();
        postgres.stop();
    }
}
