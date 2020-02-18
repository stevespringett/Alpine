package alpine.auth;

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.naming.AuthenticationException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * TODO: Investigate if merging with {@link JwtAuthenticationService} is possible
 *
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final OidcSigningKeyResolver signingKeyResolver;
    private final String accessToken;

    public OidcAuthenticationService(final OidcConfiguration oidcConfiguration, final ContainerRequest request) {
        this(Config.getInstance(), oidcConfiguration, new OidcSigningKeyResolver(oidcConfiguration), extractAccessToken(request));
    }

    /**
     * Constructor for unit tests
     */
    OidcAuthenticationService(final Config config, final OidcConfiguration oidcConfiguration,
                              final OidcSigningKeyResolver signingKeyResolver, final String accessToken) {
        this.config = config;
        this.oidcConfiguration = oidcConfiguration;
        this.signingKeyResolver = signingKeyResolver;
        this.accessToken = accessToken;
    }

    @Override
    public boolean isSpecified() {
        return config.getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED)
                && accessToken != null;
    }

    @Nullable
    @Override
    public Principal authenticate() throws AuthenticationException {
        final JwtParser jwtParser = Jwts.parser()
                .requireIssuer(oidcConfiguration.getIssuer())
                .setSigningKeyResolver(signingKeyResolver);

        if (!jwtParser.isSigned(accessToken)) {
            throw new AuthenticationException("Access token is not signed");
        }

        final Jws<Claims> accessTokenClaims;
        final String username;

        try {
            accessTokenClaims = jwtParser.parseClaimsJws(accessToken);
            username = accessTokenClaims.getBody().get(config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM), String.class);

            if (username == null) {
                throw new AuthenticationException(String.format("username claim \"%s\" not found",
                        config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM)));
            }
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Access token is expired");
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("Access token is invalid");
        } catch (SignatureException e) {
            throw new AuthenticationException("Signature of access token is invalid");
        }

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser user = qm.getOidcUser(username);
            if (user != null) {
                LOGGER.info(String.format("Attempting to authenticate user: %s", username));
                return user;
            } else if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.info(String.format("The user (%s) authenticated successfully but the account has not been provisioned", username));
                return autoProvision(qm, username);
            } else {
                LOGGER.error(String.format("The user (%s) is unmapped and user provisioning is not enabled", username));
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    OidcUser autoProvision(final AlpineQueryManager qm, final String username) throws AuthenticationException {
        LOGGER.info("Requesting UserInfo for user " + username);
        final OidcUserInfo userInfo;
        try {
            userInfo = ClientBuilder.newClient().target(oidcConfiguration.getUserInfoEndpointUri())
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .get(OidcUserInfo.class);
        } catch (WebApplicationException | ProcessingException e) {
            throw new AuthenticationException(String.format("Failed to get user info for user %s: %s", username, e.getMessage()));
        }

        if (userInfo.getEmail() == null || !userInfo.isEmailVerified()) {
            // TODO: Make it configurable whether or not a verified email address should be required?
            throw new AuthenticationException(String.format("User %s does not have a verified email", username));
        }

        OidcUser user = new OidcUser();
        user.setUsername(username);
        user.setEmail(userInfo.getEmail());
        user = qm.persist(user);

        // TODO: Team synchronization with access token roles?

        return user;
    }

    static String extractAccessToken(final ContainerRequest request) {
        return Optional.ofNullable(request.getRequestHeader(HttpHeaders.AUTHORIZATION)).orElseGet(Collections::emptyList).stream()
                // FIXME: Using non-standard header to prevent conflicts with JwtAuthenticationService for now
                .filter(header -> header.startsWith("AccessToken"))
                .map(header -> header.substring("AccessToken ".length()))
                .findFirst()
                .orElse(null);
    }

}
