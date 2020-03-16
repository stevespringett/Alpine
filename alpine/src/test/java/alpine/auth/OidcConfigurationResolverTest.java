package alpine.auth;

import alpine.cache.CacheManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class OidcConfigurationResolverTest {

    private static final String OIDC_CONFIGURATION_PATH = "/.well-known/openid-configuration";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @After
    public void tearDown() {
        // Remove configs from cache to keep testing environment clean
        CacheManager.getInstance().remove(OidcConfiguration.class, OidcConfigurationResolver.CONFIGURATION_CACHE_KEY);
    }

    @Test
    public void resolveShouldReturnNullWhenDiscoveryUriIsNull() {
        assertThat(new OidcConfigurationResolver(null).resolve()).isNull();
    }

    @Test
    public void resolveShouldReturnCachedValueWhenAvailable() {
        final OidcConfiguration cachedConfiguration = new OidcConfiguration();
        CacheManager.getInstance().put(OidcConfigurationResolver.CONFIGURATION_CACHE_KEY, cachedConfiguration);

        assertThat(new OidcConfigurationResolver(wireMockRule.url(OIDC_CONFIGURATION_PATH)).resolve()).isEqualTo(cachedConfiguration);
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithNon200StatusCode() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)));

        assertThat(new OidcConfigurationResolver(wireMockRule.url(OIDC_CONFIGURATION_PATH)).resolve()).isNull();
        verify(getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithInvalidJson() {
        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("<?xml version=\"1.0\" ?>")));

        assertThat(new OidcConfigurationResolver(wireMockRule.url(OIDC_CONFIGURATION_PATH)).resolve()).isNull();
        verify(getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnConfigurationAndStoreItInCache() {
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

        final OidcConfiguration oidcConfiguration = new OidcConfigurationResolver(wireMockRule.url(OIDC_CONFIGURATION_PATH)).resolve();
        assertThat(oidcConfiguration).isNotNull();
        assertThat(oidcConfiguration.getIssuer()).isEqualTo("https://gitlab.com");
        assertThat(oidcConfiguration.getAuthorizationEndpointUri()).isEqualTo("https://gitlab.com/oauth/authorize");
        assertThat(oidcConfiguration.getTokenEndpointUri()).isEqualTo("https://gitlab.com/oauth/token");
        assertThat(oidcConfiguration.getUserInfoEndpointUri()).isEqualTo("https://gitlab.com/oauth/userinfo");
        assertThat(oidcConfiguration.getJwksUri()).isEqualTo("https://gitlab.com/oauth/discovery/keys");

        // On the next invocation, the configuration should be loaded from cache
        assertThat(new OidcConfigurationResolver(wireMockRule.url(OIDC_CONFIGURATION_PATH)).resolve()).isEqualTo(oidcConfiguration);

        // Only one request should've been made
        verify(1, getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

}