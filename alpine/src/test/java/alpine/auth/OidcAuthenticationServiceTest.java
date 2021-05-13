package alpine.auth;

import alpine.Config;
import alpine.cache.CacheManager;
import alpine.model.MappedOidcGroup;
import alpine.model.OidcGroup;
import alpine.model.OidcUser;
import alpine.model.Team;
import alpine.persistence.AlpineQueryManager;
import alpine.util.TestUtil;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticationServiceTest {

    private static final String OIDC_USERINFO_PATH = "/userinfo";
    private static final String USERNAME_CLAIM = "usernameClaim";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String ID_TOKEN = "idToken";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private Config configMock;
    private OidcConfiguration oidcConfigurationMock;

    @BeforeClass
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @Before
    public void setUp() throws URISyntaxException {
        configMock = mock(Config.class);
        oidcConfigurationMock = mock(OidcConfiguration.class);

        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_USERNAME_CLAIM)))
                .thenReturn(USERNAME_CLAIM);

        when(oidcConfigurationMock.getUserInfoEndpointUri())
                .thenReturn(new URI(wireMockRule.url(OIDC_USERINFO_PATH)));
    }

    @After
    @SuppressWarnings("unchecked")
    public void tearDown() throws Exception {
        TestUtil.resetInMemoryDatabase();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(false);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, ID_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenAccessTokenAndIdTokenIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, null);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcConfigurationIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, null, ACCESS_TOKEN, ID_TOKEN);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndOidcConfigurationIsNotNullAndAccessTokenIsNotNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, ID_TOKEN);

        assertThat(authService.isSpecified()).isTrue();
    }

    @Test
    public void authenticateShouldThrowExceptionWhenRequestingUserInfoWithInvalidToken() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

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

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

    @Test
    public void authenticateShouldThrowExceptionWhenUserInfoResponseProcessingFailed() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("{}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

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

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, ID_TOKEN);

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
                                "    \"" + USERNAME_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

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
    public void authenticateShouldSynchronizeTeamsWhenUserAlreadyExistsAndTeamSynchronizationIsEnabled() throws AlpineAuthenticationException {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)))
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
                                "    \"" + USERNAME_CLAIM + "\": \"username\", " +
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

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

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
                                "    \"" + USERNAME_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

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
                                "    \"" + USERNAME_CLAIM + "\": \"username\"" +
                                "}")));

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

        final OidcUser provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("subject@mail.local");
        assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldAssignSubjectIdAndEmailWhenUserAlreadyExistsAndAuthenticatesForFirstTime() throws AlpineAuthenticationException {
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
                                "    \"" + USERNAME_CLAIM + "\": \"username\"" +
                                "}")));

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createOidcUser("username");
        }

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

        final OidcUser provisionedUser = (OidcUser) authService.authenticate();
        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getUsername()).isEqualTo("username");
        assertThat(provisionedUser.getSubjectIdentifier()).isEqualTo("subject");
        assertThat(provisionedUser.getEmail()).isEqualTo("subject@mail.local");
        assertThat(provisionedUser.getTeams()).isNullOrEmpty();
        assertThat(provisionedUser.getPermissions()).isNullOrEmpty();
    }

    @Test
    public void authenticateShouldThrowExceptionWhenUserAlreadyExistsAndSubjectIdentifierHasChanged() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_USER_PROVISIONING)))
                .thenReturn(true);

        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_USERINFO_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "    \"sub\": \"changedSubject\", " +
                                "    \"email\": \"subject@mail.local\", " +
                                "    \"" + USERNAME_CLAIM + "\": \"username\"" +
                                "}")));

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser existingUser = new OidcUser();
            existingUser.setUsername("username");
            existingUser.setSubjectIdentifier("subject");
            existingUser.setEmail("subject@mail.local");
            qm.persist(existingUser);
        }

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN, null);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(authService::authenticate)
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

//    @Test
//    public void synchronizeTeamsShouldReturnUserWhenNoTeamsClaimWasConfigured() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn(null);
//
//        final OidcUser oidcUser = new OidcUser();
//
//        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//        assertThat(authService.synchronizeTeams(null, oidcUser, null)).isEqualTo(oidcUser);
//    }

//    @Test
//    public void synchronizeTeamsShouldReturnUserWhenTeamsClaimIsNotAList() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn("teams");
//
//        final OidcUserInfo userInfo = new OidcUserInfo();
//        userInfo.setClaim("teams", "not-a-list");
//
//        final OidcUser oidcUser = new OidcUser();
//
//        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//        assertThat(authService.synchronizeTeams(null, oidcUser, userInfo)).isEqualTo(oidcUser);
//    }

//    @Test
//    public void synchronizeTeamsShouldReturnUserWhenTeamsClaimDoesNotExist() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn("teams");
//
//        final OidcUserInfo userInfo = new OidcUserInfo();
//
//        final OidcUser oidcUser = new OidcUser();
//
//        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//        assertThat(authService.synchronizeTeams(null, oidcUser, userInfo)).isEqualTo(oidcUser);
//    }

