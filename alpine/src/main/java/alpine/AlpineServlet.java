/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine;

import alpine.auth.KeyManager;
import alpine.logging.Logger;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import org.glassfish.jersey.servlet.ServletContainer;
import org.owasp.security.logging.util.IntervalLoggerController;
import org.owasp.security.logging.util.SecurityLoggingFactory;
import org.owasp.security.logging.util.SecurityUtil;
import javax.crypto.SecretKey;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * The AlpineServlet is the main servlet which extends
 * the Jersey ServletContainer. It is responsible for setting up
 * the runtime environment by initializing the application,
 * and setting the path to properties files used for
 * {@link Config Config}(uration).
 *
 * @since 1.0.0
 */
public class AlpineServlet extends ServletContainer {

    private static final long serialVersionUID = -133386507668410112L;
    private static final Logger logger = Logger.getLogger(alpine.AlpineServlet.class);

    /**
     * Overrides the servlet init method and loads sets the InputStream necessary
     * to load application.properties.
     * @throws ServletException a general error that occurs during initialization
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        logger.info("Starting " + Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_NAME));
        super.init(config);

        Info info = new Info()
                .title(Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_NAME) + " API")
                .version(Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_VERSION));

        Swagger swagger = new Swagger()
                .info(info)
                .securityDefinition("X-Api-Key", new ApiKeyAuthDefinition("X-Api-Key", In.HEADER));

        // Dynamically get the url-pattern from web.xml and use that as the 'baseUrl' for
        // the API documentation
        ServletContext servletContext = getServletContext();
        ServletRegistration servletRegistration = servletContext.getServletRegistration(config.getServletName());
        Collection<String> mappings = servletRegistration.getMappings();
        if (mappings.size() > 0) {
            String baseUrl = mappings.iterator().next();
            if (! baseUrl.startsWith("/")) {
                baseUrl = "/" + baseUrl;
            }
            baseUrl = baseUrl.replace("/*", "").replaceAll("\\/$", "");
            swagger.basePath(config.getServletContext().getContextPath() + baseUrl);
        }

        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);

        KeyManager keyManager = KeyManager.getInstance();
        if (!keyManager.keyPairExists()) {
            try {
                KeyPair keyPair = keyManager.generateKeyPair();
                keyManager.save(keyPair);
            } catch (NoSuchAlgorithmException e) {
                logger.error("An error occurred generating new keypair");
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error("An error occurred saving newly generated keypair");
                logger.error(e.getMessage());
            }
        }
        if (!keyManager.secretKeyExists()) {
            try {
                SecretKey secretKey = keyManager.generateSecretKey();
                keyManager.save(secretKey);
            } catch (NoSuchAlgorithmException e) {
                logger.error("An error occurred generating new secret key");
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error("An error occurred saving newly generated secret key");
                logger.error(e.getMessage());
            }
        }

        // Log all Java System Properties
        SecurityUtil.logJavaSystemProperties();

        // Determine if Watchdog logging is enabled and if so, start interval logging
        int interval = Config.getInstance().getPropertyAsInt(Config.AlpineKey.WATCHDOG_LOGGING_INTERVAL);
        if (interval > 0) {
            IntervalLoggerController wd = SecurityLoggingFactory.getControllerInstance();
            wd.start(interval * 1000); // Interval is defined in seconds
        }
    }

    /**
     * Overrides the servlet destroy method and shuts down the servlet
     */
    @Override
    public void destroy() {
        logger.info("Stopping " + Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_NAME));
        super.destroy();
    }

}