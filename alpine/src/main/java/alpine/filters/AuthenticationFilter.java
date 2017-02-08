/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.filters;

import alpine.auth.ApiKeyAuthService;
import alpine.auth.JwtAuthService;
import alpine.logging.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import javax.annotation.Priority;
import javax.naming.AuthenticationException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.security.Principal;

/**
 * A filter that ensures that all calls going through this filter are
 * authenticated. Exceptions are made for swagger URLs.
 *
 * @see AuthenticationFeature
 * @since 1.0.0
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    // Setup logging
    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext instanceof ContainerRequest) {
            ContainerRequest request = (ContainerRequest) requestContext;

            // Bypass authentication for swagger
            if (request.getRequestUri().getPath().contains("/api/swagger")) {
                return;
            }

            Principal principal = null;

            ApiKeyAuthService apiKeyAuthService = new ApiKeyAuthService(request);
            if (apiKeyAuthService.isSpecified()) {
                try {
                    principal = apiKeyAuthService.authenticate();
                } catch (AuthenticationException e) {
                    logger.info("Invalid login attempt");
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                    return;
                }
            }

            JwtAuthService jwtAuthService = new JwtAuthService(request);
            if (jwtAuthService.isSpecified()) {
                try {
                    principal = jwtAuthService.authenticate();
                } catch (AuthenticationException e) {
                    logger.info("Invalid login attempt");
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                    return;
                }
            }

            if (principal == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            } else {
                requestContext.setProperty("Principal", principal);
            }
        }
    }

}