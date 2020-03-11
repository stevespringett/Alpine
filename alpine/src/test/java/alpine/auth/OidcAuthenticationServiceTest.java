package alpine.auth;

import alpine.Config;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticationServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private Config configMock;
    private OidcConfiguration oidcConfigurationMock;

    @Before
    public void setUp() {
        configMock = mock(Config.class);
        oidcConfigurationMock = mock(OidcConfiguration.class);
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenOidcIsDisabled() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(false);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, "accessToken");

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenAccessTokenIsNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, null);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenOidcIsEnabledAndAccessTokenIsNotNull() {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.OIDC_ENABLED)))
                .thenReturn(true);

        final OidcAuthenticationService authService = new OidcAuthenticationService(configMock, oidcConfigurationMock, "accessToken");

        assertThat(authService.isSpecified()).isTrue();
    }

}
