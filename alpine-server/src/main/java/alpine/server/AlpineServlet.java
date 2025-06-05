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
package alpine.server;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.security.crypto.KeyManager;
import org.glassfish.jersey.servlet.ServletContainer;
import org.owasp.security.logging.util.SecurityUtil;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

/**
 * The AlpineServlet is the main servlet which extends
 * the Jersey ServletContainer. It is responsible for setting up
 * the runtime environment by initializing the application,
 * and setting the path to properties files used for
 * {@link Config Config}(uration).
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class AlpineServlet extends ServletContainer {

    private static final long serialVersionUID = -133386507668410112L;
    private static final Logger LOGGER = Logger.getLogger(AlpineServlet.class);

    /**
     * Overrides the servlet init method and loads sets the InputStream necessary
     * to load application.properties.
     *
     * @throws ServletException a general error that occurs during initialization
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        LOGGER.info("Starting " + Config.getInstance().getApplicationName());
        super.init(config);

        // Initializes the KeyManager
        KeyManager.getInstance();

        // Log all Java System Properties
        SecurityUtil.logJavaSystemProperties();

        LOGGER.info(Config.getInstance().getApplicationName() + " is ready");
    }

    /**
     * Overrides the servlet destroy method and shuts down the servlet.
     */
    @Override
    public void destroy() {
        LOGGER.info("Stopping " + Config.getInstance().getApplicationName());
        super.destroy();
    }

}
