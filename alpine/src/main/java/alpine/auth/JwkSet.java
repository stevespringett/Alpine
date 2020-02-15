package alpine.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwkSet {

    @JsonProperty("keys")
    private List<Jwk> keys;

    public List<Jwk> getKeys() {
        return keys;
    }

    public void setKeys(final List<Jwk> keys) {
        this.keys = keys;
    }

}
