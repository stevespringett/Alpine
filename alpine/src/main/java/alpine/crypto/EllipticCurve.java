package alpine.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @since 1.8.0
 */
public enum EllipticCurve {

    P_256("P-256", "secp256r1"),

    P_384("P-384", "secp384r1"),

    P_512("P-512", "secp521r1");

    /**
     * Name of the elliptic curve as used in JSON Web Keys.
     *
     * @see <a href="https://www.iana.org/assignments/jose/jose.xhtml#web-key-elliptic-curve">JSON Web Key Elliptic Curve</a>
     */
    private final String jwkName;

    /**
     * @see <a href="https://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269">Supported Curves by BouncyCastle</a>
     */
    private final String stdName;

    EllipticCurve(final String jwkName, final String stdName) {
        this.jwkName = jwkName;
        this.stdName = stdName;
    }

    String getJwkName() {
        return jwkName;
    }

    public static EllipticCurve forJwkName(final String name) {
        return Arrays.stream(values())
                .filter(ec -> ec.jwkName.equals(name))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException(String.format("No elliptic curve for JWK name %s found", name)));
    }

    public ECParameterSpec getParameterSpec() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidParameterSpecException {
        final AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        algorithmParameters.init(new ECGenParameterSpec(stdName));
        return algorithmParameters.getParameterSpec(ECParameterSpec.class);
    }

}
