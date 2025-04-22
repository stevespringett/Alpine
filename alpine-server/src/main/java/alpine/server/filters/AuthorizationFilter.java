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
import alpine.model.UserPrincipal;
import alpine.persistence.AlpineQueryManager;
import alpine.server.auth.PermissionRequired;
import org.glassfish.jersey.server.ContainerRequest;
import org.owasp.security.logging.SecurityMarkers;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * A filter that ensures that all principals making calls that are going
 * through this filter have the necessary permissions to do so.
 *
 * @author Steve Springett
 * @see AuthorizationFeature
 * @since 1.0.0
 */
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(AuthorizationFilter.class);

    public static final String EFFECTIVE_PERMISSIONS_PROPERTY = "effectivePermissions";

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext instanceof ContainerRequest) {

            final Principal principal = (Principal) requestContext.getProperty("Principal");
            if (principal == null) {
                LOGGER.info(SecurityMarkers.SECURITY_FAILURE, "A request was made without the assertion of a valid user principal");
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
                return;
            }

            final PermissionRequired annotation = resourceInfo.getResourceMethod().getDeclaredAnnotation(PermissionRequired.class);
            final Set<String> permissions = Set.of(annotation.value());

            final Set<String> effectivePermissions;
            try (final var qm = new AlpineQueryManager()) {
                effectivePermissions = qm.getEffectivePermissions(principal);
            }

            final boolean hasNoRequiredPermission = Collections.disjoint(permissions, effectivePermissions);
            if (hasNoRequiredPermission) {
                final String requestUri = requestContext.getUriInfo().getRequestUri().toString();

                if (principal instanceof final ApiKey apiKey) {
                    LOGGER.info(
                            SecurityMarkers.SECURITY_FAILURE,
                            "Unauthorized access attempt made by API Key %s to %s".formatted(
                                    apiKey.getMaskedKey(), requestUri));
                } else if (principal instanceof final UserPrincipal user) {
                    LOGGER.info(
                            SecurityMarkers.SECURITY_FAILURE,
                            "Unauthorized access attempt made by %s to %s".formatted(user.getUsername(), requestUri));
                } else {
                    throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
                }

                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            } else {
                requestContext.setProperty(EFFECTIVE_PERMISSIONS_PROPERTY, effectivePermissions);
            }
        }
    }

}
