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

import alpine.common.logging.Logger;
import alpine.server.persistence.PersistenceManagerFactory;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.eclipse.microprofile.health.Readiness;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * @since 2.3.0
 */
@Readiness
public class DatabaseHealthCheck implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger(DatabaseHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        final var responseBuilder = HealthCheckResponse.named("database");

        try (final PersistenceManager pm = PersistenceManagerFactory.createPersistenceManager()) {
            // DataNucleus maintains different connection pools for transactional and
            // non-transactional operations. Check both of them by executing a test query
            // in a transactional and non-transactional context.
            final Status nonTransactionalStatus = checkNonTransactionalConnectionPool(pm);
            final Status transactionalStatus = checkTransactionalConnectionPool(pm);

            responseBuilder
                    .status(nonTransactionalStatus == Status.UP && transactionalStatus == Status.UP)
                    .withData("nontx_connection_pool", nonTransactionalStatus.name())
                    .withData("tx_connection_pool", transactionalStatus.name());
        } catch (Exception e) {
            LOGGER.error("Executing database health check failed", e);
            responseBuilder.down()
                    .withData("exception_message", e.getMessage());
        }

        return responseBuilder.build();
    }

    private Status checkNonTransactionalConnectionPool(final PersistenceManager pm) {
        LOGGER.debug("Checking non-transactional connection pool");
        try {
            return executeQuery(pm);
        } catch (Exception e) {
            LOGGER.error("Checking non-transactional connection pool failed", e);
            return Status.DOWN;
        }
    }

    private Status checkTransactionalConnectionPool(final PersistenceManager pm) {
        LOGGER.debug("Checking transactional connection pool");
        final Transaction trx = pm.currentTransaction();
        trx.setRollbackOnly();
        try {
            trx.begin();
            return executeQuery(pm);
        } catch (Exception e) {
            LOGGER.error("Checking transactional connection pool failed", e);
            return Status.DOWN;
        } finally {
            if (trx.isActive()) {
                trx.rollback();
            }
        }
    }

    private Status executeQuery(final PersistenceManager pm) {
        final Query<?> query = pm.newQuery(Query.SQL, "SELECT 1");
        try {
            query.executeResultUnique(Long.class);
            return Status.UP;
        } finally {
            query.closeAll();
        }
    }

}
