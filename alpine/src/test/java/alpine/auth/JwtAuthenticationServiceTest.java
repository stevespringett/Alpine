package alpine.auth;

import alpine.Config;
import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.OidcUser;
import alpine.model.UserPrincipal;
import alpine.persistence.AlpineQueryManager;
import alpine.util.TestUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import wiremock.org.apache.http.HttpHeaders;

import javax.naming.AuthenticationException;
import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtAuthenticationServiceTest {

    @BeforeClass
    public static void setUpClass() {
        Config.enableUnitTests();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.resetInMemoryDatabase();
    }

    @Test
    public void isSpecifiedShouldReturnTrueWhenBearerIsNotNull() {
        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.isSpecified()).isTrue();
    }

    @Test
    public void isSpecifiedShouldReturnFalseWhenBearerIsNull() {
        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Basic 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.isSpecified()).isFalse();
    }

    @Test
    public void authenticateShouldReturnNullWhenBearerIsNull() throws AuthenticationException {
        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Basic 123456"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnNullWhenTokenIsInvalid() throws AuthenticationException {
        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer invalidToken"));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldThrowExceptionWhenSubjectIsNull() {
        final Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("exp", Instant.now().plusSeconds(60).getEpochSecond());
        final String token = new JsonWebToken().createToken(tokenClaims);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldThrowExceptionWhenExpirationIsNull() {
        final Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("sub", "subject");
        final String token = new JsonWebToken().createToken(tokenClaims);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(authService::authenticate);
    }

    @Test
    public void authenticateShouldReturnNullWhenManagedUserIsSuspended() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            final ManagedUser managedUser = qm.createManagedUser("username", "passwordHash");
            managedUser.setSuspended(true);
            qm.persist(managedUser);
        }

        final Principal principalMock = mock(Principal.class);
        when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnNullWhenNoMatchingUserExists() throws AuthenticationException {
        final Principal principalMock = mock(Principal.class);
        when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        assertThat(authService.authenticate()).isNull();
    }

    @Test
    public void authenticateShouldReturnOidcUserWhenIdentityProviderIsLocal() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = mock(Principal.class);
        when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LOCAL);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser).isInstanceOf(ManagedUser.class);
    }

    @Test
    public void authenticateShouldReturnLdapUserWhenIdentityProviderIsLdap() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = mock(Principal.class);
        when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.LDAP);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser).isInstanceOf(LdapUser.class);
    }

    @Test
    public void authenticateShouldReturnOidcUserWhenIdentityProviderIsOpenIdConnect() throws AuthenticationException {
        try (final AlpineQueryManager qm = new AlpineQueryManager()) {
            qm.createManagedUser("username", "passwordHash");
            qm.createLdapUser("username");

            final OidcUser oidcUser = new OidcUser();
            oidcUser.setUsername("username");
            oidcUser.setSubjectIdentifier("subjectIdentifier");
            qm.persist(oidcUser);
        }

        final Principal principalMock = mock(Principal.class);
        when(principalMock.getName())
                .thenReturn("username");

        final String token = new JsonWebToken().createToken(principalMock, null, IdentityProvider.OPENID_CONNECT);

        final ContainerRequest containerRequestMock = mock(ContainerRequest.class);
        when(containerRequestMock.getRequestHeader(eq(HttpHeaders.AUTHORIZATION)))
                .thenReturn(Collections.singletonList("Bearer " + token));

        final JwtAuthenticationService authService = new JwtAuthenticationService(containerRequestMock);

        final UserPrincipal authenticatedUser = (UserPrincipal) authService.authenticate();
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser).isInstanceOf(OidcUser.class);
    }

}