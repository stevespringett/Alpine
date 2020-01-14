package alpine.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpointUri;

    @JsonProperty("token_endpoint")
    private String tokenEndpointUri;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    public String getAuthorizationEndpointUri() {
        return authorizationEndpointUri;
    }

    public void setAuthorizationEndpointUri(final String authorizationEndpointUri) {
        this.authorizationEndpointUri = authorizationEndpointUri;
    }

    public String getTokenEndpointUri() {
        return tokenEndpointUri;
    }

    public void setTokenEndpointUri(final String tokenEndpointUri) {
        this.tokenEndpointUri = tokenEndpointUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

}
