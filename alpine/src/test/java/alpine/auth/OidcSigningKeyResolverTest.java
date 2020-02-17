package alpine.auth;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import java.security.Key;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OidcSigningKeyResolverTest {

    private static final String KEY_ID_RSA = "NonwCYtciyjpcpoVssnDyg6N-ImnQ2vvLg7tSghSBpo";

    private static final String KEY_ID_EC = "uK_CfONSQO2SB9FCUOPRv4g8k2uAYmDDnXUy15CEqOs";

    private static final String OIDC_CONFIGURATION_PATH = "/oidc-config";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Mock
    private OidcConfiguration oidcConfigurationMock;

    @InjectMocks
    private OidcSigningKeyResolver signingKeyResolver;

    @Before
    public void setUp() {
        when(oidcConfigurationMock.getJwksUri())
                .thenReturn(wireMockRule.url(OIDC_CONFIGURATION_PATH));

        wireMockRule.stubFor(get(urlPathEqualTo(OIDC_CONFIGURATION_PATH))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody("" +
                                "{\n" +
                                "  \"keys\": [\n" +
                                "    {\n" +
                                "      \"kid\": \"NonwCYtciyjpcpoVssnDyg6N-ImnQ2vvLg7tSghSBpo\",\n" +
                                "      \"kty\": \"RSA\",\n" +
                                "      \"alg\": \"RS256\",\n" +
                                "      \"use\": \"sig\",\n" +
                                "      \"n\": \"hsuiIdOIbaO81QXIvWZAeo3tuqlDO8TsJGUBWaNtIQcloPtNFG4CUTxsdeUUhD6R_n8H95OZ2aGKFp42vzLgcpynBqVnK4Dw_cwQ0cSUjTqNhlOXA5koBKWJ_HRXQu5Cw7FQjty9CH90ytU1aVh-A9Ns7p5NVqeQu1PC8EPaui-IPcJ6lon1GomfQI9DrsKYANwToHe6_pFqztlDKc8tG9XYcgbAsPooVvrdk7vPMtK4sciat6zslZaQCbQf9QXTSwPTI33sqZGJXJe0ZdrQtCYogwyXyZYH0gWncmqLyQn-VUfKwcn3Anx1Uwtn8C93pPMJS_iDt2ZgAEp3KXAR3w\",\n" +
                                "      \"e\": \"AQAB\"\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"kid\": \"uK_CfONSQO2SB9FCUOPRv4g8k2uAYmDDnXUy15CEqOs\",\n" +
                                "      \"kty\": \"EC\",\n" +
                                "      \"alg\": \"ES384\",\n" +
                                "      \"use\": \"sig\",\n" +
                                "      \"crv\": \"P-384\",\n" +
                                "      \"x\": \"RpKjA_K7HvyEyaj-PSHTnW0oJt1D9qI-WOlDy_BRaN01K38PpgELT3djZC8xioSz\",\n" +
                                "      \"y\": \"S5-Mq8SOCSuxgfr2jnrkN-E06o8StX0J7mtAMtNg1HaSPuzfdfXSCzr8Yw5ZXyLm\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")));
    }

    @Test
    public void shouldResolveMatchingRsaSigningKeyAndStoreItInCache() {
        final DefaultJwsHeader header = new DefaultJwsHeader();
        header.setAlgorithm("RS256");
        header.setKeyId(KEY_ID_RSA);

        final Key key = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
        assertThat(key).isInstanceOf(BCRSAPublicKey.class);

        // Verify that the key was cached and not requested twice
        final Key secondKey = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(secondKey).isEqualTo(key);
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void shouldResolveMatchingEcSigningKeyAndStoreItInCache() {
        final DefaultJwsHeader header = new DefaultJwsHeader();
        header.setAlgorithm("ES384");
        header.setKeyId(KEY_ID_EC);

        final Key key = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(key.getAlgorithm()).isEqualTo("EC");
        assertThat(key).isInstanceOf(BCECPublicKey.class);

        // Verify that the key was cached and not requested twice
        final Key secondKey = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(secondKey).isEqualTo(key);
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void shouldThrowExceptionWhenNoMatchingSigningKeyCouldBeFound() {
        final DefaultJwsHeader header = new DefaultJwsHeader();
        header.setAlgorithm("RS256");
        header.setKeyId("another_key_id");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> signingKeyResolver.resolveSigningKey(header, ""));
    }

}