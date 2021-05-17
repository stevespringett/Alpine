package alpine.auth;

import java.net.URI;

/**
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID Connect specification: OpenID Provider Metadata</a>
 * @since 1.8.0
 */
public class OidcConfiguration {

    private String issuer;
    private URI userInfoEndpointUri;
    private URI jwksUri;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public URI getUserInfoEndpointUri() {
        return userInfoEndpointUri;
    }

    public void setUserInfoEndpointUri(final URI userInfoEndpointUri) {
        this.userInfoEndpointUri = userInfoEndpointUri;
    }

    public URI getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final URI jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public String toString() {
        return "OidcConfiguration{" +
                "issuer='" + issuer + '\'' +
                ", userInfoEndpointUri=" + userInfoEndpointUri +
                ", jwksUri=" + jwksUri +
                '}';
    }

}
