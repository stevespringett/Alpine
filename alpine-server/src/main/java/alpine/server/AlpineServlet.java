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
import io.jsonwebtoken.lang.Collections;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import org.glassfish.jersey.servlet.ServletContainer;
import org.owasp.security.logging.util.IntervalLoggerController;
import org.owasp.security.logging.util.SecurityLoggingFactory;
import org.owasp.security.logging.util.SecurityUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.Collection;

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
     * @throws ServletException a general error that occurs during initialization
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        LOGGER.info("Starting " + Config.getInstance().getApplicationName());
        super.init(config);

        final Info info = new Info()
                .title(Config.getInstance().getApplicationName() + " API")
                .version(Config.getInstance().getApplicationVersion());

        final Swagger swagger = new Swagger()
                .info(info)
                .securityDefinition("X-Api-Key", new ApiKeyAuthDefinition("X-Api-Key", In.HEADER));

        // Dynamically get the url-pattern from web.xml and use that as the 'baseUrl' for
        // the API documentation
        final ServletContext servletContext = getServletContext();
        final ServletRegistration servletRegistration = servletContext.getServletRegistration(config.getServletName());
        final Collection<String> mappings = servletRegistration.getMappings();
        if (! Collections.isEmpty(mappings)) {
            String baseUrl = mappings.iterator().next();
            if (baseUrl.charAt(0) != '/') {
                baseUrl = "/" + baseUrl;
            }
            baseUrl = baseUrl.replace("/*", "").replaceAll("\\/$", "");
            swagger.basePath(config.getServletContext().getContextPath() + baseUrl);
        }

        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger).initScanner();

        // Initializes the KeyManager
        KeyManager.getInstance();

        // Log all Java System Properties
        SecurityUtil.logJavaSystemProperties();

        // Determine if Watchdog logging is enabled and if so, start interval logging
        final int interval = Config.getInstance().getPropertyAsInt(Config.AlpineKey.WATCHDOG_LOGGING_INTERVAL);
        if (interval > 0) {
            final IntervalLoggerController wd = SecurityLoggingFactory.getControllerInstance();
            wd.start(interval * 1000); // Interval is defined in seconds
        }
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
