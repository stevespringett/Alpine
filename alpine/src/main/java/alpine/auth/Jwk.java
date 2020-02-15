package alpine.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO: Support multiple formats, not only RSA
 *
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Jwk {

    @JsonProperty("kty")
    private String keyType;

    @JsonProperty("kid")
    private String keyId;

    @JsonProperty("e")
    private String exponent;

    @JsonProperty("n")
    private String modulus;

    @JsonProperty("use")
    private String use;

    @JsonProperty("alg")
    private String algorithm;

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

    public String getExponent() {
        return exponent;
    }

    public void setExponent(final String exponent) {
        this.exponent = exponent;
    }

    public String getModulus() {
        return modulus;
    }

    public void setModulus(final String modulus) {
        this.modulus = modulus;
    }

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
}
