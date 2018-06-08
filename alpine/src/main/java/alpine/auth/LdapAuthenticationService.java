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

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.LdapUser;
import alpine.persistence.AlpineQueryManager;
import org.apache.commons.lang3.StringUtils;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.security.Principal;
import java.util.Hashtable;

/**
 * Class that performs authentication against LDAP servers.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class LdapAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticationService.class);
    private static final String LDAP_URL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_SERVER_URL);
    private static final String DOMAIN_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_DOMAIN);
    private static final String LDAP_SECURITY_AUTH = Config.getInstance().getProperty(Config.AlpineKey.LDAP_SECURITY_AUTH);
    private static final String LDAP_AUTH_USERNAME_FMT = Config.getInstance().getProperty(Config.AlpineKey.LDAP_AUTH_USERNAME_FMT);
	
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
     * Asserts a users credentials. Returns an LdapContext if assertion is successful
     * or an exception for any other reason.
     *
     * @param username the username to assert
     * @param password the password to assert
     * @return the LdapContext upon a successful connection
     * @throws NamingException when unable to establish a connection
     * @since 1.0.0
     */
    private LdapContext getConnection(String username, String password) throws NamingException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new NamingException("Username or password cannot be empty or null");
        }
        final Hashtable<String, String> props = new Hashtable<>();
        final String principalName = formatPrincipal(username);
        
        if (StringUtils.isNotBlank(LDAP_SECURITY_AUTH)) {
            props.put(Context.SECURITY_AUTHENTICATION, LDAP_SECURITY_AUTH);
        }
        props.put(Context.SECURITY_PRINCIPAL, principalName);
        props.put(Context.SECURITY_CREDENTIALS, password);
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, LDAP_URL);

        try {
            return new InitialLdapContext(props, null);
        } catch (CommunicationException e) {
            LOGGER.error("Failed to connect to directory server", e);
            throw(e);
        } catch (NamingException e) {
            throw new NamingException("Failed to authenticate user");
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
        LdapContext ldapContext = null;
        try {
            ldapContext = getConnection(username, password);
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

    /**
     * Formats the principal in username@domain format or in a custom format if is specified in the config file.
	 * If LDAP_AUTH_USERNAME_FMT is configured to a non-empty value, the substring %s in this value will be replaced with the entered username.
	 * The recommended format of this value depends on your LDAP server(Active Directory, OpenLDAP, etc.).
	 * Examples:
	 *   alpine.ldap.auth.username.format=%s
	 * 	 alpine.ldap.auth.username.format=%s@company.com
	 *   alpine.ldap.auth.username.format=uid=%s,ou=People,dc=company,dc=com
	 *   alpine.ldap.auth.username.format=userPrincipalName=%s,ou=People,dc=company,dc=com
     * @param username the username
     * @return a formatted user principal
     * @since 1.2.0
     */
    private String formatPrincipal(String username) {
		if  (StringUtils.isNotBlank(LDAP_AUTH_USERNAME_FMT)) {
			return String.format(LDAP_AUTH_USERNAME_FMT, username);
		} else {
			if (StringUtils.isNotBlank(DOMAIN_NAME)) {
				return username + "@" + DOMAIN_NAME;
			}
		}
        return username;
    }

}
