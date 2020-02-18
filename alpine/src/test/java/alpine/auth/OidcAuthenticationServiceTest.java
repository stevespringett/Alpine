package alpine.auth;

import alpine.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OidcAuthenticationServiceTest {

    @Mock
    private Config configMock;

    @Mock
    private OidcConfiguration oidcConfigurationMock;

    @Mock
    private OidcSigningKeyResolver signingKeyResolverMock;

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(false);

        final OidcAuthenticationService authenticationService =
                new OidcAuthenticationService(configMock, oidcConfigurationMock, signingKeyResolverMock, "");

        assertThat(authenticationService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenRequestDoesntContainAccessToken() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authenticationService =
                new OidcAuthenticationService(configMock, oidcConfigurationMock, signingKeyResolverMock, null);

        assertThat(authenticationService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndAccessTokenIsProvided() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authenticationService =
                new OidcAuthenticationService(configMock, oidcConfigurationMock, signingKeyResolverMock, "a");

        assertThat(authenticationService.isSpecified()).isTrue();
    }

}
