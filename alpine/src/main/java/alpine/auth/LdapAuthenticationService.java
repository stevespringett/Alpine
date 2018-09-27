/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.auth;

import alpine.model.LdapUser;
import alpine.persistence.AlpineQueryManager;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.security.Principal;

/**
 * Class that performs authentication against LDAP servers.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class LdapAuthenticationService implements AuthenticationService {

    private String username;
    private String password;

    /**
     * Authentication service validates credentials against a directory service (LDAP).
     *
     * @param username the asserted username
     * @param password the asserted password
     * @since 1.0.0
     */
    public LdapAuthenticationService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns whether the username/password combo was specified or not. In
     * this case, since the constructor requires it, this method will always
     * return true.
     *
     * @return always will return true
     * @since 1.0.0
     */
    public boolean isSpecified() {
        return true;
    }

    /**
     * Authenticates the username/password combo against the directory service
     * and returns a Principal if authentication is successful. Otherwise,
     * returns an AuthenticationException.
     *
     * @return a Principal if authentication was successful
     * @throws AlpineAuthenticationException when authentication is unsuccessful
     * @since 1.0.0
     */
    public Principal authenticate() throws AlpineAuthenticationException {
        if (validateCredentials()) {
            try (AlpineQueryManager qm = new AlpineQueryManager()) {
                final LdapUser user = qm.getLdapUser(username);
                if (user != null) {
                    return user;
                } else {
                    throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
                }
            }
        } else {
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }
    }

    /**
     * Asserts a users credentials. Returns a boolean value indicating if
     * assertion was successful or not.
     *
     * @return true if assertion was successful, false if not
     * @since 1.0.0
     */
    private boolean validateCredentials() {
        LdapConnectionWrapper ldap = new LdapConnectionWrapper();
        LdapContext ldapContext = null;
        try {
            ldapContext = ldap.getLdapContext(username, password);
            return true;
        } catch (NamingException e) {
            return false;
        } finally {
            if (ldapContext != null) {
                try {
                    ldapContext.close();
                } catch (NamingException e) { }
            }
        }
    }

}
