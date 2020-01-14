package alpine.auth;

import alpine.Config;
import alpine.cache.CacheManager;
import alpine.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * @see <a href="https://openid.net/specs/openid-connect-basic-1_0.html#CodeFlow">OpenID Connect Basic Client
 *         Implementer's Guide 1.0</a>
 * @since 1.8.0
 */
public class OidcClient {

    private static final Logger LOGGER = Logger.getLogger(OidcClient.class);
    private static final String CONFIGURATION_CACHE_KEY = "OIDC_CONFIGURATION";

    private final String clientId;
    private final String clientSecret;
    private final OidcConfiguration configuration;

    OidcClient(final String clientId, final String clientSecret, final OidcConfiguration configuration) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.configuration = configuration;
    }

    public OidcClient() {
        this(Config.getInstance().getProperty(Config.AlpineKey.OIDC_CLIENT_ID),
                Config.getInstance().getProperty(Config.AlpineKey.OIDC_CLIENT_SECRET),
                getConfiguration(Config.getInstance().getProperty(Config.AlpineKey.OIDC_DISCOVERY_URI)));
    }

    public String obtainAccessTokenForAuthorizationCode(final String authorizationCode, final String redirectUri) {
        final JsonNode jsonNode = ClientBuilder.newClient().target(configuration.getTokenEndpointUri())
                .register(HttpAuthenticationFeature.basic(clientId, clientSecret))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(new Form()
                        .param("code", authorizationCode)
                        .param("grant_type", "authorization_code")
                        .param("redirect_uri", redirectUri)), JsonNode.class);

        return jsonNode.get("access_token").asText();
    }

    public String prepareAuthenticationRequest(final String state, final String redirectUri) {
        return UriBuilder.fromUri(configuration.getAuthorizationEndpointUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", "openid profile")
                .queryParam("state", state)
                .queryParam("redirect_uri", redirectUri)
                .build().toString();
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

}
