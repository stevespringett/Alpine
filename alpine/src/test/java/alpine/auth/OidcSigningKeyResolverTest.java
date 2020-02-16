package alpine.auth;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sun.security.rsa.RSAPublicKeyImpl;
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

    private static final String KEY_ID = "kewiQq9jiC84CvSsJYOB-N6A8WFLSV20Mb-y7IlWDSQ";

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
                                "      \"kty\": \"RSA\",\n" +
                                "      \"kid\": \"kewiQq9jiC84CvSsJYOB-N6A8WFLSV20Mb-y7IlWDSQ\",\n" +
                                "      \"e\": \"AQAB\",\n" +
                                "      \"n\": \"5RyvCSgBoOGNE03CMcJ9Bzo1JDvsU8XgddvRuJtdJAIq5zJ8fiUEGCnMfAZI4of36YXBuBalIycqkgxrRkSOENRUCWN45bf8xsQCcQ8zZxozu0St4w5S-aC7N7UTTarPZTp4BZH8ttUm-VnK4aEdMx9L3Izo0hxaJ135undTuA6gQpK-0nVsm6tRVq4akDe3OhC-7b2h6z7GWJX1SD4sAD3iaq4LZa8y1mvBBz6AIM9co8R-vU1_CduxKQc3KxCnqKALbEKXm0mTGsXha9aNv3pLNRNs_J-cCjBpb1EXAe_7qOURTiIHdv8_sdjcFTJ0OTeLWywuSf7mD0Wpx2LKcD6ImENbyq5IBuR1e2ghnh5Y9H33cuQ0FRni8ikq5W3xP3HSMfwlayhIAJN_WnmbhENRU-m2_hDPiD9JYF2CrQneLkE3kcazSdtarPbg9ZDiydHbKWCV-X7HxxIKEr9N7P1V5HKatF4ZUrG60e3eBnRyccPwmT66i9NYyrcy1_ZNN8D1DY8xh9kflUDy4dSYu4R7AEWxNJWQQov525v0MjD5FNAS03rpk4SuW3Mt7IP73m-_BpmIhW3LZsnmfd8xHRjf0M9veyJD0--ETGmh8t3_CXh3I3R9IbcSEntUl_2lCvc_6B-m8W-t2nZr4wvOq9-iaTQXAn1Au6EaOYWvDRE\",\n" +
                                "      \"use\": \"sig\",\n" +
                                "      \"alg\": \"RS256\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")));
    }

    @Test
    public void shouldResolveMatchingSigningKeyAndStoreItInCache() {
        final DefaultJwsHeader header = new DefaultJwsHeader();
        header.setAlgorithm("RS256");
        header.setKeyId(KEY_ID);

        final Key key = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
        assertThat(key).isInstanceOf(RSAPublicKeyImpl.class);

        // Verify that the key was cached and not requested twice
        final Key secondKey = signingKeyResolver.resolveSigningKey(header, "");
        assertThat(secondKey).isEqualTo(key);
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(OIDC_CONFIGURATION_PATH)));
    }

    @Test
    public void shouldThrowExceptionWhenNoMatchingSigningKeyCanBeFound() {
        final DefaultJwsHeader header = new DefaultJwsHeader();
        header.setAlgorithm("RS256");
        header.setKeyId("another_key_id");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> signingKeyResolver.resolveSigningKey(header, ""));
    }

}