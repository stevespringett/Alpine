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

import alpine.Config;
import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.OidcUser;
import alpine.model.UserPrincipal;
import alpine.persistence.AlpineQueryManager;
import alpine.server.persistence.PersistenceManagerFactory;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import wiremock.org.apache.hc.core5.http.HttpHeaders;

import javax.naming.AuthenticationException;
import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticationServiceTest {

    @BeforeAll
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @AfterEach
    public void tearDown() {
        PersistenceManagerFactory.tearDown();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenBearerIsNotNull() {
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.isSpecified()).isTrue();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenBearerIsNull() {
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Basic 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void authenticateShouldReturnNullWhenBearerIsNull() throws AuthenticationException {
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Basic 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnNullWhenTokenIsInvalid() throws AuthenticationException {
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer invalidToken"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldThrowExceptionWhenSubjectIsNull() {
        final Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("exp", Instant.now().plusSeconds(60).getEpochSecond());
        final String token = new JsonWebToken().createToken(tokenClaims);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldThrowExceptionWhenExpirationIsNull() {
        final Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("sub", "subject");
        final String token = new JsonWebToken().createToken(tokenClaims);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldReturnNullWhenManagedUserIsSuspended() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final ManagedUser managedUser = qm.createManagedUser("username", "passwordHash");
            managedUser.setSuspended(true);
            qm.persist(managedUser);
        }

        final Principal principalMock = Mockito.mock(Principal.class);
        Mockito.when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnNullWhenNoMatchingUserExists() throws AuthenticationException {
        final Principal principalMock = Mockito.mock(Principal.class);
        Mockito.when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnOidcUserWhenIdentityProviderIsLocal() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = Mockito.mock(Principal.class);
        Mockito.when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser).isInstanceOf(ManagedUser.class);
    }

    @Test
    public void authenticateShouldReturnLdapUserWhenIdentityProviderIsLdap() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = Mockito.mock(Principal.class);
        Mockito.when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LDAP);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser).isInstanceOf(LdapUser.class);
    }

    @Test
    public void authenticateShouldReturnOidcUserWhenIdentityProviderIsOpenIdConnect() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = Mockito.mock(Principal.class);
        Mockito.when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.OPENID_CONNECT);

        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getRequestHeader(ArgumentMatchers.eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser).isInstanceOf(OidcUser.class);
    }

}