package alpine.auth;

import alpine.logging.Logger;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;

import java.io.IOException;

/**
 * @since 1.10.0
 */
class OidcUserInfoAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(OidcUserInfoAuthenticator.class);

    private final OidcConfiguration configuration;

    OidcUserInfoAuthenticator(final OidcConfiguration configuration) {
        this.configuration = configuration;
    }

    OidcProfile authenticate(final String accessToken, final OidcProfileCreator profileCreator) throws AlpineAuthenticationException {
        final UserInfoResponse userInfoResponse;
        try {
            final var httpResponse =
                    new UserInfoRequest(configuration.getUserInfoEndpointUri(), new BearerAccessToken(accessToken))
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
            LOGGER.error("UserInfo request failed (Code:" + error.getCode() + ", Description: " + error.getDescription() + ")");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        final var userInfo = userInfoResponse.toSuccessResponse().getUserInfo();
        LOGGER.debug("UserInfo response: " + userInfo.toJSONString());

        return profileCreator.create(userInfo);
    }

}
