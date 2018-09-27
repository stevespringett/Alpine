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
import org.apache.commons.lang3.StringUtils;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * A convenience wrapper for LDAP connections and commons LDAP tasks.
 *
 * @since 1.4.0
 */
public class LdapConnectionWrapper {

    private static final Logger LOGGER = Logger.getLogger(LdapConnectionWrapper.class);

    private static final String BIND_USERNAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BIND_USERNAME);
    private static final String BIND_PASSWORD = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BIND_PASSWORD);
    private static final String LDAP_SECURITY_AUTH = Config.getInstance().getProperty(Config.AlpineKey.LDAP_SECURITY_AUTH);
    private static final String LDAP_AUTH_USERNAME_FMT = Config.getInstance().getProperty(Config.AlpineKey.LDAP_AUTH_USERNAME_FMT);
    private static final String USER_GROUPS_FILTER = Config.getInstance().getProperty(Config.AlpineKey.LDAP_USER_GROUPS_FILTER);
    private static final String GROUPS_FILTER = Config.getInstance().getProperty(Config.AlpineKey.LDAP_GROUPS_FILTER);

    public static final boolean LDAP_ENABLED = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.LDAP_ENABLED);
    public static final String LDAP_URL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_SERVER_URL);
    public static final String DOMAIN_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_DOMAIN);
    public static final String BASE_DN = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BASEDN);
    public static final String ATTRIBUTE_MAIL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_MAIL);
    public static final String ATTRIBUTE_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_NAME);

    public static final boolean LDAP_CONFIGURED = (!LDAP_ENABLED || StringUtils.isBlank(LDAP_URL));
    private static final boolean IS_LDAP_SSLTLS = (LDAP_URL.startsWith("ldaps:"));


    /**
     * Asserts a users credentials. Returns an LdapContext if assertion is successful
     * or an exception for any other reason.
     *
     * @param username the username to assert
     * @param password the password to assert
     * @return the LdapContext upon a successful connection
     * @throws NamingException when unable to establish a connection
     * @since 1.4.0
     */
    public LdapContext getLdapContext(String username, String password) throws NamingException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new NamingException("Username or password cannot be empty or null");
        }
        final Hashtable<String, String> env = new Hashtable<>();
        final String principalName = formatPrincipal(username);

        if (StringUtils.isNotBlank(LDAP_SECURITY_AUTH)) {
            env.put(Context.SECURITY_AUTHENTICATION, LDAP_SECURITY_AUTH);
        }
        env.put(Context.SECURITY_PRINCIPAL, principalName);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        if (IS_LDAP_SSLTLS) {
            env.put("java.naming.ldap.factory.socket", "alpine.crypto.RelaxedSSLSocketFactory");
        }
        try {
            return new InitialLdapContext(env, null);
        } catch (CommunicationException e) {
            LOGGER.error("Failed to connect to directory server", e);
            throw(e);
        } catch (NamingException e) {
            throw new NamingException("Failed to authenticate user");
        }
    }

    /**
     * Creates a DirContext with the applications configuration settings.
     * @return a DirContext
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public DirContext getDirContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<>();
        final String principalName = formatPrincipal(BIND_USERNAME);
        env.put(Context.SECURITY_PRINCIPAL, principalName);
        env.put(Context.SECURITY_CREDENTIALS, BIND_PASSWORD);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        if (IS_LDAP_SSLTLS) {
            env.put("java.naming.ldap.factory.socket", "alpine.crypto.RelaxedSSLSocketFactory");
        }
        return new InitialDirContext(env);
    }

    /**
     * Retrieves a list of all groups the user is a member of.
     * @param dirContext a DirContext
     * @param ldapUser the LdapUser to retrieve group membership for
     * @return A list of Strings representing the fully qualified DN of each group
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public List<String> getGroups(DirContext dirContext, LdapUser ldapUser) throws NamingException {
        List<String> groupDns = new ArrayList<>();
        final String searchFilter = variableSubstitution(USER_GROUPS_FILTER, ldapUser);
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration ne = dirContext.search(BASE_DN, searchFilter, sc);
        while (ne != null && ne.hasMore()) {
            SearchResult result = (SearchResult)ne.next();
            groupDns.add(result.getNameInNamespace());
        }
        closeQuietly(ne);
        return groupDns;
    }

    /**
     * Retrieves a list of all the groups in the directory.
     * @param dirContext a DirContext
     * @return A list of Strings representing the fully qualified DN of each group
     * @throws NamingException if an exception if thrown
     * @since 1.4.0
     */
    public List<String> getGroups(DirContext dirContext) throws NamingException {
        List<String> groupDns = new ArrayList<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration ne = dirContext.search(BASE_DN, GROUPS_FILTER, sc);
        while (ne != null && ne.hasMore()) {
            SearchResult result = (SearchResult)ne.next();
            groupDns.add(result.getNameInNamespace());
        }
        closeQuietly(ne);
        return groupDns;
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
     * @since 1.4.0
     */
    public static String formatPrincipal(String username) {
        if  (StringUtils.isNotBlank(LDAP_AUTH_USERNAME_FMT)) {
            return String.format(LDAP_AUTH_USERNAME_FMT, username);
        } else {
            if (StringUtils.isNotBlank(DOMAIN_NAME)) {
                return username + "@" + DOMAIN_NAME;
            }
        }
        return username;
    }

    private String variableSubstitution(String s, LdapUser user) {
        if (s == null) {
            return null;
        }
        return s.replace("{USER_DN}", user.getDN());
    }

    /**
     * Closes a NamingEnumeration object without throwing any exceptions.
     * @param object the NamingEnumeration object to close
     * @since 1.4.0
     */
    public void closeQuietly(final NamingEnumeration object) {
        try {
            if (object != null) {
                object.close();
            }
        } catch (final NamingException e) {
            // ignore
        }
    }

    /**
     * Closes a DirContext object without throwing any exceptions.
     * @param object the DirContext object to close
     * @since 1.4.0
     */
    public void closeQuietly(final DirContext object) {
        try {
            if (object != null) {
                object.close();
            }
        } catch (final NamingException e) {
            // ignore
        }
    }

}
