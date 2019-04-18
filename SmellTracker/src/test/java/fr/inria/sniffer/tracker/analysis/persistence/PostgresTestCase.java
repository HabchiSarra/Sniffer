/**
 *   Sniffer - Analyze the history of Android code smells at scale.
 *   Copyright (C) 2019 Sarra Habchi
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
