package alpine.auth;

import alpine.cache.CacheManager;
import alpine.logging.Logger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.io.IOException;
import java.text.ParseException;

/**
 * @since 1.10.0
 */
class OidcIdTokenAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(OidcIdTokenAuthenticator.class);
    static final String JWK_SET_CACHE_KEY = "OIDC_JWK_SET";

    private final OidcConfiguration configuration;
    private final String clientId;

    OidcIdTokenAuthenticator(final OidcConfiguration configuration, final String clientId) {
        this.configuration = configuration;
        this.clientId = clientId;
    }

    OidcProfile authenticate(final String idToken, final OidcProfileCreator profileCreator) throws AlpineAuthenticationException {
        final SignedJWT parsedIdToken;
        try {
            parsedIdToken = SignedJWT.parse(idToken);
        } catch (ParseException e) {
            LOGGER.error("Parsing ID token failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final JWKSet jwkSet;
        try {
            jwkSet = resolveJwkSet();
        } catch (IOException | ParseException e) {
            LOGGER.error("Resolving JWK set failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final var idTokenValidator = new IDTokenValidator(
                new Issuer(configuration.getIssuer()), new ClientID(clientId),
                parsedIdToken.getHeader().getAlgorithm(), jwkSet);

        final IDTokenClaimsSet claimsSet;
        try {
            claimsSet = idTokenValidator.validate(parsedIdToken, null);
            LOGGER.debug("ID token claims: " + claimsSet.toJSONString());
        } catch (BadJOSEException | JOSEException e) {
            LOGGER.error("ID token validation failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        return profileCreator.create(claimsSet);
    }

    JWKSet resolveJwkSet() throws IOException, ParseException {
        JWKSet jwkSet = CacheManager.getInstance().get(JWKSet.class, JWK_SET_CACHE_KEY);
        if (jwkSet != null) {
            LOGGER.debug("JWK set loaded from cache");
            return jwkSet;
        }

        LOGGER.debug("Fetching JWK set from " + configuration.getJwksUri());
        jwkSet = JWKSet.load(configuration.getJwksUri().toURL());

        LOGGER.debug("Storing JWK set in cache");
        CacheManager.getInstance().put(JWK_SET_CACHE_KEY, jwkSet);
        return jwkSet;
    }

}
