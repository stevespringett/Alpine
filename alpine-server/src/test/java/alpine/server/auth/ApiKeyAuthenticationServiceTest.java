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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

import javax.naming.AuthenticationException;

import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import alpine.Config;
import alpine.model.ApiKey;
import alpine.persistence.AlpineQueryManager;
import alpine.security.ApiKeyGenerator;
import alpine.server.persistence.PersistenceManagerFactory;

public class ApiKeyAuthenticationServiceTest {
    private static final String prefix = Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);

    @BeforeAll
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @AfterEach
    public void tearDown() {
        PersistenceManagerFactory.tearDown();
    }

    @Test
    public void authenticationWorksWithRigthKey() throws AuthenticationException {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            apiKey = qm.createApiKey(team);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(apiKey.getKey());
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        final ApiKey authenticatedUser = (ApiKey) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser.getId() == apiKey.getId());
    }

    @Test
    public void authenticationWorksWithRegeneratedKey() throws AuthenticationException {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            var originalApiKey = qm.createApiKey(team);
            apiKey = qm.regenerateApiKey(originalApiKey);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(apiKey.getKey());
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        final ApiKey authenticatedUser = (ApiKey) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser.getId() == apiKey.getId());
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForOldKeyAfterRegeneration() {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            apiKey = qm.createApiKey(team);
            qm.regenerateApiKey(apiKey);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(apiKey.getKey());
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForInvalidKey() {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            apiKey = qm.createApiKey(team);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(prefix + apiKey.getPublicId() + "0".repeat(ApiKey.API_KEY_LENGTH));
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForInvalidPrefix() {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            apiKey = qm.createApiKey(team);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(prefix + "0".repeat(ApiKey.PUBLIC_ID_LENGTH) + ApiKey.getOnlyKey(apiKey.getKey()));
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForToShortKey() {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            qm.createApiKey(team);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn("InvalidKey");
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForToLongKey() {
        ApiKey apiKey;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            apiKey = qm.createApiKey(team);
        }
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(apiKey.getKey() + "1");
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationLegacyStillWorks() throws NoSuchAlgorithmException, AuthenticationException {
        final var apiKey = genLegacyKey();
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(apiKey.getKey());
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        final ApiKey authenticatedUser = (ApiKey) authService.authenticate();
        Assertions.assertThat(authenticatedUser).isNotNull();
        Assertions.assertThat(authenticatedUser.getId() == apiKey.getId());
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForInvalidKeyForLegacy() throws NoSuchAlgorithmException {
        final var apiKey = genLegacyKey();
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(prefix + apiKey.getPublicId() + "0".repeat(ApiKey.API_KEY_LENGTH));
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticationShouldThrowAuthenticationExceptionForInvalidPrefixForLegacy() throws NoSuchAlgorithmException {
        final var apiKey = genLegacyKey();
        final ContainerRequest containerRequestMock = Mockito.mock(ContainerRequest.class);
        Mockito.when(containerRequestMock.getHeaderString("X-Api-Key"))
                .thenReturn(prefix + "0".repeat(ApiKey.PUBLIC_ID_LENGTH) + ApiKey.getOnlyKey(apiKey.getKey()));
        final ApiKeyAuthenticationService authService = new ApiKeyAuthenticationService(containerRequestMock, false);

        Assertions.assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    private ApiKey genLegacyKey() throws NoSuchAlgorithmException {
        final var apiKey = new ApiKey();
        final String clearKey = ApiKeyGenerator.generate(32);
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final var team = qm.createTeam("Test");
            final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final String hashedKey = HexFormat.of().formatHex(digest.digest(ApiKey.getOnlyKeyAsBytes(clearKey)));
            apiKey.setKey(hashedKey);
            apiKey.setPublicId(ApiKey.getPublicId(clearKey));
            apiKey.setCreated(new Date());
            apiKey.setTeams(List.of(team));
            qm.persist(apiKey);
        }
        apiKey.setKey(clearKey);
        return apiKey;
    }
}
