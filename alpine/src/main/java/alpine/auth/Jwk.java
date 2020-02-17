package alpine.auth;

import alpine.json.Base64EncodedBigIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigInteger;

/**
 * @see <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-00#section-4.2">JWK Key Object Format</a>
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Jwk {

    @JsonProperty("use")
    private String use;

    @JsonProperty("alg")
    private String algorithm;

    @JsonProperty("kty")
    private String keyType;

    @JsonProperty("kid")
    private String keyId;

    /**
     * Exponent value for the RSA public key.
     */
    @JsonProperty("e")
    @JsonDeserialize(using = Base64EncodedBigIntegerDeserializer.class)
    private BigInteger exponent;

    /**
     * Modulus value for the RSA public key.
     */
    @JsonProperty("n")
    @JsonDeserialize(using = Base64EncodedBigIntegerDeserializer.class)
    private BigInteger modulus;

    /**
     * Cryptographic curve used with the key.
     */
    @JsonProperty("crv")
    private String curve;

    /**
     * X coordinate for the elliptic curve point.
     */
    @JsonProperty("x")
    @JsonDeserialize(using = Base64EncodedBigIntegerDeserializer.class)
    private BigInteger x;

    /**
     * Y coordinate for the elliptic curve point.
     */
    @JsonProperty("y")
    @JsonDeserialize(using = Base64EncodedBigIntegerDeserializer.class)
    private BigInteger y;

    public String getUse() {
        return use;
    }

    public void setUse(final String use) {
        this.use = use;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(final String keyType) {
        this.keyType = keyType;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(final String keyId) {
        this.keyId = keyId;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    public void setExponent(final BigInteger exponent) {
        this.exponent = exponent;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public void setModulus(final BigInteger modulus) {
        this.modulus = modulus;
    }

    public String getCurve() {
        return curve;
    }

    public void setCurve(final String curve) {
        this.curve = curve;
    }

    public BigInteger getX() {
        return x;
    }

    public void setX(final BigInteger x) {
        this.x = x;
    }

    public BigInteger getY() {
        return y;
    }

    public void setY(final BigInteger y) {
        this.y = y;
    }

}
