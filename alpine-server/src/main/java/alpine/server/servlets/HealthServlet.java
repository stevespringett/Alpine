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

import alpine.common.logging.Logger;
import alpine.server.health.HealthCheckRegistry;
import alpine.server.health.HealthCheckType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A {@link HttpServlet} exposing health information, following the MicroProfile Health specification.
 * <p>
 * Health checks can be added by implementing the {@link HealthCheck} interface, and registering
 * implementations with the global {@link HealthCheckRegistry} instance.
 * <p>
 * {@link HealthCheck} implementations must be annotated with either {@link Liveness}, {@link Readiness}, {@link Startup},
 * or any combination of the same. Checks without any of those annotations will be ignored.
 *
 * @see <a href="https://download.eclipse.org/microprofile/microprofile-health-3.1/microprofile-health-spec-3.1.html">MicroProfile Health Specification</a>
 * @since 2.3.0
 */
public class HealthServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HealthServlet.class);

    private final HealthCheckRegistry checkRegistry;
    private ObjectMapper objectMapper;

    public HealthServlet() {
        this(HealthCheckRegistry.getInstance());
    }

    HealthServlet(final HealthCheckRegistry checkRegistry) {
        this.checkRegistry = checkRegistry;
    }

    @Override
    public void init() throws ServletException {
        objectMapper = new ObjectMapper()
                // HealthCheckResponse#data is of type Optional.
                // We need this module to correctly serialize Optional values.
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final HealthCheckType requestedCheckType = determineHealthCheckType(req);

        final var checkResponses = new ArrayList<HealthCheckResponse>();
        try {
            for (final HealthCheck healthCheck : checkRegistry.getChecks().values()) {
                if (matchesCheckType(healthCheck, requestedCheckType)) {
                    LOGGER.debug("Calling health check: " + healthCheck.getClass().getName());
                    checkResponses.add(healthCheck.call());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute health checks", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // The overall UP status is determined by logical conjunction of all check statuses.
        final HealthCheckResponse.Status overallStatus = checkResponses.stream()
                .map(HealthCheckResponse::getStatus)
                .filter(HealthCheckResponse.Status.DOWN::equals)
                .findFirst()
                .orElse(HealthCheckResponse.Status.UP);

        final JsonNode responseJson = JsonNodeFactory.instance.objectNode()
                .put("status", overallStatus.name())
                .putPOJO("checks", checkResponses);

        if (overallStatus == HealthCheckResponse.Status.UP) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        try {
            resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            objectMapper.writeValue(resp.getWriter(), responseJson);
        } catch (IOException e) {
            LOGGER.error("Failed to write health response", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private HealthCheckType determineHealthCheckType(final HttpServletRequest req) {
        final String requestPath = req.getPathInfo();
        if (requestPath == null) {
            return HealthCheckType.ALL;
        }

        return switch (requestPath) {
            case "/live" -> HealthCheckType.LIVENESS;
            case "/ready" -> HealthCheckType.READINESS;
            case "/started" -> HealthCheckType.STARTUP;
            default -> HealthCheckType.ALL;
        };
    }

    private boolean matchesCheckType(final HealthCheck check, final HealthCheckType requestedType) {
        final Class<? extends HealthCheck> checkClass = check.getClass();
        if (checkClass.isAnnotationPresent(Liveness.class)
                && (requestedType == HealthCheckType.ALL || requestedType == HealthCheckType.LIVENESS)) {
            return true;
        } else if (checkClass.isAnnotationPresent(Readiness.class)
                && (requestedType == HealthCheckType.ALL || requestedType == HealthCheckType.READINESS)) {
            return true;
        } else if (checkClass.isAnnotationPresent(Startup.class)
                && (requestedType == HealthCheckType.ALL || requestedType == HealthCheckType.STARTUP)) {
            return true;
        }

        // Checks without classification are supposed to be
        // ignored according to the spec.
        return false;
    }

}
