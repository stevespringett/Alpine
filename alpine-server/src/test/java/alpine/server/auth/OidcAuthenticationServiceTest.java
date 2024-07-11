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
import alpine.model.MappedOidcGroup;
import alpine.model.OidcGroup;
import alpine.model.OidcUser;
import alpine.model.Team;
import alpine.persistence.AlpineQueryManager;
import alpine.server.persistence.PersistenceManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcAuthenticationServiceTest {

    private static final String USERNAME_CLAIM_NAME = "username";
    private static final String ID_TOKEN = "idToken";
    private static final String ACCESS_TOKEN = "accessToken";

    private Config configMock;
    private OidcConfiguration oidcConfigurationMock;
    private OidcIdTokenAuthenticator idTokenAuthenticatorMock;
    private OidcUserInfoAuthenticator userInfoAuthenticatorMock;

    @BeforeAll
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @BeforeEach
    public void setUp() {
        configMock = Mockito.mock(Config.class);
        oidcConfigurationMock = Mockito.mock(OidcConfiguration.class);
        idTokenAuthenticatorMock = Mockito.mock(OidcIdTokenAuthenticator.class);
        userInfoAuthenticatorMock = Mockito.mock(OidcUserInfoAuthenticator.class);

        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USERNAME_CLAIM))).thenReturn(USERNAME_CLAIM_NAME);
    }

    @AfterEach
    public void tearDown() {
        PersistenceManagerFactory.tearDown();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(false);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenAccessTokenAndIdTokenIsNull() {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, null);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcConfigurationIsNull() {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

        final var authService = new OidcAuthenticationService(configMock, null, ID_TOKEN, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndOidcConfigurationIsNotNullAndAccessTokenIsNotNull() {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isTrue();
    }

    @Test
    public void authenticateShouldAuthenticateExistingUserWithIdToken() throws Exception {
        OidcUser existingUser;
        try (final var qm = new AlpineQueryManager()) {
            existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser = qm.persist(existingUser);
        }

        final var profile = new OidcProfile();
        profile.setSubject(existingUser.getSubjectIdentifier());
        profile.setUsername(existingUser.getUsername());
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        Assertions.assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        Assertions.assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
        Assertions.assertThat(authenticatedUser.getEmail()).isNull();
    }

    @Test
    public void authenticateShouldAuthenticateExistingUserWithUserInfo() throws Exception {
        OidcUser existingUser;
        try (final var qm = new AlpineQueryManager()) {
            existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser = qm.persist(existingUser);
        }

        final var profile = new OidcProfile();
        profile.setSubject(existingUser.getSubjectIdentifier());
        profile.setUsername(existingUser.getUsername());
        Mockito.when(userInfoAuthenticatorMock.authenticate(ArgumentMatchers.eq(ACCESS_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, userInfoAuthenticatorMock, null, ACCESS_TOKEN);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        Assertions.assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        Assertions.assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
        Assertions.assertThat(authenticatedUser.getEmail()).isNull();
    }

    @Test
    public void authenticateShouldThrowWhenUsernameClaimIsNotConfigured() {
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USERNAME_CLAIM))).thenReturn(null);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        Assertions.assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldThrowWhenTeamSyncIsEnabledAndTeamsClaimIsNotConfigured() {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn(null);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        Assertions.assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldSynchronizeTeamsWhenUserAlreadyExistsAndTeamSynchronizationIsEnabled() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        OidcUser existingUser;
        try (final var qm = new AlpineQueryManager()) {
            existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            qm.persist(existingUser);

            var group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            var teamToSync = new Team();
            teamToSync.setName("teamName");
            teamToSync = qm.persist(teamToSync);

            var mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(teamToSync);
            qm.persist(mappedGroup);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setGroups(List.of("groupName"));
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        Assertions.assertThat(authenticatedUser.getTeams()).hasSize(1);
        Assertions.assertThat(authenticatedUser.getTeams().get(0).getName()).isEqualTo("teamName");
    }

    @Test
    public void authenticateShouldSourceProfileFromIdTokenAndUserInfoIfAvailable() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        try (final var qm = new AlpineQueryManager()) {
            var group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            var teamToSync = new Team();
            teamToSync.setName("teamName");
            teamToSync = qm.persist(teamToSync);

            final var mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(teamToSync);
            qm.persist(mappedGroup);
        }

        final var idTokenProfile = new OidcProfile();
        idTokenProfile.setSubject("subject");
        idTokenProfile.setUsername("username");
        idTokenProfile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(idTokenProfile);

        final var userInfoProfile = new OidcProfile();
        userInfoProfile.setSubject("subject");
        userInfoProfile.setGroups(List.of("groupName"));
        Mockito.when(userInfoAuthenticatorMock.authenticate(ArgumentMatchers.eq(ACCESS_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(userInfoProfile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, userInfoAuthenticatorMock, ID_TOKEN, ACCESS_TOKEN);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getTeams()).hasSize(1);
        Assertions.assertThat(provisionedUser.getTeams().get(0).getName()).isEqualTo("teamName");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
    }

    @Test
    public void authenticateShouldThrowWhenUnableToAssembleCompleteProfile() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        final var idTokenProfile = new OidcProfile();
        idTokenProfile.setSubject("subject");
        idTokenProfile.setUsername("username");
        idTokenProfile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(idTokenProfile);

        final var userInfoProfile = new OidcProfile();
        userInfoProfile.setSubject("subject");
        userInfoProfile.setUsername("username");
        Mockito.when(userInfoAuthenticatorMock.authenticate(ArgumentMatchers.eq(ACCESS_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(userInfoProfile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, userInfoAuthenticatorMock, ID_TOKEN, ACCESS_TOKEN);

        Assertions.assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowWhenUserDoesNotExistAndProvisioningIsDisabled() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(false);

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        Assertions.assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT));
    }

    @Test
    public void authenticateShouldProvisionAndReturnNewUserWhenUserDoesNotExistAndProvisioningIsEnabled() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser).isNotNull();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        Assertions.assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        Assertions.assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldProvisionAndApplyDefaultTeamsAndReturnNewUserWhenUserDoesNotExistAndProvisioningIsEnabled() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        Mockito.when(configMock.getPropertyAsList(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_DEFAULT))).thenReturn(List.of("teamName"));

        try (final var qm = new AlpineQueryManager()) {
            var teamToAssign = new Team();
            teamToAssign.setName("teamName");
            qm.persist(teamToAssign);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser).isNotNull();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        Assertions.assertThat(provisionedUser.getTeams()).hasSize(1);
        Assertions.assertThat(provisionedUser.getTeams().get(0).getName()).isEqualTo("teamName");
        Assertions.assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldProvisionAndSyncTeamsAndReturnNewUserWhenUserDoesNotExistAndProvisioningAndTeamSyncIsEnabled() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        try (final var qm = new AlpineQueryManager()) {
            var group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            var teamToSync = new Team();
            teamToSync.setName("teamName");
            teamToSync = qm.persist(teamToSync);

            var mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(teamToSync);
            qm.persist(mappedGroup);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setGroups(List.of("groupName"));
        profile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser).isNotNull();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        Assertions.assertThat(provisionedUser.getTeams()).hasSize(1);
        Assertions.assertThat(provisionedUser.getTeams().get(0).getName()).isEqualTo("teamName");
        Assertions.assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldAssignSubjectIdAndEmailWhenUserAlreadyExistsAndAuthenticatesForFirstTime() throws Exception {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createOidcUser("username");
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser).isNotNull();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        Assertions.assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        Assertions.assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldUpdateEmailWhenChangedSinceLastAuthentication() throws Exception {
        try (final var qm = new AlpineQueryManager()) {
            final OidcUser user = qm.createOidcUser("username");
            user.setSubjectIdentifier("subject");
            user.setEmail("username@example.com");
            qm.updateOidcUser(user);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setEmail("username666@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(provisionedUser).isNotNull();
        Assertions.assertThat(provisionedUser.getUsername()).isEqualTo("username");
        Assertions.assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        Assertions.assertThat(provisionedUser.getEmail()).isEqualTo("username666@example.com");
        Assertions.assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        Assertions.assertThat(provisionedUser.getPermissions()).isNullOrEmpty();

        try (final var qm = new AlpineQueryManager()) {
            final OidcUser user = qm.getOidcUser("username");
            assertThat(user.getEmail()).isEqualTo("username666@example.com");
        }
    }

    @Test
    public void authenticateShouldThrowWhenUserAlreadyExistsAndSubjectIdentifierHasChanged() throws Exception {
        try (final var qm = new AlpineQueryManager()) {
            final var existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser.setEmail("username@example.com");
            qm.persist(existingUser);
        }

        final var profile = new OidcProfile();
        profile.setSubject("changedSubject");
        profile.setUsername("username");
        profile.setEmail("username@example.com");
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        Assertions.assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

    @Test
    public void synchronizeTeamsShouldRemoveOutdatedTeamMemberships() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        try (final var qm = new AlpineQueryManager()) {
            var oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subject");
            oidcUser = qm.persist(oidcUser);

            var group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            var team = new Team();
            team.setName("teamName");
            team.setOidcUsers(List.of(oidcUser));
            team = qm.persist(team);

            final var mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(team);
            qm.persist(mappedGroup);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setGroups(Collections.emptyList());
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldRemoveMembershipsOfUnmappedTeams() throws Exception {
        Mockito.when(configMock.getPropertyAsBoolean(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        Mockito.when(configMock.getProperty(ArgumentMatchers.eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        try (final var qm = new AlpineQueryManager()) {
            var oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subject");
            oidcUser = qm.persist(oidcUser);

            var team = new Team();
            team.setName("teamName");
            team.setOidcUsers(Collections.singletonList(oidcUser));
            qm.persist(team);
        }

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setGroups(List.of("groupName"));
        Mockito.when(idTokenAuthenticatorMock.authenticate(ArgumentMatchers.eq(ID_TOKEN), ArgumentMatchers.any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        Assertions.assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
    }

}
