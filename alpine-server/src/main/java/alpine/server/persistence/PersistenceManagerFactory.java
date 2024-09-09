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
package alpine.server.persistence;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.common.metrics.Metrics;
import alpine.model.InstalledUpgrades;
import alpine.model.SchemaVersion;
import alpine.persistence.IPersistenceManagerFactory;
import alpine.persistence.JdoProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.schema.SchemaAwareStoreManager;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * Initializes the JDO persistence manager on server startup.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class PersistenceManagerFactory implements IPersistenceManagerFactory, ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(PersistenceManagerFactory.class);
    private static final String DATANUCLEUS_METRICS_PREFIX = "datanucleus_";

    private static JDOPersistenceManagerFactory pmf;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Initializing persistence framework");

        final var dnProps = new Properties();

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_MIGRATION_ENABLED)) {
            dnProps.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_DATABASE, "true");
            dnProps.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES, "true");
            dnProps.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, "true");
            dnProps.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, "true");
            dnProps.put(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, "create");
        }

        // Apply pass-through properties.
        dnProps.putAll(Config.getInstance().getPassThroughProperties("datanucleus"));

        // Apply settings that are required by Alpine and shouldn't be customized.
        dnProps.put(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL, "true");
        dnProps.put(PropertyNames.PROPERTY_RETAIN_VALUES, "true");

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            dnProps.put(PropertyNames.PROPERTY_ENABLE_STATISTICS, "true");
        }

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_POOL_ENABLED)) {
            // DataNucleus per default creates two connection factories.
            //  - Primary: Used for operations in transactional context
            //  - Secondary: Used for operations in non-transactional context, schema generation and value generation
            //
            // When using pooling, DN will thus create two connection pools of equal size.
            // However, the optimal sizing of these pools depends on how the application makes use of transactions.
            // When only performing operations within transactions, connections in the secondary pool would remain
            // mostly idle.
            //
            // See also:
            //  - https://www.datanucleus.org/products/accessplatform_6_0/jdo/persistence.html#datastore_connection
            //  - https://datanucleus.groups.io/g/main/topic/95191894#490

            LOGGER.info("Creating transactional connection pool");
            final DataSource txPooledDataSource = createTxPooledDataSource();
            dnProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY, txPooledDataSource);

            // For some applications, the overhead of maintaining two separate connection pools cannot be justified.
            // Allow the transactional (primary) connection pool to be reused for non-transactional operations.
            // https://groups.io/g/datanucleus/topic/side_effects_of_setting/108286305
            if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_POOL_TX_ONLY)) {
                LOGGER.info("Reusing transactional connection pool for non-transactional operations");
                dnProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY2, txPooledDataSource);
            } else {
                LOGGER.info("Creating non-transactional connection pool");
                dnProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY2, createNonTxPooledDataSource());
            }
        } else {
            // No connection pooling; Let DataNucleus handle the datasource setup
            dnProps.put(PropertyNames.PROPERTY_CONNECTION_URL, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL));
            dnProps.put(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER));
            dnProps.put(PropertyNames.PROPERTY_CONNECTION_USER_NAME, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME));
            dnProps.put(PropertyNames.PROPERTY_CONNECTION_PASSWORD, Config.getInstance().getPropertyOrFile(Config.AlpineKey.DATABASE_PASSWORD));
        }

        pmf = (JDOPersistenceManagerFactory) JDOHelper.getPersistenceManagerFactory(dnProps, "Alpine");

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            LOGGER.info("Registering DataNucleus metrics");
            registerDataNucleusMetrics(pmf);
        }

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_MIGRATION_ENABLED)) {
            // Ensure that the UpgradeMetaProcessor and SchemaVersion tables are created NOW, not dynamically at runtime.
            final PersistenceNucleusContext ctx = pmf.getNucleusContext();
            final Set<String> classNames = new HashSet<>();
            classNames.add(InstalledUpgrades.class.getCanonicalName());
            classNames.add(SchemaVersion.class.getCanonicalName());
            ((SchemaAwareStoreManager) ctx.getStoreManager()).createSchemaForClasses(classNames, new Properties());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOGGER.info("Shutting down persistence framework");
        tearDown();
    }

    /**
     * Creates a new JDO PersistenceManager.
     * @return a PersistenceManager
     */
    public static PersistenceManager createPersistenceManager() {
        if (pmf == null && Config.isUnitTestsEnabled()) {
            pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JdoProperties.unit(), "Alpine");
        }
        if (pmf == null) {
            throw new IllegalStateException("Context is not initialized yet.");
        }
        return pmf.getPersistenceManager();
    }

    public PersistenceManager getPersistenceManager() {
        return createPersistenceManager();
    }


    /**
     * Set the {@link JDOPersistenceManagerFactory} to be used by {@link PersistenceManagerFactory}.
     * <p>
     * This is mainly useful for integration tests that run outside a servlet context,
     * yet require a persistence context setup with an external database.
     *
     * @param pmf The {@link JDOPersistenceManagerFactory} to set
     * @throws IllegalStateException When the {@link JDOPersistenceManagerFactory} was already initialized
     * @since 2.1.0
     */
    @SuppressWarnings("unused")
    public static void setJdoPersistenceManagerFactory(final JDOPersistenceManagerFactory pmf) {
        if (PersistenceManagerFactory.pmf != null) {
            throw new IllegalStateException("The PersistenceManagerFactory can only be set when it hasn't been initialized yet.");
        }

        PersistenceManagerFactory.pmf = pmf;
    }

    /**
     * Closes the {@link JDOPersistenceManagerFactory} and removes any reference to it.
     * <p>
     * This method should be called in the {@code tearDown} method of unit- and integration
     * tests that interact with the persistence layer.
     *
     * @since 2.1.0
     */
    public static void tearDown() {
        if (pmf != null) {
            pmf.close();
            pmf = null;
        }
    }

    private void registerDataNucleusMetrics(final JDOPersistenceManagerFactory pmf) {
        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "datastore_reads_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfDatastoreReads())
                .description("Total number of read operations from the datastore")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "datastore_writes_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfDatastoreWrites())
                .description("Total number of write operations to the datastore")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "object_fetches_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfObjectFetches())
                .description("Total number of objects fetched from the datastore")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "object_inserts_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfObjectInserts())
                .description("Total number of objects inserted into the datastore")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "object_updates_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfObjectUpdates())
                .description("Total number of objects updated in the datastore")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "object_deletes_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getNumberOfObjectDeletes())
                .description("Total number of objects deleted from the datastore")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "query_execution_time_ms_avg", pmf,
                        p -> p.getNucleusContext().getStatistics().getQueryExecutionTimeAverage())
                .description("Average query execution time in milliseconds")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "queries_active", pmf,
                        p -> p.getNucleusContext().getStatistics().getQueryActiveTotalCount())
                .description("Number of currently active queries")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "queries_executed_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getQueryExecutionTotalCount())
                .description("Total number of executed queries")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "queries_failed_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getQueryErrorTotalCount())
                .description("Total number of queries that completed with an error")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "transaction_execution_time_ms_avg", pmf,
                        p -> p.getNucleusContext().getStatistics().getTransactionExecutionTimeAverage())
                .description("Average transaction execution time in milliseconds")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "transactions_active", pmf,
                        p -> p.getNucleusContext().getStatistics().getTransactionActiveTotalCount())
                .description("Number of currently active transactions")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "transactions_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getTransactionTotalCount())
                .description("Total number of transactions")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "transactions_committed_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getTransactionCommittedTotalCount())
                .description("Total number of committed transactions")
                .register(Metrics.getRegistry());

        FunctionCounter.builder(DATANUCLEUS_METRICS_PREFIX + "transactions_rolledback_total", pmf,
                        p -> p.getNucleusContext().getStatistics().getTransactionRolledBackTotalCount())
                .description("Total number of rolled-back transactions")
                .register(Metrics.getRegistry());

        // This number does not necessarily equate the number of physical connections.
        // It resembles the number of active connections MANAGED BY DATANUCLEUS.
        // The number of connections reported by connection pool metrics will differ.
        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "connections_active", pmf,
                        p -> p.getNucleusContext().getStatistics().getConnectionActiveCurrent())
                .description("Number of currently active managed datastore connections")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "cache_second_level_entries", pmf,
                        p -> p.getNucleusContext().getLevel2Cache().getSize())
                .description("Number of entries in the second level cache")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "cache_query_generic_compilation_entries", pmf,
                        p -> p.getQueryGenericCompilationCache().size())
                .description("Number of entries in the generic query compilation cache")
                .register(Metrics.getRegistry());

        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "cache_query_datastore_compilation_entries", pmf,
                        p -> p.getQueryDatastoreCompilationCache().size())
                .description("Number of entries in the datastore query compilation cache")
                .register(Metrics.getRegistry());

        // Note: The query results cache is disabled per default.
        Gauge.builder(DATANUCLEUS_METRICS_PREFIX + "cache_query_result_entries", pmf,
                        p -> p.getQueryCache().getQueryCache().size())
                .description("Number of entries in the query result cache")
                .register(Metrics.getRegistry());
    }

    private DataSource createTxPooledDataSource() {
        final var hikariConfig = createBaseHikariConfig("transactional");
        hikariConfig.setMaximumPoolSize(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_TX_MAX_SIZE,
                Config.AlpineKey.DATABASE_POOL_MAX_SIZE,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setMinimumIdle(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_TX_MIN_IDLE,
                Config.AlpineKey.DATABASE_POOL_MIN_IDLE,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setMaxLifetime(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_TX_MAX_LIFETIME,
                Config.AlpineKey.DATABASE_POOL_MAX_LIFETIME,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setIdleTimeout(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_TX_IDLE_TIMEOUT,
                Config.AlpineKey.DATABASE_POOL_IDLE_TIMEOUT,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setKeepaliveTime(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_TX_KEEPALIVE_INTERVAL,
                Config.AlpineKey.DATABASE_POOL_KEEPALIVE_INTERVAL,
                Config.getInstance()::getPropertyAsInt
        ));
        return new HikariDataSource(hikariConfig);
    }

    private DataSource createNonTxPooledDataSource() {
        final var hikariConfig = createBaseHikariConfig("non-transactional");
        hikariConfig.setMaximumPoolSize(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_NONTX_MAX_SIZE,
                Config.AlpineKey.DATABASE_POOL_MAX_SIZE,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setMinimumIdle(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_NONTX_MIN_IDLE,
                Config.AlpineKey.DATABASE_POOL_MIN_IDLE,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setMaxLifetime(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_NONTX_MAX_LIFETIME,
                Config.AlpineKey.DATABASE_POOL_MAX_LIFETIME,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setIdleTimeout(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_NONTX_IDLE_TIMEOUT,
                Config.AlpineKey.DATABASE_POOL_IDLE_TIMEOUT,
                Config.getInstance()::getPropertyAsInt
        ));
        hikariConfig.setKeepaliveTime(getConfigPropertyWithFallback(
                Config.AlpineKey.DATABASE_POOL_NONTX_KEEPALIVE_INTERVAL,
                Config.AlpineKey.DATABASE_POOL_KEEPALIVE_INTERVAL,
                Config.getInstance()::getPropertyAsInt
        ));
        return new HikariDataSource(hikariConfig);
    }

    private HikariConfig createBaseHikariConfig(final String poolName) {
        final var hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(poolName);
        hikariConfig.setJdbcUrl(Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL));
        hikariConfig.setDriverClassName(Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER));
        hikariConfig.setUsername(Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME));
        hikariConfig.setPassword(Config.getInstance().getPropertyOrFile(Config.AlpineKey.DATABASE_PASSWORD));

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            hikariConfig.setMetricRegistry(Metrics.getRegistry());
        }

        return hikariConfig;
    }

    private <T> T getConfigPropertyWithFallback(final Config.Key key, final Config.Key fallbackKey,
                                                final Function<Config.Key, T> method) {
        if (Config.getInstance().getProperty(key) != null) {
            return method.apply(key);
        }

        return method.apply(fallbackKey);
    }

}
