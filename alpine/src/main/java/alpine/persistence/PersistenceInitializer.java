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
package alpine.persistence;

import alpine.Config;
import alpine.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.Server;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.SQLException;

/**
 * Initializes the embedded H2 database. This can be used as a configuration
 * store or as the main database for the application.
 *
 * Refer to {@link Config.AlpineKey#DATABASE_MODE} and application.properties for
 * additional details.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class PersistenceInitializer implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(PersistenceInitializer.class);
    private static Server dbServer;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        startDbServer();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        stopDbServer();
    }

    /**
     * Starts the H2 database engine if the database mode is set to 'server'
     */
    private void startDbServer() {
        final String mode = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_MODE);
        final int port = Config.getInstance().getPropertyAsInt(Config.AlpineKey.DATABASE_PORT);

        if (StringUtils.isEmpty(mode) || !("server".equals(mode) || "embedded".equals(mode) || "external".equals(mode))) {
            LOGGER.error("Database mode not specified. Expected values are 'server', 'embedded', or 'external'");
        }

        if (dbServer != null || "embedded".equals(mode) || "external".equals(mode)) {
            return;
        }
        final String[] args = new String[] {
                "-tcp",
                "-tcpPort", String.valueOf(port),
                "-tcpAllowOthers",
                "-ifNotExists"
        };
        try {
            LOGGER.info("Attempting to start database service");
            dbServer = Server.createTcpServer(args).start();
            LOGGER.info("Database service started");
        } catch (SQLException e) {
            LOGGER.error("Unable to start database service: " + e.getMessage());
            stopDbServer();
        }
    }

    /**
     * Stops the database server (if it was started).
     */
    private void stopDbServer() {
        LOGGER.info("Shutting down database service");
        if (dbServer != null) {
            dbServer.stop();
            dbServer.shutdown();
        }
    }

}
