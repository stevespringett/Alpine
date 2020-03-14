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

    public static boolean isOidcAvailable() {
        return isOidcAvailable(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve());
    }

    public static boolean isOidcAvailable(final Config config, final OidcConfiguration oidcConfiguration) {
        return config.getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED)
                && oidcConfiguration != null;
    }

}
