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

    private static final String OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @After
    public void tearDown() {
        // Remove configs from cache to keep testing environment clean
        CacheManager.getInstance().remove(OidcConfiguration.class, OidcConfigurationResolver.CONFIGURATION_CACHE_KEY);
    }

    @Test
    public void resolveShouldReturnNullWhenOidcIsNotEnabled() {
        assertThat(new OidcConfigurationResolver(false, wireMockRule.baseUrl()).resolve()).isNull();
    }

    @Test
    public void resolveShouldReturnNullWhenAuthorityIsNull() {
        assertThat(new OidcConfigurationResolver(true, null).resolve()).isNull();
    }

    @Test
    public void resolveShouldReturnCachedValueWhenAvailable() {
        final OidcConfiguration cachedConfiguration = new OidcConfiguration();
        CacheManager.getInstance().put(OidcConfigurationResolver.CONFIGURATION_CACHE_KEY, cachedConfiguration);

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isEqualTo(cachedConfiguration);
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithNon200StatusCode() {
        wireMockRule.stubFor(get(urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)));

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isNull();
        verify(getRequestedFor(urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnNullWhenServerRespondsWithInvalidJson() {
        wireMockRule.stubFor(get(urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("<?xml version=\"1.0\" ?>")));

        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isNull();
        verify(getRequestedFor(urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

    @Test
    public void resolveShouldReturnConfigurationAndStoreItInCache() {
        wireMockRule.stubFor(get(urlPathEqualTo(OPENID_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{\n" +
                                "  \"issuer\": \"" + wireMockRule.baseUrl() + "\",\n" +
                                "  \"userinfo_endpoint\": \"" + wireMockRule.baseUrl() + "/protocol/openid-connect/userinfo\",\n" +
                                "  \"jwks_uri\": \"" + wireMockRule.baseUrl() + "/protocol/openid-connect/certs\",\n" +
                                "  \"subject_types_supported\": [\"public\",\"pairwise\"]" +
                                "}")));

        final OidcConfiguration oidcConfiguration = new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve();
        assertThat(oidcConfiguration).isNotNull();
        assertThat(oidcConfiguration.getIssuer()).isEqualTo(wireMockRule.baseUrl());
        assertThat(oidcConfiguration.getUserInfoEndpointUri()).isEqualTo(wireMockRule.baseUrl() + "/protocol/openid-connect/userinfo");
        assertThat(oidcConfiguration.getJwksUri()).isEqualTo(wireMockRule.baseUrl() + "/protocol/openid-connect/certs");

        // On the next invocation, the configuration should be loaded from cache
        assertThat(new OidcConfigurationResolver(true, wireMockRule.baseUrl()).resolve()).isEqualTo(oidcConfiguration);

        // Only one request should've been made
        verify(1, getRequestedFor(urlPathEqualTo(OPENID_CONFIGURATION_PATH)));
    }

}