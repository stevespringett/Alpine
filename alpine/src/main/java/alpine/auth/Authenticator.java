/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.auth;

import alpine.Config;
import javax.naming.AuthenticationException;
import java.security.Principal;

/**
 * Class is responsible for authenticating managed users against the internal user
 * database and optionally against a configured directory service (LDAP).
 *
 * @see AuthenticationService
 * @see ManagedUserAuthenticationService
 * @see LdapAuthenticationService
 *
 * @since 1.0.0
 */
public class Authenticator {

    private static final boolean LDAP_ENABLED = Config.getInstance().getPropertyAsBoolean(Config.Key.LDAP_ENABLED);

    private String username;
    private String password;

    public Authenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Attempts to authenticate the credentials internally first and if not  successful,
     * checks to see if LDAP is enabled or not. If enabled, a second attempt to authenticate
     * the credentials will be made, but this time against the directory service.
     *
     * @since 1.0.0
     */
    public Principal authenticate() throws AuthenticationException {
        ManagedUserAuthenticationService userService = new ManagedUserAuthenticationService(username, password);
        try {
            userService.authenticate();
        } catch (AuthenticationException e) { }
        if (LDAP_ENABLED) {
            LdapAuthenticationService ldapService = new LdapAuthenticationService(username, password);
            return ldapService.authenticate();
        }
        throw new AuthenticationException("Username or password is not valid, or the account is suspended.");
    }

}
