package alpine.servlets;

import alpine.auth.OidcClient;
import alpine.logging.Logger;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

/**
 * @since 1.8.0
 */
public class OidcRedirectServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(OidcRedirectServlet.class);

    private String callbackUrl;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        LOGGER.info("Initializing OIDC redirect servlet");
        super.init(config);

        callbackUrl = config.getInitParameter("callbackUrl");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String redirectUri = UriBuilder.fromUri(req.getRequestURL().toString())
                .replacePath(callbackUrl)
                .replaceMatrix(null)
                .replaceQuery(null)
                .build().toString();
        final String state = UUID.randomUUID().toString();

        req.getSession().setAttribute("OIDC_STATE", state);
        req.getSession().setAttribute("OIDC_REDIRECT_URI", redirectUri);

        resp.sendRedirect(new OidcClient().prepareAuthenticationRequest(state, redirectUri));
    }
}