//    @Test
//    public void synchronizeTeamsShouldAddNewTeamMemberships() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn("teams");
//
//        final OidcUserInfo userInfo = new OidcUserInfo();
//        userInfo.setClaim("teams", Collections.singletonList("groupName"));
//
//        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
//            OidcUser oidcUser = new OidcUser();
//            oidcUser.setUsername("username");
//            oidcUser.setSubjectIdentifier("subject");
//            oidcUser = qm.persist(oidcUser);
//
//            OidcGroup group = new OidcGroup();
//            group.setName("groupName");
//            group = qm.persist(group);
//
//            Team team = new Team();
//            team.setName("teamName");
//            team = qm.persist(team);
//
//            final MappedOidcGroup mappedGroup = new MappedOidcGroup();
//            mappedGroup.setGroup(group);
//            mappedGroup.setTeam(team);
//            qm.persist(mappedGroup);
//
//            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
//            assertThat(oidcUser.getTeams()).hasSize(1);
//            assertThat(oidcUser.getTeams().get(0).getName()).isEqualTo("teamName");
//        }
//    }

//    @Test
//    public void synchronizeTeamsShouldRemoveOutdatedTeamMemberships() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn("teams");
//
//        final OidcUserInfo userInfo = new OidcUserInfo();
//        userInfo.setClaim("teams", Collections.emptyList());
//
//        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
//            OidcUser oidcUser = new OidcUser();
//            oidcUser.setUsername("username");
//            oidcUser.setSubjectIdentifier("subject");
//            oidcUser = qm.persist(oidcUser);
//
//            OidcGroup group = new OidcGroup();
//            group.setName("groupName");
//            group = qm.persist(group);
//
//            Team team = new Team();
//            team.setName("teamName");
//            team.setOidcUsers(Collections.singletonList(oidcUser));
//            team = qm.persist(team);
//
//            final MappedOidcGroup mappedGroup = new MappedOidcGroup();
//            mappedGroup.setGroup(group);
//            mappedGroup.setTeam(team);
//            qm.persist(mappedGroup);
//
//            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
//            assertThat(oidcUser.getTeams()).isNullOrEmpty();
//        }
//    }

//    @Test
//    public void synchronizeTeamsShouldRemoveMembershipsOfUnmappedTeams() {
//        when(configMock.getProperty(eq(Config.AlpineKey.OIDC_TEAMS_CLAIM)))
//                .thenReturn("teams");
//
//        final OidcUserInfo userInfo = new OidcUserInfo();
//        userInfo.setClaim("teams", Collections.singletonList("groupName"));
//
//        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
//            OidcUser oidcUser = new OidcUser();
//            oidcUser.setUsername("username");
//            oidcUser.setSubjectIdentifier("subject");
//            oidcUser = qm.persist(oidcUser);
//
//            Team team = new Team();
//            team.setName("teamName");
//            team.setOidcUsers(Collections.singletonList(oidcUser));
//            qm.persist(team);
//
//            final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, ACCESS_TOKEN);
//
//            oidcUser = authService.synchronizeTeams(qm, oidcUser, userInfo);
//            assertThat(oidcUser.getTeams()).isNullOrEmpty();
//        }
//    }

    @Test
    public void resolveJwkSetShouldReturnCachedValueWhenAvailable() throws IOException, ParseException {
        final JWKSet cachedJwkSet = new JWKSet();
        CacheManager.getInstance().put(OidcAuthenticationService.JWK_SET_CACHE_KEY, cachedJwkSet);

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, null);

        assertThat(authService.resolveJwkSet()).isEqualTo(cachedJwkSet);
    }

    @Test
    public void resolveJwkSetShouldReturnJwkSetAndStoreItInCache() throws URISyntaxException, IOException, ParseException {
        wireMockRule.stubFor(get(urlPathEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        // This JWK set is taken from https://datatracker.ietf.org/doc/html/rfc7517#appendix-A.1
                        .withBody("" +
                                "{\n" +
                                "  \"keys\": [\n" +
                                "    {\n" +
                                "      \"kty\": \"EC\",\n" +
                                "      \"crv\": \"P-256\",\n" +
                                "      \"x\": \"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n" +
                                "      \"y\": \"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" +
                                "      \"use\": \"enc\",\n" +
                                "      \"kid\": \"1\"\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"kty\": \"RSA\",\n" +
                                "      \"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
                                "      \"e\": \"AQAB\",\n" +
                                "      \"alg\": \"RS256\",\n" +
                                "      \"kid\": \"2011-04-29\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")));

        when(oidcConfigurationMock.getJwksUri())
                .thenReturn(new URI(wireMockRule.url("/jwks")));

        final var authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null, null);

        // Resolve and verify JWK set
        final JWKSet jwkSet = authService.resolveJwkSet();
        assertThat(jwkSet.getKeys()).hasSize(2);
        assertThat(jwkSet.getKeyByKeyId("1").getKeyType()).isEqualTo(KeyType.EC);
        assertThat(jwkSet.getKeyByKeyId("2011-04-29").getKeyType()).isEqualTo(KeyType.RSA);

        // On the next invocation, the JWK set should be loaded from cache
        assertThat(authService.resolveJwkSet()).isEqualTo(jwkSet);

        // Only one request should've been made
        verify(1, getRequestedFor(urlPathEqualTo("/jwks")));
    }

}
