package alpine.auth;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class OidcUserInfoAuthenticatorTest {

    private static final String USERNAME_CLAIM_NAME = "username";
    private static final String TEAMS_CLAIM_NAME = "groups";
    private static final OidcProfileCreator PROFILE_CREATOR = claims -> {
        final var profile = new OidcProfile();
        profile.setSubject(claims.getStringClaim(UserInfo.SUB_CLAIM_NAME));
        profile.setUsername(claims.getStringClaim(USERNAME_CLAIM_NAME));
        profile.setGroups(claims.getStringListClaim(TEAMS_CLAIM_NAME));
        profile.setEmail(claims.getStringClaim(UserInfo.EMAIL_CLAIM_NAME));
        return profile;
    };

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private OidcConfiguration oidcConfiguration;

    @Before
    public void setUp() {
        oidcConfiguration = new OidcConfiguration();
    }

    @Test
    public void authenticateShouldReturnOidcProfile() throws Exception {
        // Provide a UserInfo response with all required claims
        wireMockRule.stubFor(get(urlPathEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{" +
                                "  \"" + UserInfo.SUB_CLAIM_NAME + "\": \"subject\", " +
                                "  \"" + USERNAME_CLAIM_NAME + "\": \"username\", " +
                                "  \"" + TEAMS_CLAIM_NAME + "\": [\"group1\",\"group2\"],\n" +
                                "  \"" + UserInfo.EMAIL_CLAIM_NAME + "\": \"username@example.com\"" +
                                "}")));

        oidcConfiguration.setUserInfoEndpointUri(new URI(wireMockRule.url("/userinfo")));

        final var authenticator = new OidcUserInfoAuthenticator(oidcConfiguration);

        final OidcProfile profile = authenticator.authenticate("accessToken", PROFILE_CREATOR);
        assertThat(profile.getSubject()).isEqualTo("subject");
        assertThat(profile.getUsername()).isEqualTo("username");
        assertThat(profile.getGroups()).containsExactly("group1", "group2");
        assertThat(profile.getEmail()).isEqualTo("username@example.com");
    }

    @Test
    public void authenticateShouldThrowWhenUserInfoRequestFailed() throws Exception {
        // Simulate an error during the request
        wireMockRule.stubFor(get(urlPathEqualTo("/userinfo"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        oidcConfiguration.setUserInfoEndpointUri(new URI(wireMockRule.url("/userinfo")));

        final var authenticator = new OidcUserInfoAuthenticator(oidcConfiguration);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(() -> authenticator.authenticate("accessToken", PROFILE_CREATOR))
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowWhenParsingUserInfoResponseFailed() throws Exception {
        // Simulate a response with unparseable body
        wireMockRule.stubFor(get(urlPathEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType())
                        .withBody("<?xml version=\"1.0\"?>")));

        oidcConfiguration.setUserInfoEndpointUri(new URI(wireMockRule.url("/userinfo")));

        final var authenticator = new OidcUserInfoAuthenticator(oidcConfiguration);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(() -> authenticator.authenticate("accessToken", PROFILE_CREATOR))
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.OTHER));
    }

    @Test
    public void authenticateShouldThrowWhenUserInfoResponseIndicatesError() throws Exception {
        // Simulate a response indicating an invalid access token
        wireMockRule.stubFor(get(urlPathEqualTo("/userinfo"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)
                        .withHeader("WWW-Authenticate", "Bearer error=invalid_token")));

        oidcConfiguration.setUserInfoEndpointUri(new URI(wireMockRule.url("/userinfo")));

        final var authenticator = new OidcUserInfoAuthenticator(oidcConfiguration);

        assertThatExceptionOfType(AlpineAuthenticationException.class)
                .isThrownBy(() -> authenticator.authenticate("accessToken", PROFILE_CREATOR))
                .satisfies(exception -> assertThat(exception.getCauseType())
                        .isEqualTo(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS));
    }

}