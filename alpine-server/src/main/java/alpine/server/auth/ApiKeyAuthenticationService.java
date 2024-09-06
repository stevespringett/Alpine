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
package alpine.server.auth;

import alpine.model.ApiKey;
import alpine.persistence.AlpineQueryManager;
import org.glassfish.jersey.server.ContainerRequest;

import javax.naming.AuthenticationException;
import java.security.Principal;

/**
 * Authentication service that validates API keys.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class ApiKeyAuthenticationService implements AuthenticationService {

    private final String assertedApiKey;

    /**
     * Given the specified ContainerRequest, the constructor retrieves a header
     * named 'X-Api-Key' or, if allowed, a URI query parameter named 'apiKey', if
     * they exist.
     * @param request the ContainerRequest object
     * @param allowByQuery allow looking for the API key in the query when
     *                     it is not passed via header
     * @since 1.0.0
     */
    public ApiKeyAuthenticationService(final ContainerRequest request, boolean allowByQuery) {
        if (request.getHeaderString("X-Api-Key") != null) {
            this.assertedApiKey = request.getHeaderString("X-Api-Key");
        } else if (allowByQuery) {
            this.assertedApiKey = request.getUriInfo().getQueryParameters().getFirst("apiKey");
        } else {
            this.assertedApiKey = null;
        }
    }

    /**
     * Returns whether an API key was specified or not.
     * @return true if API key was specified, false if not
     * @since 1.0.0
     */
    public boolean isSpecified() {
        return assertedApiKey != null;
    }

    /**
     * Authenticates the API key (if it was specified in the X-Api-Key header
     * or apiKey query param and returns a Principal if authentication is
     * successful. Otherwise, returns an AuthenticationException.
     * @return a Principal of which ApiKey is an instance of
     * @throws AuthenticationException upon an authentication failure
     * @since 1.0.0
     */
    public Principal authenticate() throws AuthenticationException {
        try (AlpineQueryManager qm = new AlpineQueryManager()) {
            final ApiKey apiKey = qm.getApiKey(assertedApiKey);
            if (apiKey == null) {
                throw new AuthenticationException();
            } else {
                return apiKey;
            }
        }
    }

}
