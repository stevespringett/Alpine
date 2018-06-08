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
package alpine.tasks;

import alpine.Config;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.Event;
import alpine.event.framework.Subscriber;
import alpine.logging.Logger;
import alpine.model.LdapUser;
import alpine.persistence.AlpineQueryManager;
import org.apache.commons.lang3.StringUtils;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * A task to synchronize LDAP users. This should be added to a concrete class that
 * extends {@link AlpineTaskScheduler}.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class LdapSyncTask implements Subscriber {

    private static final Logger LOGGER = Logger.getLogger(LdapSyncTask.class);
    private static final boolean LDAP_ENABLED = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.LDAP_ENABLED);
    private static final String LDAP_URL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_SERVER_URL);
    private static final String DOMAIN_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_DOMAIN);
    private static final String BASE_DN = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BASEDN);
    private static final String BIND_USERNAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BIND_USERNAME);
    private static final String BIND_PASSWORD = Config.getInstance().getProperty(Config.AlpineKey.LDAP_BIND_PASSWORD);
    private static final String ATTRIBUTE_MAIL = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_MAIL);
    private static final String LDAP_ATTRIBUTE_NAME = Config.getInstance().getProperty(Config.AlpineKey.LDAP_ATTRIBUTE_NAME);

    @Override
    public void inform(Event e) {

        if (!LDAP_ENABLED || StringUtils.isBlank(LDAP_URL)) {
            return;
        }

        if (e instanceof LdapSyncEvent) {
            LOGGER.info("Starting LDAP synchronization task");
            final LdapSyncEvent event = (LdapSyncEvent) e;

            final Hashtable<String, String> props = new Hashtable<>();
            final String principalName = formatPrincipal(BIND_USERNAME);
            props.put(Context.SECURITY_PRINCIPAL, principalName);
            props.put(Context.SECURITY_CREDENTIALS, BIND_PASSWORD);
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            props.put(Context.PROVIDER_URL, LDAP_URL);

            final String[] attributeFilter = {};
            final SearchControls sc = new SearchControls();
            sc.setReturningAttributes(attributeFilter);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            DirContext ctx = null;
            AlpineQueryManager qm = null;
            try {
                ctx = new InitialDirContext(props);
                qm = new AlpineQueryManager();

                if (event.getUsername() == null) {
                    // If username was null, we are going to sync all users
                    final List<LdapUser> users = qm.getLdapUsers();
                    for (LdapUser user: users) {
                        sync(ctx, qm, sc, user);
                    }
                } else {
                    // If username was specified, we will only sync the one
                    final LdapUser user = qm.getLdapUser(event.getUsername());
                    if (user != null) {
                        sync(ctx, qm, sc, user);
                    }
                }
            } catch (NamingException ex) {
                LOGGER.error("Error occurred during LDAP synchronization", ex);
            } finally {
                if (qm != null) {
                    qm.close();
                }
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException ex) {
                    }
                }
                LOGGER.info("LDAP synchronization complete");
            }
        }
    }

    /**
     * Performs the actual sync of the specified user.
     * @param ctx a DirContext
     * @param qm the AlpineQueryManager to use
     * @param sc the SearchControls to use
     * @param user the LdapUser instance to sync
     * @throws NamingException when a problem with the connection with the directory
     */
    private void sync(DirContext ctx, AlpineQueryManager qm, SearchControls sc, LdapUser user) throws NamingException {
        final String searchFor = LDAP_ATTRIBUTE_NAME + "=" + formatPrincipal(user.getUsername());

        LOGGER.debug("Syncing: " + user.getUsername());

        final List<SearchResult> results = Collections.list(ctx.search(BASE_DN, searchFor, sc));
        if (results.size() > 0) {
            // Should only return 1 result, but just in case, get the very first one
            final SearchResult result = results.get(0);

            user.setDN(result.getNameInNamespace());
            final Attribute mail = result.getAttributes().get(ATTRIBUTE_MAIL);
            if (mail != null) {
                // user.setMail(mail.get()); //todo
            }
        } else {
            // This is an invalid user - a username that exists in the database that does not exist in LDAP
            user.setDN("INVALID");
            // user.setMail(null); //todo
        }
        qm.updateLdapUser(user);
    }

    /**
     * Formats the principal in username@domain format.
     * @param username the username
     * @return a formatted user principal
     */
    private String formatPrincipal(String username) {
        if (StringUtils.isNotBlank(DOMAIN_NAME)) {
            return username + "@" + DOMAIN_NAME;
        }
        return username;
    }
}
