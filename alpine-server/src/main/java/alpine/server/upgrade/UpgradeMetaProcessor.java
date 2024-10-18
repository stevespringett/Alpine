/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.server.upgrade;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.common.util.VersionComparator;
import alpine.server.util.DbUtil;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * This class determines what upgrades (if any) have already been executed against the current database and
 * documents upgrades that are being installed so that an audit trail of the execution history is maintained.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public class UpgradeMetaProcessor implements Closeable {

    private final static Logger LOGGER = Logger.getLogger(UpgradeMetaProcessor.class);

    private final Connection connection;

    /**
     * Constructs a new UpgradeMetaProcessor object
     * @param connection a SQL Connection object
     * @since 1.2.0
     */
    public UpgradeMetaProcessor(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Constructs a new UpgradeMetaProcessor object
     * along with a new SQL Connection object.
     * @throws UpgradeException when an error occurs creating a Connection object
     * @since 1.8.0
     */
    public UpgradeMetaProcessor() throws UpgradeException {
        this.connection = createConnection();
    }

    /**
     * Determines if the specified upgrade already has a record of being executed previously or not.
     * @param upgradeClass the class to check fi an upgrade has previously executed
     * @return true if already executed, false if not
     * @throws SQLException a SQLException
     * @since 1.2.0
     */
    public boolean hasUpgradeRan(final Class<? extends UpgradeItem> upgradeClass) throws SQLException {
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement("SELECT \"UPGRADECLASS\" FROM \"INSTALLEDUPGRADES\" WHERE \"UPGRADECLASS\" = ?");
            statement.setString(1, upgradeClass.getCanonicalName());
            results = statement.executeQuery();
            return results.next();
        } finally {
            DbUtil.close(results);
            DbUtil.close(statement);
            //DbUtil.close(connection); // do not close connection
        }
    }

    /**
     * Documents a record in the database for the specified class indicating it has been executed.
     * @param upgradeClass the name of the upgrade class
     * @param startTime the time (in millis) of the execution
     * @param endTime the time (in millis) the execution completed
     * @throws SQLException a SQLException
     * @since 1.2.0
     */
    public void installUpgrade(final Class<? extends UpgradeItem> upgradeClass, final long startTime, final long endTime) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO \"INSTALLEDUPGRADES\" (\"UPGRADECLASS\", \"STARTTIME\", \"ENDTIME\") VALUES (?, ?, ?)");
            statement.setString(1, upgradeClass.getCanonicalName());
            statement.setTimestamp(2, new Timestamp(startTime));
            statement.setTimestamp(3, new Timestamp(endTime));
            statement.executeUpdate();
            connection.commit();

            LOGGER.debug("Added: " + upgradeClass.getCanonicalName() + " to UpgradeMetaProcessor table (Starttime: " + startTime + "; Endtime: " + endTime + ")");
        } finally {
            DbUtil.close(statement);
            //DbUtil.close(connection); // do not close connection
        }
    }

    /**
     * Retrieves the current schema version documented in the database.
     * @return A VersionComparator of the schema version
     * @since 1.2.0
     */
    public VersionComparator getSchemaVersion() {
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement("SELECT \"VERSION\" FROM \"SCHEMAVERSION\"");
            results = statement.executeQuery();

            if (results.next()) {
                return new VersionComparator(results.getString(1));
            }
        } catch (SQLException e) {
            // throw it away
        } finally {
            DbUtil.close(results);
            DbUtil.close(statement);
            //DbUtil.close(connection); // do not close connection
        }
        return null;
    }

    /**
     * Updates the schema version in the database.
     * @param version the version to set the schema to
     * @throws SQLException a SQLException
     * @since 1.2.0
     */
    public void updateSchemaVersion(VersionComparator version) throws SQLException {
        PreparedStatement statement = null;
        PreparedStatement updateStatement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement("SELECT \"VERSION\" FROM \"SCHEMAVERSION\"");
            results = statement.executeQuery();

            if (results.next()) {
                final VersionComparator currentVersion = new VersionComparator(results.getString(1));
                if (version == null || currentVersion.isNewerThan(version)) {
                    return;
                }
                updateStatement = connection.prepareStatement("UPDATE \"SCHEMAVERSION\" SET \"VERSION\" = ?");
            } else {
                // Does not exist. Populate schema table with current running version
                version = new VersionComparator(Config.getInstance().getApplicationVersion());
                updateStatement = connection.prepareStatement("INSERT INTO \"SCHEMAVERSION\" (\"VERSION\") VALUES (?)");
            }

            LOGGER.debug("Updating database schema to: " + version.toString());

            updateStatement.setString(1, version.toString());
            updateStatement.executeUpdate();
            connection.commit();
        } finally {
            DbUtil.close(results);
            DbUtil.close(updateStatement);
            DbUtil.close(statement);
            //DbUtil.close(connection); // do not close connection
        }
    }

    /**
     * Creates a SQL Connection object.
     */
    private Connection createConnection() throws UpgradeException {
        final String driver = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER);
        final String dbUrl = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL);
        final String user = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME);
        final String password = Config.getInstance().getPropertyOrFile(Config.AlpineKey.DATABASE_PASSWORD);
        try {
            Class.forName(driver);
            return DriverManager.getConnection(dbUrl, user, password);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to load JDBC driver.", e);
            throw new UpgradeException("Unable to load JDBC driver.", e);
        } catch (SQLException e) {
            LOGGER.error("An error occurred connecting to the database.", e);
            throw new UpgradeException("An error occurred connecting to the database.", e);
        }
    }

    /**
     * Closing a Connection object should only be done when this class creates the Connection
     * and not when a Connection is passed to this objects constructor.
     */
    @Override
    public void close() {
        DbUtil.close(connection);
    }
}
