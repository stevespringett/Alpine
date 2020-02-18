package alpine.auth;

import alpine.cache.CacheManager;
import alpine.crypto.EllipticCurve;
import alpine.logging.Logger;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

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

    private Key resolveSigningKey(final SignatureAlgorithm signatureAlgorithm, final String keyId) {
        final Optional<PublicKey> cachedSigningKey = loadSigningKeyFromCache(signatureAlgorithm, keyId);
        if (cachedSigningKey.isPresent()) {
            LOGGER.info(String.format("Signing key for alg %s and key ID %s loaded from cache", signatureAlgorithm, keyId));
            return cachedSigningKey.get();
        }

        LOGGER.info(String.format("Resolving signing key for alg %s and key ID %s", signatureAlgorithm, keyId));
        final JwkSet jwkSet;
        try {
            jwkSet = ClientBuilder.newClient().target(oidcConfiguration.getJwksUri())
                    .request(MediaType.APPLICATION_JSON)
                    .get(JwkSet.class);
        } catch (WebApplicationException | ProcessingException e) {
            // TODO: Exception handling
            throw new RuntimeException(e);
        }

        final Jwk signingJwk = jwkSet.getKeys().stream()
                .filter(jwk -> "sig".equals(jwk.getUse()))
                .filter(jwk -> signatureAlgorithm.getValue().equals(jwk.getAlgorithm()))
                .filter(jwk -> keyId.equals(jwk.getKeyId()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException(String.format("No key for alg %s and key ID %s found", signatureAlgorithm, keyId)));

        final KeyFactory keyFactory = getKeyFactory(signatureAlgorithm);
        final KeySpec keySpec = getKeySpec(signingJwk, signatureAlgorithm);

        try {
            final PublicKey signingKey = keyFactory.generatePublic(keySpec);

            LOGGER.info(String.format("Storing signing key for alg %s and key ID %s in cache", signatureAlgorithm, keyId));
            CacheManager.getInstance().put(getCacheKey(keyId), signingKey);

            return signingKey;
        } catch (InvalidKeySpecException e) {
            // TODO: Exception handling
            throw new RuntimeException(e);
        }
    }

    private Optional<PublicKey> loadSigningKeyFromCache(final SignatureAlgorithm sigAlg, final String keyId) {
        final PublicKey publicKey;

        if (sigAlg.isRsa()) {
            publicKey = CacheManager.getInstance().get(BCRSAPublicKey.class, getCacheKey(keyId));
        } else if (sigAlg.isEllipticCurve()) {
            publicKey = CacheManager.getInstance().get(BCECPublicKey.class, getCacheKey(keyId));
        } else {
            throw new UnsupportedOperationException(String.format("Signature algorithm %s is not supported", sigAlg.getValue()));
        }

        return Optional.ofNullable(publicKey);
    }

    private String getCacheKey(final String keyId) {
        return String.format("OIDC_SIGNINGKEY_%s", keyId);
    }

    /**
     * @param signatureAlgorithm Algorithm to get a {@link KeyFactory} for
     * @return A {@link KeyFactory} for the given algorithm
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyFactory">KeyFactory
     *         Algorithms</a>
     */
    private KeyFactory getKeyFactory(final SignatureAlgorithm signatureAlgorithm) {
        try {
            if (signatureAlgorithm.isRsa()) {
                return KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            } else if (signatureAlgorithm.isEllipticCurve()) {
                return KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            } else {
                throw new UnsupportedOperationException(String.format("Signature algorithm %s is not supported", signatureAlgorithm.getValue()));
            }
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            // TODO: Exception handling
            throw new RuntimeException(e);
        }
    }

    private KeySpec getKeySpec(final Jwk jwk, final SignatureAlgorithm signatureAlgorithm) {
        if (signatureAlgorithm.isRsa()) {
            return new RSAPublicKeySpec(jwk.getModulus(), jwk.getExponent());
        } else if (signatureAlgorithm.isEllipticCurve()) {
            final EllipticCurve ellipticCurve = EllipticCurve.forJwkName(jwk.getCurve());
            final ECPoint ecPoint = new ECPoint(jwk.getX(), jwk.getY());

            try {
                return new ECPublicKeySpec(ecPoint, ellipticCurve.getParameterSpec());
            } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
                // TODO: Exception handling
                throw new IllegalStateException(e);
            }
        } else {
            throw new UnsupportedOperationException(String.format("Signature algorithm %s is not supported", signatureAlgorithm.getValue()));
        }
    }

}
