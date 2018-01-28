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
package alpine.persistence;

import alpine.auth.ApiKeyGenerator;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.EventService;
import alpine.event.framework.LoggableSubscriber;
import alpine.event.framework.Subscriber;
import alpine.model.ApiKey;
import alpine.model.EventServiceLog;
import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.Team;
import alpine.model.UserPrincipal;
import alpine.resources.AlpineRequest;
import javax.jdo.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This QueryManager provides a concrete entension of {@link AbstractAlpineQueryManager} by
 * providing methods that operate on the default Alpine models such as ManagedUser and Team.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class AlpineQueryManager extends AbstractAlpineQueryManager {

    /**
     * Default constructor.
     */
    public AlpineQueryManager() {
        super();
    }

    /**
     * Constructs a new AlpineQueryManager.
     * @param request an AlpineRequest
     */
    public AlpineQueryManager(final AlpineRequest request) {
        super(request);
    }

    /**
     * Returns an API key.
     * @param key the key to return
     * @return an ApiKey
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public ApiKey getApiKey(final String key) {
        final Query query = pm.newQuery(ApiKey.class, "key == :key");
        final List<ApiKey> result = (List<ApiKey>) query.execute(key);
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * Regenerates an API key. This method does not create a new ApiKey object,
     * rather it uses the existing ApiKey object and simply creates a new
     * key string.
     * @param apiKey the ApiKey object to regenerate the key of.
     * @return an ApiKey
     * @since 1.0.0
     */
    public ApiKey regenerateApiKey(final ApiKey apiKey) {
        pm.currentTransaction().begin();
        apiKey.setKey(ApiKeyGenerator.generate());
        pm.currentTransaction().commit();
        return pm.getObjectById(ApiKey.class, apiKey.getId());
    }

    /**
     * Creates a new ApiKey object, including a cryptographically secure
     * API key string.
     * @param team The team to create the key for
     * @return an ApiKey
     */
    public ApiKey createApiKey(final Team team) {
        final List<Team> teams = new ArrayList<>();
        teams.add(team);
        pm.currentTransaction().begin();
        final ApiKey apiKey = new ApiKey();
        apiKey.setKey(ApiKeyGenerator.generate());
        apiKey.setTeams(teams);
        pm.makePersistent(apiKey);
        pm.currentTransaction().commit();
        return pm.getObjectById(ApiKey.class, apiKey.getId());
    }

    /**
     * Retrieves an LdapUser containing the specified username. If the username
     * does not exist, returns null.
     * @param username The username to retrieve
     * @return an LdapUser
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public LdapUser getLdapUser(final String username) {
        final Query query = pm.newQuery(LdapUser.class, "username == :username");
        final List<LdapUser> result = (List<LdapUser>) query.execute(username);
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * Returns a complete list of all LdapUser objects, in ascending order by username.
     * @return a list of LdapUsers
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public List<LdapUser> getLdapUsers() {
        final Query query = pm.newQuery(LdapUser.class);
        query.setOrdering("username asc");
        return (List<LdapUser>) query.execute();
    }

    /**
     * Creates a new LdapUser object with the specified username.
     * @param username The username of the new LdapUser. This must reference an existing username in the directory service
     * @return an LdapUser
     * @since 1.0.0
     */
    public LdapUser createLdapUser(final String username) {
        pm.currentTransaction().begin();
        final LdapUser user = new LdapUser();
        user.setUsername(username);
        user.setDN("Syncing...");
        pm.makePersistent(user);
        pm.currentTransaction().commit();
        EventService.getInstance().publish(new LdapSyncEvent(user.getUsername()));
        return getObjectById(LdapUser.class, user.getId());
    }

    /**
     * Updates the specified LdapUser.
     * @param transientUser the optionally detached LdapUser object to update.
     * @return an LdapUser
     * @since 1.0.0
     */
    public LdapUser updateLdapUser(final LdapUser transientUser) {
        final LdapUser user = getObjectById(LdapUser.class, transientUser.getId());
        pm.currentTransaction().begin();
        user.setDN(transientUser.getDN());
        pm.currentTransaction().commit();
        return pm.getObjectById(LdapUser.class, user.getId());
    }

    /**
     * Creates a new ManagedUser object.
     * @param username The username for the user
     * @param passwordHash The hashed password.
     * @return a ManagedUser
     * @see alpine.auth.PasswordService
     * @since 1.0.0
     */
    public ManagedUser createManagedUser(final String username, final String passwordHash) {
        pm.currentTransaction().begin();
        final ManagedUser user = new ManagedUser();
        user.setUsername(username);
        user.setPassword(passwordHash);
        user.setSuspended(false);
        pm.makePersistent(user);
        pm.currentTransaction().commit();
        return getObjectById(ManagedUser.class, user.getId());
    }

    /**
     * Returns a ManagedUser with the specified username. If the username
     * does not exist, returns null.
     * @param username The username to retrieve
     * @return a ManagedUser
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public ManagedUser getManagedUser(final String username) {
        final Query query = pm.newQuery(ManagedUser.class, "username == :username");
        final List<ManagedUser> result = (List<ManagedUser>) query.execute(username);
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * Returns a complete list of all ManagedUser objects, in ascending order by username.
     * @return a List of ManagedUsers
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public List<ManagedUser> getManagedUsers() {
        final Query query = pm.newQuery(ManagedUser.class);
        query.setOrdering("username asc");
        return (List<ManagedUser>) query.execute();
    }

    /**
     * Resolves a UserPrincipal. Default order resolution is to first match
     * on ManagedUser then on LdapUser. This may be configurable in a future
     * release.
     * @param username the username of the principal to retrieve
     * @return a UserPrincipal if found, null if not found
     * @since 1.0.0
     */
    public UserPrincipal getUserPrincipal(String username) {
        final UserPrincipal principal = getManagedUser(username);
        if (principal != null) {
            return principal;
        }
        return getLdapUser(username);
    }

    /**
     * Creates a new Team with the specified name. If createApiKey is true,
     * then {@link #createApiKey} is invoked and a cryptographically secure
     * API key is generated.
     * @param name The name of th team
     * @param createApiKey whether or not to create an API key for the team
     * @return a Team
     * @since 1.0.0
     */
    public Team createTeam(final String name, final boolean createApiKey) {
        pm.currentTransaction().begin();
        final Team team = new Team();
        team.setName(name);
        //todo assign permissions
        pm.makePersistent(team);
        pm.currentTransaction().commit();
        if (createApiKey) {
            createApiKey(team);
        }
        return getObjectByUuid(Team.class, team.getUuid(), Team.FetchGroup.ALL.name());
    }

    /**
     * Returns a complete list of all Team objects, in ascending order by name.
     * @return a List of Teams
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public List<Team> getTeams() {
        pm.getFetchPlan().addGroup(Team.FetchGroup.ALL.name());
        final Query query = pm.newQuery(Team.class);
        query.setOrdering("name asc");
        return (List<Team>) query.execute();
    }

    /**
     * Updates the specified Team.
     * @param transientTeam the optionally detached Team object to update
     * @return a Team
     * @since 1.0.0
     */
    public Team updateTeam(final Team transientTeam) {
        final Team team = getObjectByUuid(Team.class, transientTeam.getUuid());
        pm.currentTransaction().begin();
        team.setName(transientTeam.getName());
        //todo assign permissions
        pm.currentTransaction().commit();
        return pm.getObjectById(Team.class, team.getId());
    }

    /**
     * Associates a UserPrincipal to a Team.
     * @param user The user to bind
     * @param team The team to bind
     * @return true if operation was successful, false if not. This is not an indication of team association,
     * an unsuccessful return value may be due to the team or user not existing, or a binding that already
     * exists between the two.
     * @since 1.0.0
     */
    public boolean addUserToTeam(final UserPrincipal user, final Team team) {
        List<Team> teams = user.getTeams();
        boolean found = false;
        if (teams == null) {
            teams = new ArrayList<>();
        }
        for (Team t: teams) {
            if (team.getUuid().equals(t.getUuid())) {
                found = true;
            }
        }
        if (!found) {
            pm.currentTransaction().begin();
            teams.add(team);
            user.setTeams(teams);
            pm.currentTransaction().commit();
            return true;
        }
        return false;
    }

    /**
     * Removes the association of a UserPrincipal to a Team.
     * @param user The user to unbind
     * @param team The team to unbind
     * @return true if operation was successful, false if not. This is not an indication of team disassociation,
     * an unsuccessful return value may be due to the team or user not existing, or a binding that may not exist.
     * @since 1.0.0
     */
    public boolean removeUserFromTeam(final UserPrincipal user, final Team team) {
        final List<Team> teams = user.getTeams();
        if (teams == null) {
            return false;
        }
        boolean found = false;
        for (Team t: teams) {
            if (team.getUuid().equals(t.getUuid())) {
                found = true;
            }
        }
        if (found) {
            pm.currentTransaction().begin();
            teams.remove(team);
            user.setTeams(teams);
            pm.currentTransaction().commit();
            return true;
        }
        return false;
    }

    /**
     * Creates a new EventServiceLog. This method will automatically determine
     * if the subscriber is an implementation of {@link LoggableSubscriber} and
     * if so, will log the event. If not, then nothing will be logged and this
     * method will return null.
     * @param clazz the class of the subscriber task that handles the event
     * @return a new EventServiceLog
     */
    public EventServiceLog createEventServiceLog(Class<? extends Subscriber> clazz) {
        if (LoggableSubscriber.class.isAssignableFrom(clazz)) {
            pm.currentTransaction().begin();
            final EventServiceLog log = new EventServiceLog();
            log.setSubscriberClass(clazz.getCanonicalName());
            log.setStarted(new Timestamp(new Date().getTime()));
            pm.makePersistent(log);
            pm.currentTransaction().commit();
            return getObjectById(EventServiceLog.class, log.getId());
        }
        return null;
    }

    /**
     * Updates a EventServiceLog.
     * @param eventServiceLog the EventServiceLog to update
     * @return an updated EventServiceLog
     */
    public EventServiceLog updateEventServiceLog(EventServiceLog eventServiceLog) {
        if (eventServiceLog != null) {
            final EventServiceLog log = getObjectById(EventServiceLog.class, eventServiceLog.getId());
            if (log != null) {
                pm.currentTransaction().begin();
                log.setCompleted(new Timestamp(new Date().getTime()));
                pm.currentTransaction().commit();
                return pm.getObjectById(EventServiceLog.class, log.getId());
            }
        }
        return null;
    }

    /**
     * Returns the most recent log entry for the specified Subscriber.
     * If no log entries are found, this method will return null.
     * @param clazz The LoggableSubscriber class to query on
     * @return a EventServiceLog
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public EventServiceLog getLatestEventServiceLog(final Class<LoggableSubscriber> clazz) {
        final Query query = pm.newQuery(EventServiceLog.class, "eventClass == :clazz");
        query.setOrdering("completed desc");
        final List<EventServiceLog> result = (List<EventServiceLog>) query.execute(clazz);
        return result.size() == 0 ? null : result.get(0);
    }

}
