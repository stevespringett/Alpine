package alpine.util;

import alpine.persistence.PersistenceManagerFactory;

import javax.jdo.PersistenceManager;
import javax.jdo.datastore.JDOConnection;
import java.sql.Connection;
import java.sql.Statement;

/**
 * @since 1.8.0
 */
public final class TestUtil {

    private TestUtil() {
    }

    public static void resetInMemoryDatabase() throws Exception {
        try (final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager()) {
            final JDOConnection jdoConnection = pm.getDataStoreConnection();
            try (final Connection conn = (Connection) jdoConnection.getNativeConnection();
                 final Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP ALL OBJECTS DELETE FILES");
            }
        }
    }

}
