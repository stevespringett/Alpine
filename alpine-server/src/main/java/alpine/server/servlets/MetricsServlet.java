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
package alpine.server.servlets;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.common.metrics.Metrics;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.commons.lang3.StringUtils;
import org.owasp.security.logging.SecurityMarkers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @since 2.1.0
 */
public class MetricsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MetricsServlet.class);

    private final Config config;
    private boolean metricsEnabled;
    private String basicAuthUsername;
    private String basicAuthPassword;

    @SuppressWarnings("unused")
    public MetricsServlet() {
        this(Config.getInstance());
    }

    MetricsServlet(final Config config) {
        this.config = config;
    }

    @Override
    public void init() throws ServletException {
        metricsEnabled = config.getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED);
        basicAuthUsername = config.getProperty(Config.AlpineKey.METRICS_AUTH_USERNAME);
        basicAuthPassword = config.getProperty(Config.AlpineKey.METRICS_AUTH_PASSWORD);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        if (isAuthenticationEnabled() && !isAuthenticated(req)) {
            LOGGER.warn(SecurityMarkers.SECURITY_AUDIT, "Unauthorized access attempt (IP address: " +
                    req.getRemoteAddr() + " / User-Agent: " + req.getHeader("User-Agent") + ")");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"metrics\"");
            return;
        }

        if (metricsEnabled) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader(HttpHeaders.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
            Metrics.getRegistry().scrape(resp.getOutputStream());
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean isAuthenticationEnabled() {
        return StringUtils.isNotBlank(basicAuthUsername) && StringUtils.isNotBlank(basicAuthPassword);
    }

    private boolean isAuthenticated(final HttpServletRequest req) {
        final String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authHeader)) {
            LOGGER.debug("No Authorization header provided");
            return false;
        }

        final String[] headerParts = authHeader.split("\s");
        if (headerParts.length != 2 || !"basic".equalsIgnoreCase(headerParts[0])) {
            LOGGER.debug("Invalid Authorization header format");
            return false;
        }

        final String credentials;
        try {
            final byte[] credentialsBytes = Base64.getUrlDecoder().decode(headerParts[1]);
            credentials = new String(credentialsBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.debug("Decoding basic auth credentials failed", e);
            return false;
        }

        final String[] credentialsParts = credentials.split(":");
        if (credentialsParts.length != 2) {
            LOGGER.debug("Invalid basic auth credentials format");
            return false;
        }

        return StringUtils.equals(basicAuthUsername, credentialsParts[0])
                && StringUtils.equals(basicAuthPassword, credentialsParts[1]);
    }

}
