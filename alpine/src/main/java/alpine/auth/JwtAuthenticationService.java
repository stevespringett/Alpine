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
package alpine.auth;

import alpine.crypto.KeyManager;
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
 * @author Steve Springett
 * @since 1.0.0
 */
public class JwtAuthenticationService implements AuthenticationService {

    private String bearer = null;

    /**
     * Constructs a new JwtAuthenticationService.
     * @param request a ContainerRequest object to parse
     */
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
        final KeyManager keyManager = KeyManager.getInstance();
        if (bearer != null) {
            final JsonWebToken jwt = new JsonWebToken(keyManager.getSecretKey());
            final boolean isValid = jwt.validateToken(bearer);
            if (isValid) {
                try (AlpineQueryManager qm = new AlpineQueryManager()) {
                    if (jwt.getSubject() == null || jwt.getExpiration() == null) {
                        throw new AuthenticationException("Token does not contain a valid subject or expiration");
                    }
                    final ManagedUser managedUser = qm.getManagedUser(jwt.getSubject());
                    if (managedUser != null) {
                        return (managedUser.isSuspended()) ? null : managedUser;
                    }
                    final LdapUser ldapUser =  qm.getLdapUser(jwt.getSubject());
                    if (ldapUser != null) {
                        return ldapUser;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the token (as a String), if it exists, otherwise returns null.
     *
     * @param headers the HttpHeader to inspect to find Authorization Bearer header
     * @return the token if found, otherwise null
     * @since 1.0.0
     */
    private String getAuthorizationToken(HttpHeaders headers) {
        final List<String> header = headers.getRequestHeader("Authorization");
        if (header != null) {
            final String bearer = header.get(0);
            if (bearer != null) {
                return bearer.substring("Bearer ".length());
            }
        }
        return null;
    }

}
