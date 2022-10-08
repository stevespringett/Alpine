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
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.connection.ConnectionManagerImpl;
import org.datanucleus.store.rdbms.ConnectionFactoryImpl;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.schema.SchemaAwareStoreManager;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
    //private static final ThreadLocal<PersistenceManager> PER_THREAD_PM = new ThreadLocal<>();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Initializing persistence framework");
        pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JdoProperties.get(), "Alpine");

        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            LOGGER.info("Registering DataNucleus metrics");
            registerDataNucleusMetrics(pmf);

            if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_POOL_ENABLED)) {
                LOGGER.info("Registering HikariCP metrics");
                try {
                    registerHikariMetrics(pmf);
                } catch (Exception ex) {
                    // An exception may be thrown here when accessing hidden fields
                    // via reflection failed. Potentially because fields were renamed.
                    // Nothing mission-critical, but users should still be warned.
                    LOGGER.warn("An unexpected error occurred while registering HikariCP metrics", ex);
                }
            }
        }

        // Ensure that the UpgradeMetaProcessor and SchemaVersion tables are created NOW, not dynamically at runtime.
        final PersistenceNucleusContext ctx = pmf.getNucleusContext();
        final Set<String> classNames = new HashSet<>();
        classNames.add(InstalledUpgrades.class.getCanonicalName());
        classNames.add(SchemaVersion.class.getCanonicalName());
        ((SchemaAwareStoreManager)ctx.getStoreManager()).createSchemaForClasses(classNames, new Properties());
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
    }

    private void registerHikariMetrics(final JDOPersistenceManagerFactory pmf) throws IllegalAccessException {
        // HikariCP has native support for Dropwizard and Micrometer metrics.
        // However, DataNucleus doesn't provide access to the underlying DataSource
        // after the PMF has been created. We use reflection to still get access
        // to it and register it with the global metrics registry.
        // In the future, we may construct the DataSources ourselves, so that this
        // workaround won't be necessary anymore.
        if (pmf.getNucleusContext().getStoreManager() instanceof final RDBMSStoreManager storeManager
                && storeManager.getConnectionManager() instanceof final ConnectionManagerImpl connectionManager) {
            registerConnectionPoolMetricsForConnectionFactory(FieldUtils.readField(connectionManager, "primaryConnectionFactory", true));
            registerConnectionPoolMetricsForConnectionFactory(FieldUtils.readField(connectionManager, "secondaryConnectionFactory", true));
        }
    }

    private void registerConnectionPoolMetricsForConnectionFactory(final Object connectionFactory) throws IllegalAccessException {
        if (connectionFactory instanceof final ConnectionFactoryImpl connectionFactoryImpl) {
            final Object dataSource = FieldUtils.readField(connectionFactoryImpl, "dataSource", true);
            if (dataSource instanceof final HikariDataSource hikariDataSource) {
                hikariDataSource.setMetricRegistry(Metrics.getRegistry());
            }
        }
    }

    /*
    private synchronized static PersistenceManager getPerThreadPersistenceManager() {
        PersistenceManager pm = PER_THREAD_PM.get();
        if(pm == null || pm.isClosed()) {
            pm = pmf.getPersistenceManager();
            PER_THREAD_PM.set(pm);
        }
        return pm;
    }
    */
}
