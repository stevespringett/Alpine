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
package alpine.server.util;

import alpine.persistence.AbstractAlpineQueryManager;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.schema.SchemaAwareStoreManager;

import javax.annotation.WillClose;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class DbUtil {

    private static final String H2_PLATFORM_NAME = "H2";
    private static final String MSSQL_PLATFORM_NAME = "Microsoft SQL Server";
    private static final String MYSQL_PLATFORM_NAME = "MySQL";
    private static final String ORACLE_PLATFORM_NAME = "Oracle";
    private static final String POSTGRESQL_PLATFORM_NAME = "PostgreSQL";

    private static String platform;

    public static void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            // throw it away
        }
    }

    @WillClose
    public static void close(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            // throw it away
        }
    }

    @WillClose
    public static void close(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            // throw it away
        }
    }

    @WillClose
    public static void close(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            // throw it away
        }
    }

    public static void initPlatformName(Connection connection) {
        try {
            DatabaseMetaData dbmd = connection.getMetaData();
            platform = dbmd.getDatabaseProductName();
        } catch (SQLException e) {
            // throw it away
        }
    }

    public static boolean isH2() {
        return platform != null && platform.equalsIgnoreCase(H2_PLATFORM_NAME);
    }

    public static boolean isMssql() {
        return platform != null && platform.equalsIgnoreCase(MSSQL_PLATFORM_NAME);
    }

    public static boolean isMysql() {
        return platform != null && platform.equalsIgnoreCase(MYSQL_PLATFORM_NAME);
    }

    public static boolean isOracle() {
        return platform != null && platform.equalsIgnoreCase(ORACLE_PLATFORM_NAME);
    }

    public static boolean isPostgreSQL() {
        return platform != null && platform.equalsIgnoreCase(POSTGRESQL_PLATFORM_NAME);
    }

    public static void dropColumn(Connection connection, String tableName, String columnName) {
        Statement drop = null;
        try {
            drop = connection.createStatement();
            drop.execute("ALTER TABLE \"" + tableName + "\" DROP COLUMN \"" + columnName + "\"");
        } catch (SQLException e) {
            // throw it away. Some databases do not permit this, so we'll ignore any errors.
        } finally {
            close(drop);
        }
    }

    public static void dropTable(Connection connection, String tableName) {
        Statement drop = null;
        try {
            drop = connection.createStatement();
            drop.execute("DROP TABLE \"" + tableName + "\"");
        } catch (SQLException e) {
            // throw it away. Some databases do not permit this, so we'll ignore any errors.
        } finally {
            close(drop);
        }
    }

    public static void executeUpdate(Connection connection, String statement) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(statement);
        } finally {
            close(stmt);
        }
    }

    public static ArrayList<String> getTableNames(Connection connection) throws SQLException {
        ArrayList<String> tableNames = new ArrayList<>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"});
        while(resultSet.next()) {
            tableNames.add(resultSet.getString("TABLE_NAME"));
        }
        DbUtil.close(resultSet);
        return tableNames;
    }

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        for (String s: getTableNames(connection)) {
            if (s.equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<String> getColumnNames(Connection connection, String tableName) throws SQLException {
        ArrayList<String> columnNames = new ArrayList<>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getColumns(null, null, tableName, null);
        while(resultSet.next()) {
            columnNames.add(resultSet.getString("COLUMN_NAME"));
        }
        DbUtil.close(resultSet);
        return columnNames;
    }

    public static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        for (String s: getColumnNames(connection, tableName)) {
            if (s.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    public static void createTable(AbstractAlpineQueryManager qm, Class... classes) {
        JDOPersistenceManagerFactory pmf = (JDOPersistenceManagerFactory) qm.getPersistenceManager().getPersistenceManagerFactory();
        final PersistenceNucleusContext ctx = pmf.getNucleusContext();
        final Set<String> classNames = new HashSet<>();
        for (Class clazz: classes) {
            classNames.add(clazz.getCanonicalName());
        }
        ((SchemaAwareStoreManager)ctx.getStoreManager()).createSchemaForClasses(classNames, new Properties());
    }

}
