package alpine.util;

import alpine.Config;
import alpine.auth.OidcConfiguration;
import alpine.auth.OidcConfigurationResolver;

/**
 * @since 1.8.0
 */
public final class OidcUtil {

    private OidcUtil() {
    }

    /**
     * Determines whether or not the OpenID Connect integration is available.
     * <p>
     * Availability is given if OpenID Connect has been enabled via Alpine's configuration
     * <strong>and</strong> the configuration of the configured OpenID Connect identity provider
     * has been resolved successfully.
     *
     * @return {@code true} when OpenID Connect is available, otherwise {@code false}
     */
    public static boolean isOidcAvailable() {
        return isOidcAvailable(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve());
    }

    public static boolean isOidcAvailable(final Config config, final OidcConfiguration oidcConfiguration) {
        return config.getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED)
                && oidcConfiguration != null;
    }

}
