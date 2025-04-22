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
package alpine.server.filters;

import alpine.common.logging.Logger;
import alpine.model.ApiKey;
import alpine.server.auth.ApiKeyAuthenticationService;
import alpine.server.auth.AllowApiKeyInQueryParameter;
import alpine.server.auth.JwtAuthenticationService;
import org.glassfish.jersey.server.ContainerRequest;
import org.owasp.security.logging.SecurityMarkers;
import org.slf4j.MDC;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import javax.naming.AuthenticationException;
import java.io.IOException;
import java.security.Principal;

/**
 * A filter that ensures that all calls going through this filter are
 * authenticated.
 *
 * @see AuthenticationFeature
 * @author Steve Springett
 * @since 1.0.0
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Setup logging
    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext instanceof ContainerRequest) {
            final ContainerRequest request = (ContainerRequest) requestContext;
            // Bypass authentication for CORS preflight
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                return;
            }

            Principal principal = null;

            final boolean allowsApiKeyInQueryParameter = resourceInfo.getResourceMethod().isAnnotationPresent(AllowApiKeyInQueryParameter.class);
            final ApiKeyAuthenticationService apiKeyAuthService = new ApiKeyAuthenticationService(request, allowsApiKeyInQueryParameter);
            if (apiKeyAuthService.isSpecified()) {
                try {
                    principal = apiKeyAuthService.authenticate();
                    if (principal instanceof final ApiKey apiKey) {
                        ApiKeyUsageTracker.onApiKeyUsed(apiKey);
                    }
                } catch (AuthenticationException e) {
                    LOGGER.info(SecurityMarkers.SECURITY_FAILURE, "Invalid API key asserted");
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                    return;
                }
            }

            final JwtAuthenticationService jwtAuthService = new JwtAuthenticationService(request);
            if (jwtAuthService.isSpecified()) {
                try {
                    principal = jwtAuthService.authenticate();
                } catch (AuthenticationException e) {
                    LOGGER.info(SecurityMarkers.SECURITY_FAILURE, "Invalid JWT asserted");
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                    return;
                }
            }

            if (principal == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            } else {
                requestContext.setProperty("Principal", principal);
                MDC.put("principal", principal.getName());
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {
        MDC.remove("principal");
    }

}
