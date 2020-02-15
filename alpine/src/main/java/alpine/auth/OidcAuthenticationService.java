package alpine.auth;

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import io.jsonwebtoken.*;
import org.glassfish.jersey.server.ContainerRequest;

import javax.annotation.Nullable;
import javax.naming.AuthenticationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * TODO: Investigate if merging with {@link JwtAuthenticationService} is possible
 *
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);

    private final OidcConfiguration oidcConfiguration;

    private final String idToken;

    public OidcAuthenticationService(final OidcConfiguration oidcConfiguration, final ContainerRequest request) {
        this(oidcConfiguration, extractIdToken(request));
    }

    OidcAuthenticationService(final OidcConfiguration oidcConfiguration, final String idToken) {
        this.oidcConfiguration = oidcConfiguration;
        this.idToken = idToken;
    }

    @Override
    public boolean isSpecified() {
        return Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED)
                && idToken != null;
    }

    @Nullable
    @Override
    public Principal authenticate() throws AuthenticationException {
        // TODO: Decide whether an ID or access token is expected. In case of an ID token, we may need to request an access token to be able to use the userinfo endpoint
        final JwtParser jwtParser = Jwts.parser().setSigningKeyResolver(new OidcSigningKeyResolver(oidcConfiguration));

        if (!jwtParser.isSigned(idToken)) {
            throw new AuthenticationException("ID token is not signed");
        }

        final String subject;

        try {
            jwtParser.parse(idToken);
            subject = jwtParser.parseClaimsJws(idToken).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("ID token is expired");
        } catch (MalformedJwtException e) {
            throw new AuthenticationException("ID token is invalid");
        } catch (SignatureException e) {
            throw new AuthenticationException("Signature of ID token is invalid");
        }

        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser user = qm.getOidcUser(subject);
            if (user != null) {
                LOGGER.info(String.format("Attempting to authenticate user: %s", subject));
                return user;
            } else if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.info(String.format("The user (%s) authenticated successfully but the account has not been provisioned", subject));
                return autoProvision(qm);
            } else {
                LOGGER.error(String.format("The user (%s) is unmapped and user provisioning is not enabled", subject));
                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    OidcUser autoProvision(final AlpineQueryManager queryManager) {
        // The ID Token may not include all required information
        final OidcUserInfo userInfo = ClientBuilder.newClient().target(oidcConfiguration.getUserInfoEndpointUri())
                .request(MediaType.APPLICATION_JSON)
                .get(OidcUserInfo.class);

        final String username = Optional.ofNullable(userInfo.getClaim(Config.getInstance().getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM), String.class))
                .orElseThrow(() -> new IllegalStateException("username claim not found"));

        OidcUser user = new OidcUser();
        user.setUsername(username);
        user.setEmail(userInfo.getEmail());
        user = queryManager.persist(user);

        return user;
    }

    static String extractIdToken(final ContainerRequest request) {
        return Optional.ofNullable(request.getRequestHeader(HttpHeaders.AUTHORIZATION)).orElseGet(Collections::emptyList).stream()
                .filter(header -> header.startsWith("Bearer"))
                .map(header -> header.substring("Bearer ".length()))
                .findFirst()
                .orElse(null);
    }

}
