package alpine.auth;

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.security.Principal;

/**
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final String accessToken;

    public OidcAuthenticationService(final String accessToken) {
        this(Config.getInstance(), OidcConfiguration.getInstance(), accessToken);
    }

    /**
     * Constructor for unit tests
     */
    OidcAuthenticationService(final Config config, final OidcConfiguration oidcConfiguration, final String accessToken) {
        this.config = config;
        this.oidcConfiguration = oidcConfiguration;
        this.accessToken = accessToken;
    }

    @Override
    public boolean isSpecified() {
        return config.getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED)
                && accessToken != null;
    }

    @Nullable
    @Override
    public Principal authenticate() throws AlpineAuthenticationException {
        // Exchange the Access Token for User Info
        // There's no guarantee that Access Tokens will always be JWTs (in case of GitLab, it indeed isn't),
        // so this has the nice side effect that the Authorization Server can validate the Token itself
        final OidcUserInfo userInfo;
        try {
            userInfo = ClientBuilder.newClient().target(oidcConfiguration.getUserInfoEndpointUri())
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .get(OidcUserInfo.class);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 401) {
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
            }
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        } catch (ProcessingException e) {
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final String username = userInfo.getClaim(config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM), String.class);
        if (username == null) {
            LOGGER.error("Configured OIDC username claim does not exist");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser user = qm.getOidcUser(username);
            if (user != null) {
                LOGGER.info(String.format("Attempting to authenticate user: %s", username));
                return user;
            } else if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.info(String.format("The user (%s) authenticated successfully but the account has not been provisioned", username));
                return autoProvision(qm, username, userInfo);
            } else {
                LOGGER.error(String.format("The user (%s) is unmapped and user provisioning is not enabled", username));
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    OidcUser autoProvision(final AlpineQueryManager qm, final String username, final OidcUserInfo userInfo) {
        OidcUser user = new OidcUser();
        user.setUsername(username);
        user.setEmail(userInfo.getEmail());
        user = qm.persist(user);

        // TODO: Team synchronization with access token roles?

        return user;
    }

}
