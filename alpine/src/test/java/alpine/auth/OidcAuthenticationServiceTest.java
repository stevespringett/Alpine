package alpine.auth;

import alpine.Config;
import alpine.model.MappedOidcGroup;
import alpine.model.OidcGroup;
import alpine.model.OidcUser;
import alpine.model.Team;
import alpine.persistence.AlpineQueryManager;
import alpine.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticationServiceTest {

    private static final String USERNAME_CLAIM_NAME = "username";
    private static final String ID_TOKEN = "idToken";
    private static final String ACCESS_TOKEN = "accessToken";

    private Config configMock;
    private OidcConfiguration oidcConfigurationMock;
    private OidcIdTokenAuthenticator idTokenAuthenticatorMock;
    private OidcUserInfoAuthenticator userInfoAuthenticatorMock;

    @BeforeClass
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @Before
    public void setUp() {
        configMock = mock(Config.class);
        oidcConfigurationMock = mock(OidcConfiguration.class);
        idTokenAuthenticatorMock = mock(OidcIdTokenAuthenticator.class);
        userInfoAuthenticatorMock = mock(OidcUserInfoAuthenticator.class);

        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_USERNAME_CLAIM))).thenReturn(USERNAME_CLAIM_NAME);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.resetInMemoryDatabase();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(false);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenAccessTokenAndIdTokenIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, null);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcConfigurationIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

        final var authService = new OidcAuthenticationService(configMock, null, ID_TOKEN, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndOidcConfigurationIsNotNullAndAccessTokenIsNotNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED))).thenReturn(true);

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
        assertThat(authenticatedUser.getEmail()).isNull();
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
        when(userInfoAuthenticatorMock.authenticate(eq(ACCESS_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, userInfoAuthenticatorMock, null, ACCESS_TOKEN);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
        assertThat(authenticatedUser.getEmail()).isNull();
    }

    @Test
    public void authenticateShouldThrowWhenUsernameClaimIsNotConfigured() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_USERNAME_CLAIM))).thenReturn(null);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldThrowWhenTeamSyncIsEnabledAndTeamsClaimIsNotConfigured() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn(null);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ID_TOKEN, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldSynchronizeTeamsWhenUserAlreadyExistsAndTeamSynchronizationIsEnabled() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(authenticatedUser.getTeams()).hasSize(1);
        assertThat(authenticatedUser.getTeams().get(0).getName()).isEqualTo("teamName");
    }

    @Test
    public void authenticateShouldSourceProfileFromIdTokenAndUserInfoIfAvailable() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(idTokenProfile);

        final var userInfoProfile = new OidcProfile();
        userInfoProfile.setSubject("subject");
        userInfoProfile.setGroups(List.of("groupName"));
        when(userInfoAuthenticatorMock.authenticate(eq(ACCESS_TOKEN), any(OidcProfileCreator.class))).thenReturn(userInfoProfile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, userInfoAuthenticatorMock, ID_TOKEN, ACCESS_TOKEN);

        final var provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getTeams()).hasSize(1);
        assertThat(provisionedUser.getTeams().get(0).getName()).isEqualTo("teamName");
        assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
    }

    @Test
    public void authenticateShouldThrowWhenUnableToAssembleCompleteProfile() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

        final var idTokenProfile = new OidcProfile();
        idTokenProfile.setSubject("subject");
        idTokenProfile.setUsername("username");
        idTokenProfile.setEmail("username@example.com");
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(idTokenProfile);

        final var userInfoProfile = new OidcProfile();
        userInfoProfile.setSubject("subject");
        userInfoProfile.setUsername("username");
        when(userInfoAuthenticatorMock.authenticate(eq(ACCESS_TOKEN), any(OidcProfileCreator.class))).thenReturn(userInfoProfile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, userInfoAuthenticatorMock, ID_TOKEN, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowWhenUserDoesNotExistAndProvisioningIsDisabled() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(false);

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT));
    }

    @Test
    public void authenticateShouldProvisionAndReturnNewUserWhenUserDoesNotExistAndProvisioningIsEnabled() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);

        final var profile = new OidcProfile();
        profile.setSubject("subject");
        profile.setUsername("username");
        profile.setEmail("username@example.com");
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldProvisionAndSyncTeamsAndReturnNewUserWhenUserDoesNotExistAndProvisioningAndTeamSyncIsEnabled() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING))).thenReturn(true);
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        assertThat(provisionedUser.getTeams()).hasSize(1);
        assertThat(provisionedUser.getTeams().get(0).getName()).isEqualTo("teamName");
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("username@example.com");
        assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

    @Test
    public void synchronizeTeamsShouldRemoveOutdatedTeamMemberships() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldRemoveMembershipsOfUnmappedTeams() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM))).thenReturn("groups");

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
        when(idTokenAuthenticatorMock.authenticate(eq(ID_TOKEN), any(OidcProfileCreator.class))).thenReturn(profile);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, idTokenAuthenticatorMock, null, ID_TOKEN, null);

        final var authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
    }

}
