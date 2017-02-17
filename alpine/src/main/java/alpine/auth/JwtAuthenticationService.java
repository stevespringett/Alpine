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

import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.persistence.AlpineQueryManager;
import org.glassfish.jersey.server.ContainerRequest;
import javax.naming.AuthenticationException;
import javax.ws.rs.core.HttpHeaders;
import java.security.Principal;
import java.util.List;

/**
 * An AuthenticationService implementation for JWTs that authenticates users
 * based on a token presented in the request. Tokens must be presented
 * using the Authorization Bearer header.
 *
 * @since 1.0.0
 */
public class JwtAuthenticationService implements AuthenticationService {

    private String bearer = null;

    public JwtAuthenticationService(ContainerRequest request) {
        this.bearer = getAuthorizationToken(request);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSpecified() {
        return (bearer != null);
    }

    /**
     * {@inheritDoc}
     */
    public Principal authenticate() throws AuthenticationException {
        KeyManager keyManager = KeyManager.getInstance();
        if (bearer != null) {
            JsonWebToken jwt = new JsonWebToken(keyManager.getSecretKey());
            boolean isValid = jwt.validateToken(bearer);
            if (isValid) {
                try (AlpineQueryManager qm = new AlpineQueryManager()) {
                    if (jwt.getSubject() == null || jwt.getExpiration() == null) {
                        throw new AuthenticationException("Token does not contain a valid subject or expiration");
                    }
                    ManagedUser managedUser = qm.getManagedUser(jwt.getSubject());
                    if (managedUser != null) {
                        return managedUser;
                    }
                    LdapUser ldapUser =  qm.getLdapUser(jwt.getSubject());
                    if (ldapUser != null) {
                        return ldapUser;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the token (as a String), if it exists, otherwise returns null;
     *
     * @since 1.0.0
     */
    private String getAuthorizationToken(HttpHeaders headers) {
        List<String> header = headers.getRequestHeader("Authorization");
        if (header != null) {
            String bearer = header.get(0);
            if (bearer != null) {
                return bearer.substring("Bearer ".length());
            }
        }
        return null;
    }

}