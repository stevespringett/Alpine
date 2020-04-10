package alpine.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID Connect specification: OpenID Provider Metadata</a>
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpointUri;

    @JsonProperty("token_endpoint")
    private String tokenEndpointUri;

    @JsonProperty("userinfo_endpoint")
    private String userInfoEndpointUri;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

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

    public String getUserInfoEndpointUri() {
        return userInfoEndpointUri;
    }

    public void setUserInfoEndpointUri(final String userInfoEndpointUri) {
        this.userInfoEndpointUri = userInfoEndpointUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

}
