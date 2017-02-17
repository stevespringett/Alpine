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
package alpine.auth;

import alpine.model.ApiKey;
import alpine.persistence.AlpineQueryManager;
import org.glassfish.jersey.server.ContainerRequest;
import javax.naming.AuthenticationException;
import java.security.Principal;

/**
 * Authentication service that validates API keys
 *
 * @since 1.0.0
 */
public class ApiKeyAuthenticationService implements AuthenticationService {

    private String assertedApiKey = null;

    /**
     * Given the specified ContainerRequest, the constructor retrieves a header
     * named 'X-Api-Key', if it exists.
     *
     * @since 1.0.0
     */
    public ApiKeyAuthenticationService(ContainerRequest request) {
        this.assertedApiKey = request.getHeaderString("X-Api-Key");
    }

    /**
     * Returns whether an API key was specified or not
     *
     * @since 1.0.0
     */
    public boolean isSpecified() {
        return (assertedApiKey != null);
    }

    /**
     * Authenticates the API key (if it was specified in the X-Api-Key header)
     * and returns a Principal if authentication is successful. Otherwise,
     * returns an AuthenticationException.
     *
     * @since 1.0.0
     */
    public Principal authenticate() throws AuthenticationException {
        try (AlpineQueryManager qm = new AlpineQueryManager()) {
            ApiKey apiKey = qm.getApiKey(assertedApiKey);
            if (apiKey == null) {
                throw new AuthenticationException();
            } else {
                return apiKey;
            }
        }
    }

}
