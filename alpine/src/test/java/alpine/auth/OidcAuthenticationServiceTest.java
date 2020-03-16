package alpine.auth;

import alpine.Config;
import alpine.model.MappedOidcGroup;
import alpine.model.OidcGroup;
import alpine.model.OidcUser;
import alpine.model.Team;
import alpine.persistence.AlpineQueryManager;
import alpine.persistence.PersistenceManagerFactory;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import javax.jdo.PersistenceManager;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticationServiceTest {

    private static final String OIDC_USERINFO_PATH = "/userinfo";
    private static final String USERNAMCE_CLAIM = "usernameClaim";
    private static final String ACCESS_TOKEN = "accessToken";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private Config configMock;
    private OidcConfiguration oidcConfigurationMock;
    private PersistenceManager persistenceManager;

    @BeforeClass
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @Before
    public void setUp() {
        configMock = mock(Config.class);
        oidcConfigurationMock = mock(OidcConfiguration.class);

        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_USERNAME_CLAIM)))
                .thenReturn(USERNAMCE_CLAIM);

        when(oidcConfigurationMock.getUserInfoEndpointUri())
                .thenReturn(wireMockRule.url(OIDC_USERINFO_PATH));

        persistenceManager = PersistenceManagerFactory.createPersistenceManager();
    }

    @After
    @SuppressWarnings("unchecked")
    public void tearDown() {
        // Delete all users that may have been persisted during the test
        final List<OidcUser> users = (List<OidcUser>) persistenceManager.newQuery(OidcUser.class).execute();
        if (users != null) {
            persistenceManager.deletePersistentAll(users);
        }
        final List<MappedOidcGroup> mappedGroups = (List<MappedOidcGroup>) persistenceManager.newQuery(MappedOidcGroup.class).execute();
        if (mappedGroups != null) {
            persistenceManager.deletePersistentAll(mappedGroups);
        }
        final List<OidcGroup> groups = (List<OidcGroup>) persistenceManager.newQuery(OidcGroup.class).execute();
        if (users != null) {
            persistenceManager.deletePersistentAll(groups);
        }
        final List<Team> teams = (List<Team>) persistenceManager.newQuery(Team.class).execute();
        if (teams != null) {
            persistenceManager.deletePersistentAll(teams);
        }
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(false);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenAccessTokenIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcConfigurationIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, null, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndOidcConfigurationIsNotNullAndAccessTokenIsNotNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThat(authService.isSpecified()).isTrue();
    }

    @Test
    public void authenticateShouldThrowExceptionWhenRequestingUserInfoWithInvalidToken() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

    @Test
    public void authenticateShouldThrowExceptionWhenRequestingUserCausesAnUnexpectedHttpError() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowExceptionWhenUserInfoResponseProcessingFailed() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("{}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowExceptionWhenUserInfoDoesNotContainConfiguredUsernameClaim() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"subject\", " +
                                "    \"email\": \"subject@mail.local\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldReturnUserWhenAlreadyExists() throws AlpineAuthenticationException {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"subject\", " +
                                "    \"email\": \"subject@mail.local\", " +
                                "    \"" + USERNAMCE_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        OidcUser existingUser;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser.setEmail("subject@mail.local");
            existingUser = qm.persist(existingUser);
        }

        final OidcUser authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        assertThat(authenticatedUser.getSubjectIdentifier()).isEqualTo(existingUser.getSubjectIdentifier());
        assertThat(authenticatedUser.getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(authenticatedUser.getTeams()).isNullOrEmpty();
        assertThat(authenticatedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldSynchronizeTeamsWhenUserAlreadyExistsAndAlwaysSyncTeamsIsEnabled() throws AlpineAuthenticationException {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)))
                .thenReturn(true);
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ALWAYS_SYNC_TEAMS)))
                .thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn("groups");

        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"subject\", " +
                                "    \"email\": \"subject@mail.local\", " +
                                "    \"" + USERNAMCE_CLAIM + "\": \"username\", " +
                                "    \"groups\": [\"groupName\"]" +
                                "}")));

        Team teamToSync;
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser.setEmail("subject@mail.local");
            qm.persist(existingUser);

            OidcGroup group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            teamToSync = new Team();
            teamToSync.setName("teamName");
            teamToSync = qm.persist(teamToSync);

            MappedOidcGroup mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(teamToSync);
            qm.persist(mappedGroup);
        }

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        final OidcUser authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser.getTeams()).hasSize(1);
        assertThat(authenticatedUser.getTeams().get(0).getName()).isEqualTo("teamName");
    }

    @Test
    public void authenticateShouldThrowExceptionWhenUserDoesNotExistAndProvisioningIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING)))
                .thenReturn(false);

        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"subject\", " +
                                "    \"email\": \"subject@mail.local\", " +
                                "    \"" + USERNAMCE_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT));
    }

    @Test
    public void authenticateShouldProvisionAndReturnNewUserWhenUserDoesNotExistAndProvisioningIsEnabled() throws AlpineAuthenticationException {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING)))
                .thenReturn(true);

        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"subject\", " +
                                "    \"email\": \"subject@mail.local\", " +
                                "    \"" + USERNAMCE_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        final OidcUser provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("subject@mail.local");
        assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void synchronizeTeamsShouldReturnUserWhenNoTeamsClaimWasConfigured() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn(null);

        final OidcUser oidcUser = new OidcUser();

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThat(authService.synchronizeTeams(null, oidcUser, null)).isEqualTo(oidcUser);
    }

    @Test
    public void synchronizeTeamsShouldReturnUserWhenTeamsClaimIsNotAList() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn("teams");

        final OidcUserInfo userInfo = new OidcUserInfo();
        userInfo.setClaim("teams", "not-a-list");

        final OidcUser oidcUser = new OidcUser();

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

        assertThat(authService.synchronizeTeams(null, oidcUser, userInfo)).isEqualTo(oidcUser);
    }

    @Test
    public void synchronizeTeamsShouldAddNewTeamMemberships() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn("teams");

        final OidcUserInfo userInfo = new OidcUserInfo();
        userInfo.setClaim("teams", Collections.singletonList("groupName"));

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subject");
            oidcUser = qm.persist(oidcUser);

            OidcGroup group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            Team team = new Team();
            team.setName("teamName");
            team = qm.persist(team);

            final MappedOidcGroup mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(team);
            qm.persist(mappedGroup);

            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
            assertThat(oidcUser.getTeams()).hasSize(1);
            assertThat(oidcUser.getTeams().get(0).getName()).isEqualTo("teamName");
        }
    }

    @Test
    public void synchronizeTeamsShouldRemoveOutdatedTeamMemberships() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn("teams");

        final OidcUserInfo userInfo = new OidcUserInfo();
        userInfo.setClaim("teams", Collections.emptyList());

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subject");
            oidcUser = qm.persist(oidcUser);

            OidcGroup group = new OidcGroup();
            group.setName("groupName");
            group = qm.persist(group);

            Team team = new Team();
            team.setName("teamName");
            team.setOidcUsers(Collections.singletonList(oidcUser));
            team = qm.persist(team);

            final MappedOidcGroup mappedGroup = new MappedOidcGroup();
            mappedGroup.setGroup(group);
            mappedGroup.setTeam(team);
            qm.persist(mappedGroup);

            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
            assertThat(oidcUser.getTeams()).isNullOrEmpty();
        }
    }

    @Test
    public void synchronizeTeamsShouldRemoveMembershipsOfUnmappedTeams() {
        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
                .thenReturn("teams");

        final OidcUserInfo userInfo = new OidcUserInfo();
        userInfo.setClaim("teams", Collections.singletonList("groupName"));

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subject");
            oidcUser = qm.persist(oidcUser);

            Team team = new Team();
            team.setName("teamName");
            team.setOidcUsers(Collections.singletonList(oidcUser));
            qm.persist(team);

            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);

            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
            assertThat(oidcUser.getTeams()).isNullOrEmpty();
        }
    }

}
