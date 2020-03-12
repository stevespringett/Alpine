package alpine.auth;

import alpine.Config;
import alpine.model.OidcUser;
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
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    public WireMockRule wireMockRule = new WireMockRule();

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
            existingUser.setEmail("subject@mail.local");
            existingUser = qm.persist(existingUser);
        }

        final OidcUser authenticatedUser = (OidcUser) authService.authenticate();
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(authenticatedUser.getUsername()).isEqualTo(existingUser.getUsername());
        assertThat(authenticatedUser.getEmail()).isEqualTo(existingUser.getEmail());
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
        assertThat(provisionedUser.getEmail()).isEqualTo("subject@mail.local");
    }

}
