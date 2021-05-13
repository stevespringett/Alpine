package alpine.auth;

import alpine.cache.CacheManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.HttpStatus;
import wiremock.org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class OidcIdTokenAuthenticatorTest {

    private static final String CLIENT_ID = "clientId";
    private static final String SUBJECT_CLAIM = "subject";
    private static final String USERNAME_CLAIM = "username";
    private static final String USERNAME_CLAIM_NAME = "username";
    private static final List<String> TEAMS_CLAIM = List.of("team1", "team2");
    private static final String TEAMS_CLAIM_NAME = "teams";
    private static final String EMAIL_CLAIM = "username@example.com";
    private static final String EMAIL_CLAIM_NAME = "email";
    private static final OidcProfileCreator PROFILE_CREATOR = claims -> {
        final var profile = new OidcProfile();
        profile.setSubject(claims.getStringClaim(IDTokenClaimsSet.SUB_CLAIM_NAME));
        profile.setUsername(claims.getStringClaim(USERNAME_CLAIM_NAME));
        profile.setTeams(claims.getStringListClaim(TEAMS_CLAIM_NAME));
        profile.setEmail(claims.getStringClaim(EMAIL_CLAIM_NAME));
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
    public void authenticateShouldReturnOidcProfile() throws JOSEException, URISyntaxException, AlpineAuthenticationException {

        final RSAKey jwk = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID(UUID.randomUUID().toString()).generate();
        final JWKSet jwkSet = new JWKSet(jwk.toPublicJWK());

        // Construct a JWS object with all required claims
        final JWSObject jws = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwk.getKeyID()).build(),
                new Payload(Map.of(
                        IDTokenClaimsSet.SUB_CLAIM_NAME, SUBJECT_CLAIM,
                        IDTokenClaimsSet.AUD_CLAIM_NAME, CLIENT_ID,
                        IDTokenClaimsSet.ISS_CLAIM_NAME, wireMockRule.baseUrl(),
                        IDTokenClaimsSet.EXP_CLAIM_NAME, LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault()).toEpochSecond(),
                        IDTokenClaimsSet.IAT_CLAIM_NAME, LocalDateTime.now().minusMinutes(1).atZone(ZoneId.systemDefault()).toEpochSecond(),
                        USERNAME_CLAIM_NAME, USERNAME_CLAIM,
                        TEAMS_CLAIM_NAME, TEAMS_CLAIM,
                        EMAIL_CLAIM_NAME, EMAIL_CLAIM
                ))
        );

        // Sign the object
        final JWSSigner signer = new RSASSASigner(jwk);
        jws.sign(signer);

        // Make the JWK set available
        wireMockRule.stubFor(get(urlPathEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(jwkSet.toString())));

        oidcConfiguration.setIssuer(wireMockRule.baseUrl());
        oidcConfiguration.setJwksUri(new URI(wireMockRule.url("/jwks")));

        final var authenticator = new OidcIdTokenAuthenticator(oidcConfiguration, CLIENT_ID);

        final OidcProfile profile = authenticator.authenticate(jws.serialize(), PROFILE_CREATOR);
        assertThat(profile.getSubject()).isEqualTo(SUBJECT_CLAIM);
        assertThat(profile.getUsername()).isEqualTo(USERNAME_CLAIM);
        assertThat(profile.getTeams()).isEqualTo(TEAMS_CLAIM);
        assertThat(profile.getEmail()).isEqualTo(EMAIL_CLAIM);
    }

    @Test
    public void resolveJwkSetShouldReturnCachedValueWhenAvailable() throws IOException, ParseException {
        final JWKSet cachedJwkSet = new JWKSet();
        CacheManager.getInstance().put(OidcIdTokenAuthenticator.JWK_SET_CACHE_KEY, cachedJwkSet);

        final var authenticator = new OidcIdTokenAuthenticator(oidcConfiguration, CLIENT_ID);
        assertThat(authenticator.resolveJwkSet()).isEqualTo(cachedJwkSet);
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

        oidcConfiguration.setJwksUri(new URI(wireMockRule.url("/jwks")));

        final var authenticator = new OidcIdTokenAuthenticator(oidcConfiguration, CLIENT_ID);

        // Resolve and verify JWK set
        final JWKSet jwkSet = authenticator.resolveJwkSet();
        assertThat(jwkSet.getKeys()).hasSize(2);
        assertThat(jwkSet.getKeyByKeyId("1").getKeyType()).isEqualTo(KeyType.EC);
        assertThat(jwkSet.getKeyByKeyId("2011-04-29").getKeyType()).isEqualTo(KeyType.RSA);

        // On the next invocation, the JWK set should be loaded from cache
        assertThat(authenticator.resolveJwkSet()).isEqualTo(jwkSet);

        // Only one request should've been made
        verify(1, getRequestedFor(urlPathEqualTo("/jwks")));
    }

}