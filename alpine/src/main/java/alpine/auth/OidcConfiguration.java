package alpine.auth;

import alpine.Config;
import alpine.cache.CacheManager;
import alpine.logging.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

/**
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID Provider Metadata</a>
 * @since 1.8.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {

    private static final Logger LOGGER = Logger.getLogger(OidcConfiguration.class);

    private static final String CONFIGURATION_CACHE_KEY = "OIDC_CONFIGURATION";

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpointUri;

    @JsonProperty("token_endpoint")
    private String tokenEndpointUri;

    @JsonProperty("userinfo_endpoint")
    private String userInfoEndpointUri;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    // TODO: Move this logic to a separate class
    public static OidcConfiguration getInstance() {
        return getConfiguration(Config.getInstance().getProperty(Config.AlpineKey.OIDC_DISCOVERY_URI));
    }

    static OidcConfiguration getConfiguration(final String discoveryUri) {
        OidcConfiguration configuration = CacheManager.getInstance().get(OidcConfiguration.class, CONFIGURATION_CACHE_KEY);
        if (configuration != null) {
            LOGGER.info("OIDC configuration loaded from cache");
            return configuration;
        }

        LOGGER.info("Loading OIDC configuration from " + discoveryUri);
        configuration = ClientBuilder.newClient().target(discoveryUri)
                .request(MediaType.APPLICATION_JSON)
                .get(OidcConfiguration.class);

        LOGGER.info("Storing OIDC configuration in cache");
        CacheManager.getInstance().put(CONFIGURATION_CACHE_KEY, configuration);

        return configuration;
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
