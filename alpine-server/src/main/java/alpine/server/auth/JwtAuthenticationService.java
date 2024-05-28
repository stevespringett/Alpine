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

import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import org.glassfish.jersey.server.ContainerRequest;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import javax.naming.AuthenticationException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * An AuthenticationService implementation for JWTs that authenticates users
 * based on a token presented in the request. Tokens must be presented
 * using the Authorization Bearer header.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class JwtAuthenticationService implements AuthenticationService {

    private final String bearer;

    /**
     * Constructs a new JwtAuthenticationService.
     * @param request a ContainerRequest object to parse
     */
    public JwtAuthenticationService(final ContainerRequest request) {
        this.bearer = getAuthorizationToken(request);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSpecified() {
        return bearer != null;
    }

    /**
     * {@inheritDoc}
     */
    public Principal authenticate() throws AuthenticationException {
        if (bearer != null) {
            final JsonWebToken jwt = new JsonWebToken();
            final boolean isValid = jwt.validateToken(bearer);
            if (isValid) {
                try (AlpineQueryManager qm = new AlpineQueryManager()) {
                    if (jwt.getSubject() == null || jwt.getExpiration() == null) {
                        throw new AuthenticationException("Token does not contain a valid subject or expiration");
                    }
                    if (jwt.getIdentityProvider() == null || IdentityProvider.LOCAL == jwt.getIdentityProvider()) {
                        final ManagedUser managedUser = qm.getManagedUser(jwt.getSubject());
                        if (managedUser != null) {
                            return managedUser.isSuspended() ? null : managedUser;
                        }
                    } else if (IdentityProvider.LDAP == jwt.getIdentityProvider()) {
                        final LdapUser ldapUser =  qm.getLdapUser(jwt.getSubject());
                        if (ldapUser != null) {
                            return ldapUser;
                        }
                    } else if (IdentityProvider.OPENID_CONNECT == jwt.getIdentityProvider()) {
                        final OidcUser oidcUser = qm.getOidcUser(jwt.getSubject());
                        if (oidcUser != null) {
                            return oidcUser;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the token (as a String), if it exists, otherwise returns null.
     *
     * @param headers the HttpHeader to inspect to find the Authorization-Token
     *                cookie or Authorization Bearer header
     * @return the token if found, otherwise null
     * @since 1.0.0
     */
    private String getAuthorizationToken(final HttpHeaders headers) {
        if (headers.getCookies() != null) {
            for (Map.Entry<String, Cookie> entry : headers.getCookies().entrySet()) {
                if (AuthorizationTokenCookie.COOKIE_NAME.equals(entry.getValue().getName())) {
                    return entry.getValue().getValue();
                }
            }
        }
        final List<String> header = headers.getRequestHeader("Authorization");
        if (header != null) {
            final String bearer = header.get(0);
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return bearer.substring("Bearer ".length());
            }
        }
        return null;
    }

}
