/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.server.auth;

import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.OidcUser;
import alpine.model.Permission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonWebTokenTest {

    @Test
    public void createTokenTest() {
        Principal user = Mockito.mock(Principal.class);
        Mockito.when(user.getName()).thenReturn("admin");
        JsonWebToken jwt = new JsonWebToken();
        String token = jwt.createToken(user);
        Assertions.assertNotNull(token);
        Assertions.assertTrue(jwt.validateToken(token));
        Assertions.assertEquals("admin", jwt.getSubject());
        Assertions.assertNotNull(jwt.getExpiration());
    }

    @Test
    public void createTokenPermissionsTest() {
        List<Permission> permissions = new ArrayList<>();
        Permission p1 = Mockito.mock(Permission.class);
        Mockito.when(p1.getName()).thenReturn("PERM-1");
        Permission p2 = Mockito.mock(Permission.class);
        Mockito.when(p2.getName()).thenReturn("PERM-2");
        permissions.add(p1);
        permissions.add(p2);
        Principal user = Mockito.mock(Principal.class);
        Mockito.when(user.getName()).thenReturn("admin");
        JsonWebToken jwt = new JsonWebToken();
        String token = jwt.createToken(user, permissions);
        Assertions.assertNotNull(token);
        Assertions.assertTrue(jwt.validateToken(token));
        Assertions.assertEquals("admin", jwt.getSubject());
        Assertions.assertNotNull(jwt.getExpiration());
    }

    @Test
    public void createTokenShouldDeriveIdentityProviderLocal() {
        final JsonWebToken jwt = new JsonWebToken();

        final String token = jwt.createToken(new ManagedUser());

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getIdentityProvider()).isEqualTo(IdentityProvider.LOCAL);
    }

    @Test
    public void createTokenShouldDeriveIdentityProviderLdap() {
        final JsonWebToken jwt = new JsonWebToken();

        final String token = jwt.createToken(new LdapUser());

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getIdentityProvider()).isEqualTo(IdentityProvider.LDAP);
    }

    @Test
    public void createTokenShouldDeriveIdentityProviderOidc() {
        final JsonWebToken jwt = new JsonWebToken();

        final String token = jwt.createToken(new OidcUser());

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getIdentityProvider()).isEqualTo(IdentityProvider.OPENID_CONNECT);
    }

    @Test
    public void createTokenShouldSetLocalIdentityProviderWhenProviderCouldNotBeDerived() {
        final JsonWebToken jwt = new JsonWebToken();

        final String token = jwt.createToken(Mockito.mock(Principal.class));

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getIdentityProvider()).isEqualTo(IdentityProvider.LOCAL);
    }

    @Test
    public void createTokenShouldUseProvidedIdentityProvider() {
        final JsonWebToken jwt = new JsonWebToken();

        final String token = jwt.createToken(Mockito.mock(Principal.class), null, IdentityProvider.OPENID_CONNECT);

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getIdentityProvider()).isEqualTo(IdentityProvider.OPENID_CONNECT);
    }

}
