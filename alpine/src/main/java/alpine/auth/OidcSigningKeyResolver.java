package alpine.auth;

import alpine.cache.CacheManager;
import alpine.logging.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import org.apache.commons.codec.binary.Base64;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 * A {@link SigningKeyResolver} that resolves signing keys from a remote authorization server.
 *
 * @since 1.8.0
 */
public class OidcSigningKeyResolver implements SigningKeyResolver {

    private static final Logger LOGGER = Logger.getLogger(OidcSigningKeyResolver.class);

    private final OidcConfiguration oidcConfiguration;

    public OidcSigningKeyResolver(final OidcConfiguration oidcConfiguration) {
        this.oidcConfiguration = oidcConfiguration;
    }

    @Override
    public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
        return resolveSigningKey(SignatureAlgorithm.forName(header.getAlgorithm()), header.getKeyId());
    }

    @Override
    public Key resolveSigningKey(final JwsHeader header, final String plaintext) {
        return resolveSigningKey(SignatureAlgorithm.forName(header.getAlgorithm()), header.getKeyId());
    }

    private Key resolveSigningKey(final SignatureAlgorithm sigAlg, final String keyId) {
        // TODO: Potentially sanitize key ID before using it in cache key
        final String cacheKey = String.format("OIDC_SIGNINGKEY_%s", keyId);

        // FIXME: Find a generic way to retrieve and store public keys in cache
        // The Cache only uses effective classes, so we can't just .get(PublicKey.class, cacheKey) here
        RSAPublicKeyImpl signingKey = CacheManager.getInstance().get(RSAPublicKeyImpl.class, cacheKey);

        if (signingKey != null) {
            LOGGER.info(String.format("Signing key for alg %s and key ID %s loaded from cache", sigAlg, keyId));
            return signingKey;
        }

        LOGGER.info(String.format("Resolving signing key for alg %s and key ID %s", sigAlg, keyId));
        final JwkSet jwkSet = ClientBuilder.newClient().target(oidcConfiguration.getJwksUri())
                .request(MediaType.APPLICATION_JSON)
                .get(JwkSet.class);

        final Jwk signingJwk = jwkSet.getKeys().stream()
                .filter(jwk -> "sig".equals(jwk.getUse()))
                .filter(jwk -> sigAlg.getValue().equals(jwk.getAlgorithm()))
                .filter(jwk -> keyId.equals(jwk.getKeyId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(String.format("No key for alg %s and key ID %s found", sigAlg, keyId)));

        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(sigAlg.getFamilyName());
            final BigInteger modulus = new BigInteger(1, Base64.decodeBase64(signingJwk.getModulus()));
            final BigInteger exponent = new BigInteger(1, Base64.decodeBase64(signingJwk.getExponent()));
            signingKey = (RSAPublicKeyImpl) keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));

            LOGGER.info(String.format("Storing signing key for alg %s and key ID %s in cache", sigAlg, keyId));
            CacheManager.getInstance().put(cacheKey, signingKey);

            return signingKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

}