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

import alpine.common.AboutProvider;
import alpine.common.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.datastore.JDOConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static alpine.server.persistence.PersistenceManagerFactory.createPersistenceManager;

/**
 * An {@link AboutProvider} for database information.
 *
 * @since 3.0.0
 */
public class DatabaseAboutProvider implements AboutProvider {

    private static final Logger LOGGER = Logger.getLogger(DatabaseAboutProvider.class);

    @Override
    public String name() {
        return "database";
    }

    @Override
    public Map<String, Object> collect() {
        final var data = new HashMap<String, Object>();

        try (final PersistenceManager pm = createPersistenceManager()) {
            final JDOConnection jdoConnection = pm.getDataStoreConnection();
            final var nativeConnection = (Connection) jdoConnection.getNativeConnection();
            try {
                final DatabaseMetaData databaseMetaData = nativeConnection.getMetaData();
                data.put("productName", databaseMetaData.getDatabaseProductName());
                data.put("productVersion", databaseMetaData.getDatabaseProductVersion());
            } catch (SQLException e) {
                LOGGER.error("Failed to retrieve database metadata", e);
            } finally {
                jdoConnection.close();
            }
        }

        return data;
    }

}
