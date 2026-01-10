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
package alpine.embedded;

import org.eclipse.jetty.ee11.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * The primary class that starts an embedded Jetty server
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class EmbeddedJettyServer {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EmbeddedJettyServer.class);

    /**
     * Private constructor.
     */
    private EmbeddedJettyServer() {
    }

    public static void main(final String[] args) throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        final Properties execWarProperties;
        try (final InputStream fis = contextClassLoader.getResourceAsStream("alpine-executable-war.version")) {
            execWarProperties = new Properties();
            execWarProperties.load(fis);
            LOGGER.info(execWarProperties.getProperty("name") + " v" + execWarProperties.getProperty("version") + " (" + execWarProperties.getProperty("uuid") + ") built on: " + execWarProperties.getProperty("timestamp"));
        }

        final CliArgs cliArgs = new CliArgs(args);
        final String contextPath = cliArgs.switchValue("-context", "/");
        final String host = cliArgs.switchValue("-host", "0.0.0.0");
        final int port = cliArgs.switchIntegerValue("-port", 8080);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final Server server = new Server();
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer()); // Add support for X-Forwarded headers

        // Enable legacy (mimicking Jetty 9) URI compliance.
        // This is required to allow URL encoding in path segments, e.g. "/foo/bar%2Fbaz".
        // https://github.com/jetty/jetty.project/issues/12162
        // https://github.com/jetty/jetty.project/issues/11448
        // https://jetty.org/docs/jetty/12/programming-guide/server/compliance.html#uri
        //
        // NB: The setting on its own is not sufficient. Decoding of ambiguous URIs
        // must additionally be enabled in the servlet handler. This can only be done
        // after the server is started, further down below.
        //
        // TODO: Remove this for the next major version bump. Since we're going against Servlet API
        //  here, the only viable long-term solution is to adapt REST APIs to follow Servlet API 6 spec.
        httpConfig.setUriCompliance(UriCompliance.LEGACY);

        final HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
        final ServerConnector connector = new ServerConnector(server, connectionFactory);
        connector.setHost(host);
        connector.setPort(port);
        disableServerVersionHeader(connector);
        server.setConnectors(new Connector[]{connector});

        final WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setContextPath(contextPath);
        context.setErrorHandler(new ErrorHandler());
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/[^/]*taglibs.*\\.jar$");
        context.setThrowUnavailableOnStartupException(true);

        // Define a static temp directory to extract the WAR file into.
        // The directory name contains the host and port to accommodate
        // for multiple instances running at the same time.
        //
        // Prevents Jetty from creating new directories with random
        // names, which could lead to disk bloat if the server is
        // not shut down gracefully: https://github.com/DependencyTrack/dependency-track/issues/4797
        //
        // Note that disabling WAR extraction entirely is not possible,
        // because Java does not support loading nested JARs,
        // which is needed to load dependencies from WEB-INF/lib/*.
        Optional<String> applicationName;
        try {
            applicationName = getApplicationName(contextClassLoader);
        } catch (IOException e) {
            LOGGER.warn("Failed to determine application name", e);
            applicationName = Optional.empty();
        }

        final File tempDirectory = new File(
                System.getProperty("java.io.tmpdir"),
                "%s-%s-%d".formatted(
                        applicationName
                                .orElse(execWarProperties.getProperty("name"))
                                .replaceAll("[^a-zA-Z0-9\\-_]", "_"),
                        host.replaceAll("[^a-zA-Z0-9\\-_]", "_"),
                        port));
        context.setTempDirectory(tempDirectory);

        if (tempDirectory.exists()) {
            LOGGER.warn("Deleting stale temp directory: {}", tempDirectory);
            try {
                deleteRecursively(tempDirectory.toPath());
            } catch (IOException e) {
                LOGGER.error("Failed to delete stale temp directory: {}", tempDirectory, e);
                System.exit(-1);
            }
        }

        // Prevent loading of logging classes
        context.getProtectedClassMatcher().add("org.apache.log4j.");
        context.getProtectedClassMatcher().add("org.slf4j.");
        context.getProtectedClassMatcher().add("org.apache.commons.logging.");

        final ProtectionDomain protectionDomain = EmbeddedJettyServer.class.getProtectionDomain();
        final URL location = protectionDomain.getCodeSource().getLocation();
        context.setWar(location.toExternalForm());

        // Allow applications to customize the WebAppContext via Jetty context XML file.
        // An example use-case is the customization of JARs that Jetty shall scan for annotations.
        //
        // https://jetty.org/docs/jetty/12/operations-guide/xml/index.html
        // https://jetty.org/docs/jetty/12/operations-guide/annotations/index.html
        final URL jettyContextUrl = contextClassLoader.getResource("WEB-INF/jetty-context.xml");
        if (jettyContextUrl != null) {
            LOGGER.debug("Applying Jetty customization from {}", jettyContextUrl);
            final Resource jettyContextResource = new URLResourceFactory().newResource(jettyContextUrl);
            final var xmlConfiguration = new XmlConfiguration(jettyContextResource);
            xmlConfiguration.configure(context);
        }

        server.setHandler(context);
        server.addBean(new ErrorHandler());
        try {
            server.start();
            for (final ServletHandler handler : server.getContainedBeans(ServletHandler.class)) {
                LOGGER.debug("Enabling decoding of ambiguous URIs for servlet handler: {}", handler.getClass().getName());
                handler.setDecodeAmbiguousURIs(true);
            }
            addJettyShutdownHook(server);
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void disableServerVersionHeader(Connector connector) {
        connector.getConnectionFactories().stream()
                .filter(cf -> cf instanceof HttpConnectionFactory)
                .forEach(cf -> ((HttpConnectionFactory) cf)
                        .getHttpConfiguration().setSendServerVersion(false));
    }

    /**
     * Dummy error handler that disables any error pages or jetty related messages and an empty page with a status code.
     */
    private static class ErrorHandler extends ErrorPageErrorHandler {
        @Override
        public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
            response.setStatus(response.getStatus());
            callback.succeeded();
            return true;
        }
    }

    private static void addJettyShutdownHook(final Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("Shutting down application");
                    server.stop();
                } catch (Exception e) {
                    //System.err.println("Exception occurred shutting down: " + e.getMessage());
                }
            }
        });
    }

    private static Optional<String> getApplicationName(ClassLoader classLoader) throws IOException {
        try (final InputStream fis = classLoader.getResourceAsStream("WEB-INF/classes/application.version")) {
            if (fis == null) {
                return Optional.empty();
            }

            final var properties = new Properties();
            properties.load(fis);

            return Optional.ofNullable(properties.getProperty("name"));
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (final Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

}
