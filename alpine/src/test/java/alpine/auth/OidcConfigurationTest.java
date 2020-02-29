package alpine.auth;

import alpine.cache.CacheManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class OidcConfigurationTest {

    private static final String OIDC_CONFIGURATION_PATH = "/.well-known/openid-configuration";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void getConfigurationShouldReturnNullWhenDiscoveryUriIsNull() {
        assertThat(OidcConfiguration.getConfiguration(null)).isNull();
    }

    @Test
    public void getConfigurationShouldReturnCachedValueWhenAvailable() {
        final OidcConfiguration cachedConfiguration = new OidcConfiguration();
        CacheManager.getInstance().put(OidcConfiguration.CONFIGURATION_CACHE_KEY, cachedConfiguration);

        assertThat(OidcConfiguration.getConfiguration(wireMockRule.url(OIDC_CONFIGURATION_PATH))).isEqualTo(cachedConfiguration);
    }

    @Test
    public void getConfigurationShouldReturnNullWhenServerRespondsWithNon200StatusCode() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)));

        assertThat(OidcConfiguration.getConfiguration(wireMockRule.url(OIDC_CONFIGURATION_PATH))).isNull();
        verify(getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void getConfigurationShouldReturnNullWhenServerRespondsWithInvalidJson() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("<?xml version=\"1.0\" ?>")));

        assertThat(OidcConfiguration.getConfiguration(wireMockRule.url(OIDC_CONFIGURATION_PATH))).isNull();
        verify(getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void getConfigurationShouldReturnConfigurationAndStoreItInCache() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{\n" +
                                "  \"issuer\": \"https://gitlab.com\",\n" +
                                "  \"authorization_endpoint\": \"https://gitlab.com/oauth/authorize\",\n" +
                                "  \"token_endpoint\": \"https://gitlab.com/oauth/token\",\n" +
                                "  \"userinfo_endpoint\": \"https://gitlab.com/oauth/userinfo\",\n" +
                                "  \"jwks_uri\": \"https://gitlab.com/oauth/discovery/keys\"\n" +
                                "}")));

        final OidcConfiguration oidcConfiguration = OidcConfiguration.getConfiguration(wireMockRule.url(OIDC_CONFIGURATION_PATH));
        assertThat(oidcConfiguration.getIssuer()).isEqualTo("https://gitlab.com");
        assertThat(oidcConfiguration.getAuthorizationEndpointUri()).isEqualTo("https://gitlab.com/oauth/authorize");
        assertThat(oidcConfiguration.getTokenEndpointUri()).isEqualTo("https://gitlab.com/oauth/token");
        assertThat(oidcConfiguration.getUserInfoEndpointUri()).isEqualTo("https://gitlab.com/oauth/userinfo");
        assertThat(oidcConfiguration.getJwksUri()).isEqualTo("https://gitlab.com/oauth/discovery/keys");

        // On the next invocation, the configuration should be loaded from cache
        assertThat(OidcConfiguration.getConfiguration(wireMockRule.url(OIDC_CONFIGURATION_PATH))).isEqualTo(oidcConfiguration);

        // Only one request should've been made
        verify(1, getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

}