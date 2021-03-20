package alpine.auth;

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import alpine.util.OidcUtil;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.List;

/**
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final String accessToken;

    public OidcAuthenticationService(final String accessToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), accessToken);
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
        return OidcUtil.isOidcAvailable(config, oidcConfiguration) && accessToken != null;
    }

    @Nullable
    @Override
    public Principal authenticate() throws AlpineAuthenticationException {
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
            LOGGER.error("An error occurred requesting the OIDC UserInfo", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        } catch (ProcessingException e) {
            LOGGER.error("An error occurred while processing the OIDC UserInfo response", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final String usernameClaim = config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM);
        final String username = userInfo.getClaim(usernameClaim, String.class);
        if (username == null) {
            LOGGER.error("The configured OIDC username claim (" + usernameClaim + ") could not be found in UserInfo response");
            LOGGER.debug("Claims returned in UserInfo response: " + userInfo.getClaims());
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            OidcUser user = qm.getOidcUser(username);
            if (user != null) {
                LOGGER.debug("Attempting to authenticate user: " + username);
                user.setEmail(userInfo.getEmail()); // email is not persisted and thus needs to be set ad-hoc
                if (user.getSubjectIdentifier() == null) {
                    LOGGER.debug("Assigning subject identifier " + userInfo.getSubject() + " to user " + username);
                    user.setSubjectIdentifier(userInfo.getSubject());
                    return qm.updateOidcUser(user);
                } else if (!user.getSubjectIdentifier().equals(userInfo.getSubject())) {
                    LOGGER.error("Refusing to authenticate user " + username + ": subject identifier has changed (" +
                            user.getSubjectIdentifier() + " to " + userInfo.getSubject() + ")");
                    throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
                }
                if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
                    return synchronizeTeams(qm, user, userInfo);
                }
                return user;
            } else if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.debug("The user (" + username + ") authenticated successfully but the account has not been provisioned");
                return autoProvision(qm, username, userInfo);
            } else {
                LOGGER.debug("The user (" + username + ") is unmapped and user provisioning is not enabled");
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    private OidcUser autoProvision(final AlpineQueryManager qm, final String username, final OidcUserInfo userInfo) {
        OidcUser user = new OidcUser();
        user.setUsername(username);
        user.setSubjectIdentifier(userInfo.getSubject());
        user.setEmail(userInfo.getEmail());
        user = qm.persist(user);

        if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
            LOGGER.debug("Synchronizing teams for user " + username);
            return synchronizeTeams(qm, user, userInfo);
        }

        return user;
    }

    OidcUser synchronizeTeams(final AlpineQueryManager qm, final OidcUser user, final OidcUserInfo userInfo) {
        final String teamsClaim = config.getProperty(Config.AlpineKey.OIDC_TEAMS_CLAIM);
        if (teamsClaim == null) {
            LOGGER.error("Synchronizing teams for user " + user.getUsername() + " failed: Synchronization is enabled, but no teams claim is configured");
            return user;
        }

        final List<String> groups;
        try {
            //noinspection unchecked
            groups = userInfo.getClaim(teamsClaim, List.class);
        } catch (ClassCastException e) {
            LOGGER.error("Synchronizing teams for user " + user.getUsername() + " failed: Teams claim is not a list", e);
            return user;
        }

        if (groups == null) {
            LOGGER.error("Synchronizing teams for user " + user.getUsername() + " failed: Teams claim " + teamsClaim + " does not exist");
            return user;
        }

        return qm.synchronizeTeamMembership(user, groups);
    }

}
