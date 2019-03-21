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
import alpine.validation.LdapStringSanitizer;
import org.apache.commons.lang3.StringUtils;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String BASE_DN = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BASEDN);
    public static final String ATTRIBUTE_MAIL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_MAIL);
    public static final String ATTRIBUTE_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_NAME);

    public static final boolean USER_PROVISIONING = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.LDAP_USER_PROVISIONING);
    public static final boolean TEAM_SYNCHRONIZATION = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.LDAP_TEAM_SYNCHRONIZATION);

    public static final boolean LDAP_CONFIGURED = LDAP_ENABLED && StringUtils.isNotBlank(LDAP_URL);
    private static final boolean IS_LDAP_SSLTLS = StringUtils.isNotBlank(LDAP_URL) && LDAP_URL.startsWith("ldaps:");


    /**
     * Asserts a users credentials. Returns an LdapContext if assertion is successful
     * or an exception for any other reason.
     *
     * @param userDn the users DN to assert
     * @param password the password to assert
     * @return the LdapContext upon a successful connection
     * @throws NamingException when unable to establish a connection
     * @since 1.4.0
     */
    public LdapContext createLdapContext(final String userDn, final String password) throws NamingException {
        if (StringUtils.isEmpty(userDn) || StringUtils.isEmpty(password)) {
            throw new NamingException("Username or password cannot be empty or null");
        }
        final Hashtable<String, String> env = new Hashtable<>();
        if (StringUtils.isNotBlank(LDAP_SECURITY_AUTH)) {
            env.put(Context.SECURITY_AUTHENTICATION, LDAP_SECURITY_AUTH);
        }
        env.put(Context.SECURITY_PRINCIPAL, userDn);
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
    public DirContext createDirContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.SECURITY_PRINCIPAL, BIND_USERNAME);
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
    public List<String> getGroups(final DirContext dirContext, final LdapUser ldapUser) throws NamingException {
        final List<String> groupDns = new ArrayList<>();
        final String searchFilter = variableSubstitution(USER_GROUPS_FILTER, ldapUser);
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final NamingEnumeration<SearchResult> ne = dirContext.search(BASE_DN, searchFilter, sc);
        while (hasMoreEnum(ne)) {
            final SearchResult result = ne.next();
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
    public List<String> getGroups(final DirContext dirContext) throws NamingException {
        final List<String> groupDns = new ArrayList<>();
        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final NamingEnumeration<SearchResult> ne = dirContext.search(BASE_DN, GROUPS_FILTER, sc);
        while (hasMoreEnum(ne)) {
            final SearchResult result = ne.next();
            groupDns.add(result.getNameInNamespace());
        }
        closeQuietly(ne);
        return groupDns;
    }

    /**
     * Performs a search for the specified username. Internally, this method queries on
     * the attribute defined by {@link Config.AlpineKey#LDAP_ATTRIBUTE_NAME}.
     * @param ctx the DirContext to use
     * @param username the username to query on
     * @return a list of SearchResult objects. If the username is found, the list should typically only contain one result.
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public List<SearchResult> searchForUsername(final DirContext ctx, final String username) throws NamingException {
        final String[] attributeFilter = {};
        final SearchControls sc = new SearchControls();
        sc.setReturningAttributes(attributeFilter);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String searchFor = LdapConnectionWrapper.ATTRIBUTE_NAME + "=" +
                LdapStringSanitizer.sanitize(formatPrincipal(username));
        return Collections.list(ctx.search(LdapConnectionWrapper.BASE_DN, searchFor, sc));
    }

    /**
     * Performs a search for the specified username. Internally, this method queries on
     * the attribute defined by {@link Config.AlpineKey#LDAP_ATTRIBUTE_NAME}.
     * @param ctx the DirContext to use
     * @param username the username to query on
     * @return a list of SearchResult objects. If the username is found, the list should typically only contain one result.
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public SearchResult searchForSingleUsername(final DirContext ctx, final String username) throws NamingException {
        final List<SearchResult> results = searchForUsername(ctx, username);
        if (results == null || results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new NamingException("Multiple entries in the directory contain the same username. This scenario is not supported");
        }
    }

    /**
     * Retrieves an attribute by its name for the specified dn.
     * @param ctx the DirContext to use
     * @param dn the distinguished name of the entry to obtain the attribute value for
     * @param attributeName the name of the attribute to return
     * @return the value of the attribute, or null if not found
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public String getAttribute(final DirContext ctx, final String dn, final String attributeName) throws NamingException {
        final Attributes attributes = ctx.getAttributes(dn);
        return getAttribute(attributes, attributeName);
    }

    /**
     * Retrieves an attribute by its name for the specified search result.
     * @param result the search result of the entry to obtain the attribute value for
     * @param attributeName the name of the attribute to return
     * @return the value of the attribute, or null if not found
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public String getAttribute(final SearchResult result, final String attributeName) throws NamingException {
        return getAttribute(result.getAttributes(), attributeName);
    }

    /**
     * Retrieves an attribute by its name.
     * @param attributes the list of attributes to query on
     * @param attributeName the name of the attribute to return
     * @return the value of the attribute, or null if not found
     * @throws NamingException if an exception is thrown
     * @since 1.4.0
     */
    public String getAttribute(final Attributes attributes, final String attributeName) throws NamingException {
        if (attributes == null || attributes.size() == 0) {
            return null;
        } else {
            final Attribute attribute = attributes.get(attributeName);
            if (attribute != null) {
                final Object o = attribute.get();
                if (o instanceof String) {
                    return (String) attribute.get();
                }
            }
        }
        return null;
    }

    /**
     * Formats the principal in username@domain format or in a custom format if is specified in the config file.
     * If LDAP_AUTH_USERNAME_FMT is configured to a non-empty value, the substring %s in this value will be replaced with the entered username.
     * The recommended format of this value depends on your LDAP server(Active Directory, OpenLDAP, etc.).
     * Examples:
     *   alpine.ldap.auth.username.format=%s
     * 	 alpine.ldap.auth.username.format=%s@company.com
     * @param username the username
     * @return a formatted user principal
     * @since 1.4.0
     */
    private static String formatPrincipal(final String username) {
        if  (StringUtils.isNotBlank(LDAP_AUTH_USERNAME_FMT)) {
            return String.format(LDAP_AUTH_USERNAME_FMT, username);
        }
        return username;
    }

    private String variableSubstitution(final String s, final LdapUser user) {
        if (s == null) {
            return null;
        }
        return s.replace("{USER_DN}", LdapStringSanitizer.sanitize(user.getDN()));
    }

    /**
     * Convenience method that wraps {@link NamingEnumeration#hasMore()} but ignores {@link PartialResultException}s
     * that may be thrown as a result. This is typically an issue with a directory server that does not support
     * {@link Context#REFERRAL} being set to 'ignore' (which is the default value).
     *
     * Issue: https://github.com/stevespringett/Alpine/issues/19
     * @since 1.4.3
     */
    private boolean hasMoreEnum(final NamingEnumeration<SearchResult> ne) throws NamingException {
        if (ne == null) {
            return false;
        }
        boolean hasMore = true;
        try {
            if (!ne.hasMore()) {
                hasMore = false;
            }
        } catch (PartialResultException e) {
            hasMore = false;
            LOGGER.warn("Partial results returned. If this is an Active Directory server, try using port 3268 or 3269 in " + Config.AlpineKey.LDAP_SERVER_URL.name());
        }
        return hasMore;
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
