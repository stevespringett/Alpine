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

import alpine.auth.LdapConnectionWrapper;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.Event;
import alpine.event.framework.Subscriber;
import alpine.logging.Logger;
import alpine.model.LdapUser;
import alpine.persistence.AlpineQueryManager;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
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

    @Override
    public void inform(Event e) {

        if (!LdapConnectionWrapper.LDAP_CONFIGURED) {
            return;
        }

        if (e instanceof LdapSyncEvent) {
            LOGGER.info("Starting LDAP synchronization task");
            final LdapSyncEvent event = (LdapSyncEvent) e;
            final LdapConnectionWrapper ldap = new LdapConnectionWrapper();
            DirContext ctx = null;
            try (AlpineQueryManager qm = new AlpineQueryManager()) {
                ctx = ldap.createDirContext();
                if (event.getUsername() == null) {
                    // If username was null, we are going to sync all users
                    final List<LdapUser> users = qm.getLdapUsers();
                    for (LdapUser user: users) {
                        sync(ctx, qm, ldap, user);
                    }
                } else {
                    // If username was specified, we will only sync the one
                    final LdapUser user = qm.getLdapUser(event.getUsername());
                    if (user != null) {
                        sync(ctx, qm, ldap, user);
                    }
                }
            } catch (NamingException ex) {
                LOGGER.error("Error occurred during LDAP synchronization", ex);
            } finally {
                ldap.closeQuietly(ctx);
                LOGGER.info("LDAP synchronization complete");
            }
        }
    }

    /**
     * Performs the actual sync of the specified user.
     * @param ctx a DirContext
     * @param qm the AlpineQueryManager to use
     * @param ldap the LdapConnectionWrapper to use
     * @param user the LdapUser instance to sync
     * @throws NamingException when a problem with the connection with the directory
     */
    private void sync(DirContext ctx, AlpineQueryManager qm, LdapConnectionWrapper ldap, LdapUser user) throws NamingException {
        LOGGER.debug("Syncing: " + user.getUsername());
        if (user.getDN() == null || user.getDN().equals("INVALID")) {
            final List<SearchResult> results = ldap.searchForUsername(ctx, user.getUsername());
            if (results != null && results.size() > 0) {
                // Should only return 1 result, but just in case, get the very first one
                final SearchResult result = results.get(0);
                user.setDN(result.getNameInNamespace());
                user.setEmail(ldap.getAttribute(result, LdapConnectionWrapper.ATTRIBUTE_MAIL));
            } else {
                // This is an invalid user - a username that exists in the database that does not exist in LDAP
                user.setDN("INVALID");
                user.setEmail(null);
            }
        } else {
            final Attributes attributes = ctx.getAttributes(user.getDN());
            if (attributes == null || attributes.size() == 0) {
                user.setDN("INVALID");
                user.setEmail(null);
            } else {
                user.setEmail(ldap.getAttribute(attributes, LdapConnectionWrapper.ATTRIBUTE_MAIL));
            }
        }
        user = qm.updateLdapUser(user);
        // Dynamically assign team membership (if enabled)
        if (LdapConnectionWrapper.TEAM_SYNCHRONIZATION) {
            List<String> groupDNs = ldap.getGroups(ctx, user);
            qm.synchronizeTeamMembership(user, groupDNs);
        }
    }

}
