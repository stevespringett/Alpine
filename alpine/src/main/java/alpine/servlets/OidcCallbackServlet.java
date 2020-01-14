package alpine.servlets;

import alpine.Config;
import alpine.auth.JsonWebToken;
import alpine.auth.OidcClient;
import alpine.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import alpine.util.HttpUtil;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 1.8.0
 */
public class OidcCallbackServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(OidcClient.class);

    private String loginSuccessUrl;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        LOGGER.info("Initializing OIDC callback servlet");
        super.init(config);

        loginSuccessUrl = config.getInitParameter("loginSuccessUrl");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String requestState = req.getParameter("state");
        final String sessionState = HttpUtil.getSessionAttribute(req.getSession(), "OIDC_STATE");

        if (requestState == null || sessionState == null) {
            LOGGER.error("Either request or session state is missing");
            resp.sendError(400);
            return;
        }

        final String authorizationCode = req.getParameter("code");
        if (authorizationCode == null) {
            LOGGER.error("Authorization code is missing");
            resp.sendError(400);
            return;
        }

        final String redirectUri = HttpUtil.getSessionAttribute(req.getSession(), "OIDC_REDIRECT_URI");
        if (redirectUri == null) {
            LOGGER.error("Cannot find redirect URI in session");
            resp.sendError(400);
            return;
        }

        final OidcClient oidcClient = new OidcClient();

        // TODO: Parse id or access token
        LOGGER.debug("Exchanging authorization code for access token");
        final String accessToken = oidcClient.obtainAccessTokenForAuthorizationCode(authorizationCode, redirectUri);

        // Redirect URI and State are not needed anymore
        req.getSession().removeAttribute("OIDC_REDIRECT_URI");
        req.getSession().removeAttribute("OIDC_STATE");

        try (AlpineQueryManager qm = new AlpineQueryManager()) {
            final OidcUser user = qm.getOidcUser("test");
            if (user != null) {
                LOGGER.info("Logged in as \"test\"");
            } else {
                if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                    LOGGER.info("Creating user for OIDC user \"test\"");
                    qm.createOidcUser("test");
                } else {
                    LOGGER.error("OIDC user provisioning is disabled");
                    resp.sendError(401);
                    return;
                }
            }
        }

        // TODO: Is it possible / feasible to return a JWT here?
        req.getSession().setAttribute("OIDC_ACCESS_TOKEN", accessToken);
        resp.sendRedirect(loginSuccessUrl);
    }

}
