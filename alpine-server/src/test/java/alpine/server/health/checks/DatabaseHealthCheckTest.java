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
package alpine.server.health.checks;

import alpine.Config;
import alpine.server.persistence.PersistenceManagerFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ConnectionManagerImpl;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.jdo.PersistenceManager;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseHealthCheckTest {

    @BeforeAll
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @AfterEach
    public void tearDown() {
        PersistenceManagerFactory.tearDown();
    }

    @Test
    public void testWithAllConnectionFactoriesUp() {
        final HealthCheckResponse response = new DatabaseHealthCheck().call();
        assertThat(response.getName()).isEqualTo("database");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get()).containsAllEntriesOf(Map.of(
                "nontx_connection_pool", "UP",
                "tx_connection_pool", "UP"
        ));
    }

    @Test
    public void testWithPersistenceManagerFactoryClosed() {
        try (final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager()) {
            pm.getPersistenceManagerFactory().close();
        }

        final HealthCheckResponse response = new DatabaseHealthCheck().call();
        assertThat(response.getName()).isEqualTo("database");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get()).containsAllEntriesOf(Map.of(
                "exception_message", "Cant access or use PMF after it has been closed."
        ));
    }

    @Test
    public void testWithTransactionalConnectionFactoryDown() throws Exception {
        try (final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager()) {
            final var pmf = (JDOPersistenceManagerFactory) pm.getPersistenceManagerFactory();
            final var connectionManager = (ConnectionManagerImpl) pmf.getNucleusContext().getStoreManager().getConnectionManager();
            final var primaryConnectionFactory = (ConnectionFactory) FieldUtils.readField(connectionManager, "primaryConnectionFactory", true);
            primaryConnectionFactory.close();
        }

        final HealthCheckResponse response = new DatabaseHealthCheck().call();
        assertThat(response.getName()).isEqualTo("database");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get()).containsAllEntriesOf(Map.of(
                "nontx_connection_pool", "UP",
                "tx_connection_pool", "DOWN"
        ));
    }

    @Test
    public void testWithSecondaryConnectionFactoryDown() throws Exception {
        try (final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager()) {
            final var pmf = (JDOPersistenceManagerFactory) pm.getPersistenceManagerFactory();
            final var connectionManager = (ConnectionManagerImpl) pmf.getNucleusContext().getStoreManager().getConnectionManager();
            final var secondaryConnectionFactory = (ConnectionFactory) FieldUtils.readField(connectionManager, "secondaryConnectionFactory", true);
            secondaryConnectionFactory.close();
        }

        final HealthCheckResponse response = new DatabaseHealthCheck().call();
        assertThat(response.getName()).isEqualTo("database");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isPresent();
        assertThat(response.getData().get()).containsAllEntriesOf(Map.of(
                "nontx_connection_pool", "DOWN",
                "tx_connection_pool", "UP"
        ));
    }

}