package alpine.auth;

import alpine.Config;
import alpine.cache.CacheManager;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import alpine.util.OidcUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;

/**
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);
    private static final String JWK_SET_CACHE_KEY = "OIDC_JWK_SET";

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final String accessToken;
    private final String idToken;

    /**
     * @param accessToken The access token acquired by authenticating with an IdP
     * @deprecated Use {@link #OidcAuthenticationService(String, String)} instead
     */
    @Deprecated
    public OidcAuthenticationService(final String accessToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), accessToken, null);
    }

    /**
     * @param accessToken The access token acquired by authenticating with an IdP
     * @param idToken     The ID token acquired by authenticating with an IdP
     * @since 1.10.0
     */
    public OidcAuthenticationService(final String accessToken, final String idToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), accessToken, idToken);
    }

    /**
     * Constructor for unit tests
     */
    OidcAuthenticationService(final Config config, final OidcConfiguration oidcConfiguration, final String accessToken, final String idToken) {
        this.config = config;
        this.oidcConfiguration = oidcConfiguration;
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    @Override
    public boolean isSpecified() {
        return OidcUtil.isOidcAvailable(config, oidcConfiguration)
                && (accessToken != null || idToken != null);
    }

    /**
     * Authenticate a {@link Principal} using the provided credentials.
     * <p>
     * If an ID token is provided, Alpine will validate it and source configured claims from it.
     * <p>
     * If an access token is provided, Alpine will call the IdP's {@code /userinfo} endpoint with it
     * to verify its validity, and source configured claims from the response.
     * <p>
     * If both access token and ID token are provided, the ID token takes precedence.
     * When all configured claims are found in the ID token, {@code /userinfo} won't be requested.
     * When not all claims were found in the ID token, {@code /userinfo} will be requested supplementary.
     *
     * @return An authenticated {@link Principal}
     * @throws AlpineAuthenticationException When authentication failed
     */
    @Nullable
    @Override
    public Principal authenticate() throws AlpineAuthenticationException {
        final String usernameClaim = config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM);
        if (usernameClaim == null) {
            LOGGER.error("No username claim has been configured");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final boolean teamSyncEnabled = config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION);
        final String teamsClaim = config.getProperty(Config.AlpineKey.OIDC_TEAMS_CLAIM);
        if (teamSyncEnabled && teamsClaim == null) {
            LOGGER.error("Team synchronization is enabled, but no teams claim has been configured");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        OidcProfile idTokenProfile = null;
        if (idToken != null) {
            idTokenProfile = getProfileFromIdToken(idToken, usernameClaim, teamsClaim);
            LOGGER.debug("ID token profile: " + idTokenProfile);

            if (isProfileComplete(idTokenProfile, teamSyncEnabled)) {
                LOGGER.debug("ID token profile is complete, proceeding to authenticate");
                return authenticateInternal(idTokenProfile);
            }
        }

        OidcProfile userInfoProfile = null;
        if (accessToken != null) {
            userInfoProfile = getProfileFromUserInfo(accessToken, usernameClaim, teamsClaim);
            LOGGER.debug("UserInfo profile: " + userInfoProfile);

            if (isProfileComplete(userInfoProfile, teamSyncEnabled)) {
                LOGGER.debug("UserInfo profile is complete, proceeding to authenticate");
                return authenticateInternal(userInfoProfile);
            }
        }

        OidcProfile mergedProfile = null;
        if (idTokenProfile != null && userInfoProfile != null) {
            try {
                mergedProfile = mergeProfiles(idTokenProfile, userInfoProfile);
                LOGGER.debug("Merged profile: " + mergedProfile);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to merge profiles", e);
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
            }

            if (isProfileComplete(mergedProfile, teamSyncEnabled)) {
                LOGGER.debug("Merged profile is complete, proceeding to authenticate");
                return authenticateInternal(mergedProfile);
            }
        }

        LOGGER.error("Unable to assemble complete profile (ID token: " + idTokenProfile +
                ", UserInfo: " + userInfoProfile + ", Merged: " + mergedProfile + ")");
        throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
    }

    private OidcUser authenticateInternal(final OidcProfile profile) throws AlpineAuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            OidcUser user = qm.getOidcUser(profile.getUsername());
            if (user != null) {
                LOGGER.debug("Attempting to authenticate user: " + user.getUsername());
                user.setEmail(profile.getEmail()); // email is not persisted and thus needs to be set ad-hoc
                if (user.getSubjectIdentifier() == null) {
                    LOGGER.debug("Assigning subject identifier " + profile.getSubject() + " to user " + user.getUsername());
                    user.setSubjectIdentifier(profile.getSubject());
                    return qm.updateOidcUser(user);
                } else if (!user.getSubjectIdentifier().equals(profile.getSubject())) {
                    LOGGER.error("Refusing to authenticate user " + user.getUsername() + ": subject identifier has changed (" +
                            user.getSubjectIdentifier() + " to " + profile.getSubject() + ")");
                    throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
                }
                if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
                    return qm.synchronizeTeamMembership(user, profile.getTeams());
                }
                return user;
            } else if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.debug("The user (" + profile.getUsername() + ") authenticated successfully but the account has not been provisioned");
                return autoProvision(qm, profile);
            } else {
                LOGGER.debug("The user (" + profile.getUsername() + ") is unmapped and user provisioning is not enabled");
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    private OidcProfile getProfileFromIdToken(final String idToken, final String usernameClaim, final String teamsClaim) throws AlpineAuthenticationException {
        final SignedJWT parsedIdToken;
        try {
            parsedIdToken = SignedJWT.parse(idToken);
        } catch (ParseException e) {
            LOGGER.error("Parsing ID token failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final JWKSet jwkSet;
        try {
            jwkSet = resolveJwkSet();
        } catch (IOException | ParseException e) {
            LOGGER.error("Resolving JWK set failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final var idTokenValidator = new IDTokenValidator(
                new Issuer(oidcConfiguration.getIssuer()),
                new ClientID(config.getProperty(Config.AlpineKey.OIDC_CLIENT_ID)),
                parsedIdToken.getHeader().getAlgorithm(), jwkSet);

        final IDTokenClaimsSet claimsSet;
        try {
            claimsSet = idTokenValidator.validate(parsedIdToken, null);
            LOGGER.debug("ID token claims: " + claimsSet.toJSONString());
        } catch (BadJOSEException | JOSEException e) {
            LOGGER.error("ID token validation failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        final var profile = new OidcProfile();
        profile.setSubject(claimsSet.getSubject().getValue());
        profile.setUsername(claimsSet.getStringClaim(usernameClaim));
        profile.setTeams(claimsSet.getStringListClaim(teamsClaim));
        profile.setEmail(claimsSet.getStringClaim(UserInfo.EMAIL_CLAIM_NAME));
        return profile;
    }

    private OidcProfile getProfileFromUserInfo(final String accessToken, final String usernameClaim, final String teamsClaim) throws AlpineAuthenticationException {
        final UserInfoResponse userInfoResponse;
        try {
            final var httpResponse =
                    new UserInfoRequest(oidcConfiguration.getUserInfoEndpointUri(), new BearerAccessToken(accessToken))
                            .toHTTPRequest()
                            .send();
            userInfoResponse = UserInfoResponse.parse(httpResponse);
        } catch (IOException e) {
            LOGGER.error("UserInfo request failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            LOGGER.error("Parsing UserInfo response failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        if (!userInfoResponse.indicatesSuccess()) {
            final var error = userInfoResponse.toErrorResponse().getErrorObject();
            LOGGER.error("UserInfo request failed: " + error.getCode() + " - " + error.getDescription());
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        final var userInfo = userInfoResponse.toSuccessResponse().getUserInfo();
        LOGGER.debug("UserInfo response: " + userInfo.toJSONString());

        final var profile = new OidcProfile();
        profile.setSubject(userInfo.getSubject().getValue());
        profile.setUsername(userInfo.getStringClaim(usernameClaim));
        profile.setTeams(userInfo.getStringListClaim(teamsClaim));
        profile.setEmail(userInfo.getEmailAddress());
        return profile;
    }

    private boolean isProfileComplete(final OidcProfile profile, final boolean teamSyncEnabled) {
        return profile.getSubject() != null
                && profile.getUsername() != null
                && (!teamSyncEnabled || (profile.getTeams() != null));
    }

    OidcProfile mergeProfiles(final OidcProfile left, final OidcProfile right) {
        final var profile = new OidcProfile();
        profile.setSubject(mergeProfileClaim(left.getSubject(), right.getSubject()));
        profile.setUsername(mergeProfileClaim(left.getUsername(), right.getUsername()));
        profile.setTeams(mergeProfileClaim(left.getTeams(), right.getTeams()));
        profile.setEmail(mergeProfileClaim(left.getEmail(), right.getEmail()));
        return profile;
    }

    private <T> T mergeProfileClaim(final T left, final T right) {
        return (left != null) ? left : right;
    }

    JWKSet resolveJwkSet() throws IOException, ParseException {
        JWKSet jwkSet = CacheManager.getInstance().get(JWKSet.class, JWK_SET_CACHE_KEY);
        if (jwkSet != null) {
            LOGGER.debug("JWK set loaded from cache");
            return jwkSet;
        }

        LOGGER.debug("Fetching JWK set from " + oidcConfiguration.getJwksUri());
        jwkSet = JWKSet.load(oidcConfiguration.getJwksUri().toURL());

        LOGGER.debug("Storing JWK set in cache");
        CacheManager.getInstance().put(JWK_SET_CACHE_KEY, jwkSet);
        return jwkSet;
    }

    private OidcUser autoProvision(final AlpineQueryManager qm, final OidcProfile profile) {
        OidcUser user = new OidcUser();
        user.setUsername(profile.getUsername());
        user.setSubjectIdentifier(profile.getSubject());
        user.setEmail(profile.getEmail());
        user = qm.persist(user);

        if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
            LOGGER.debug("Synchronizing teams for user " + user.getUsername());
            return qm.synchronizeTeamMembership(user, profile.getTeams());
        }

        return user;
    }

}
