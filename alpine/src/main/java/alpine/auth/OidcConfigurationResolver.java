package alpine.auth;

import alpine.Config;
import alpine.cache.CacheManager;
import alpine.logging.Logger;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.8.0
 */
public class OidcConfigurationResolver {

    private static final OidcConfigurationResolver INSTANCE = new OidcConfigurationResolver(Config.getInstance().getProperty(Config.AlpineKey.OIDC_DISCOVERY_URI));
    private static final Logger LOGGER = Logger.getLogger(OidcConfigurationResolver.class);
    static final String CONFIGURATION_CACHE_KEY = "OIDC_CONFIGURATION";

    private final String discoveryUri;

    OidcConfigurationResolver(final String discoveryUri) {
        this.discoveryUri = discoveryUri;
    }

    public static OidcConfigurationResolver getInstance() {
        return INSTANCE;
    }

    /**
     * Resolve the OpenID Connect configuration either from a remote authorization server or from cache.
     *
     * @return The resolved {@link OidcConfiguration} or {@code null}, when resolving was not possible
     */
    @Nullable
    public OidcConfiguration resolve() {
        if (discoveryUri == null) {
            LOGGER.error("Cannot resolve OIDC configuration: No discovery URI provided");
            return null;
        }

        OidcConfiguration configuration = CacheManager.getInstance().get(OidcConfiguration.class, CONFIGURATION_CACHE_KEY);
        if (configuration != null) {
            LOGGER.debug("OIDC configuration loaded from cache");
            return configuration;
        }

        LOGGER.debug("Loading OIDC configuration from " + discoveryUri);
        try {
            configuration = ClientBuilder.newClient().target(discoveryUri)
                    .request(MediaType.APPLICATION_JSON)
                    .get(OidcConfiguration.class);
        } catch (WebApplicationException | ProcessingException e) {
            LOGGER.error("Failed to load OIDC configuration from " + discoveryUri + ": " + e.getMessage());
            return null;
        }

        LOGGER.debug("Storing OIDC configuration in cache");
        CacheManager.getInstance().put(CONFIGURATION_CACHE_KEY, configuration);

        return configuration;
    }

}
