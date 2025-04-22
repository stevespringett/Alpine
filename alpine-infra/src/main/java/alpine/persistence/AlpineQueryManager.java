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
package alpine.persistence;

import alpine.common.logging.Logger;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.EventService;
import alpine.event.framework.LoggableSubscriber;
import alpine.event.framework.Subscriber;
import alpine.model.ApiKey;
import alpine.model.ConfigProperty;
import alpine.model.EventServiceLog;
import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.MappedLdapGroup;
import alpine.model.MappedOidcGroup;
import alpine.model.OidcGroup;
import alpine.model.OidcUser;
import alpine.model.Permission;
import alpine.model.Team;
import alpine.model.UserPrincipal;
import alpine.resources.AlpineRequest;
import alpine.security.ApiKeyGenerator;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This QueryManager provides a concrete extension of {@link AbstractAlpineQueryManager} by
 * providing methods that operate on the default Alpine models such as ManagedUser and Team.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class AlpineQueryManager extends AbstractAlpineQueryManager {

    private static final Logger LOGGER = Logger.getLogger(AlpineQueryManager.class);

    /**
     * Default constructor.
     */
    public AlpineQueryManager() {
        super();
    }

    /**
     * Constructs a new AlpineQueryManager.
     * @param pm a PersistenceManager
     */
    public AlpineQueryManager(final PersistenceManager pm) {
        super(pm);
    }

    /**
     * Constructs a new AlpineQueryManager.
     * @param request an AlpineRequest
     */
    public AlpineQueryManager(final AlpineRequest request) {
        super(request);
    }

    /**
     * Constructs a new AlpineQueryManager.
     * @param pm a PersistenceManager
     * @param request an AlpineRequest
     * @since 1.9.3
     */
    public AlpineQueryManager(final PersistenceManager pm, final AlpineRequest request) {
        super(pm, request);
    }

    /**
     * Returns an API key by the public ID.
     * @param publicId the public ID for the key to return
     * @return an ApiKey
     * @since 3.2.0
     */
    public ApiKey getApiKeyByPublicId(final String publicId) {
        final Query<ApiKey> query = pm.newQuery(ApiKey.class, "publicId == :publicId");
        query.setParameters(publicId);
        return executeAndCloseUnique(query);
    }

    /**
     * Regenerates an API key. This method does not create a new ApiKey object,
     * rather it uses the existing ApiKey object and simply creates a new
     * key string.
     * @param apiKey the ApiKey object to regenerate the key of.
     * @return an ApiKey
     * @since 3.2.0
     */
    public ApiKey regenerateApiKey(final ApiKey apiKey) {
        final var generatedApiKey = ApiKeyGenerator.generate(apiKey.getPublicId());

        return callInTransaction(() -> {
            apiKey.setKey(generatedApiKey.getKey());
            apiKey.setSecretHash(generatedApiKey.getSecretHash());
            return pm.makePersistent(apiKey);
        });
    }

    /**
     * Creates a new ApiKey object, including a cryptographically secure
     * API key string.
     * @param team The team to create the key for
     * @return an ApiKey
     * @since 3.2.0
     */
    public ApiKey createApiKey(final Team team) {
        final ApiKey generatedApiKey = ApiKeyGenerator.generate();

        return callInTransaction(() -> {
            final var apiKey = new ApiKey();
            apiKey.setKey(generatedApiKey.getKey());
            apiKey.setPublicId(generatedApiKey.getPublicId());
            apiKey.setSecret(generatedApiKey.getSecret());
            apiKey.setSecretHash(generatedApiKey.getSecretHash());
            apiKey.setCreated(new Date());
            apiKey.setTeams(List.of(team));
            return pm.makePersistent(apiKey);
        });
    }

    public ApiKey updateApiKey(final ApiKey transientApiKey) {
        return callInTransaction(() -> {
            final ApiKey apiKey = getObjectById(ApiKey.class, transientApiKey.getId());
            apiKey.setComment(transientApiKey.getComment());
            return apiKey;
        });
    }

    /**
     * Creates a new OidcUser object with the specified username.
     * @param username The username of the new OidcUser. This must reference an
     *                 existing username in the OpenID Connect identity provider.
     * @return an LdapUser
     * @since 1.8.0
     */
    public OidcUser createOidcUser(final String username) {
        return callInTransaction(() -> {
            final var user = new OidcUser();
            user.setUsername(username);
            // Subject identifier and email will be synced when a
            // user with the given username signs in for the first time
            return pm.makePersistent(user);
        });
    }

    /**
     * Updates the specified OidcUser.
     * @param transientUser the optionally detached OidcUser object to update.
     * @return an OidcUser
     * @since 1.8.0
     */
    public OidcUser updateOidcUser(final OidcUser transientUser) {
        return callInTransaction(() -> {
            final OidcUser user = getObjectById(OidcUser.class, transientUser.getId());
            user.setSubjectIdentifier(transientUser.getSubjectIdentifier());
            user.setEmail(transientUser.getEmail());
            return user;
        });
    }

    /**
     * Retrieves an OidcUser containing the specified username. If the username
     * does not exist, returns null.
     * @param username The username to retrieve
     * @return an OidcUser
     * @since 1.8.0
     */
    public OidcUser getOidcUser(final String username) {
        final Query<OidcUser> query = pm.newQuery(OidcUser.class, "username == :username");
        query.setParameters(username);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a complete list of all OidcUser objects, in ascending order by username.
     * @return a list of OidcUser
     * @since 1.8.0
     */
    public List<OidcUser> getOidcUsers() {
        final Query<OidcUser> query = pm.newQuery(OidcUser.class);
        query.setOrdering("username asc");
        return executeAndCloseList(query);
    }

    /**
     * Creates a OidcGroup.
     * @param name Name of the group to create
     * @return a OidcGroup
     * @since 1.8.0
     */
    public OidcGroup createOidcGroup(final String name) {
        return callInTransaction(() -> {
            final var group = new OidcGroup();
            group.setName(name);
            return pm.makePersistent(group);
        });
    }

    /**
     * Updates a OidcGroup.
     * @param oidcGroup The group to update
     * @return a refreshed OidcGroup
     * @since 1.8.0
     */
    public OidcGroup updateOidcGroup(final OidcGroup oidcGroup) {
        return callInTransaction(() -> {
            final OidcGroup oidcGroupToUpdate = getObjectByUuid(OidcGroup.class, oidcGroup.getUuid());
            oidcGroupToUpdate.setName(oidcGroup.getName());
            return oidcGroupToUpdate;
        });
    }

    /**
     * Returns a complete list of all OidcGroup objects, in ascending order by name.
     * @return a list of OidcGroup
     * @since 1.8.0
     */
    public List<OidcGroup> getOidcGroups() {
        final Query<OidcGroup> query = pm.newQuery(OidcGroup.class);
        query.setOrdering("name asc");
        return executeAndCloseList(query);
    }

    /**
     * Returns an OidcGroup containing the specified name. If the name
     * does not exist, returns null.
     * @param name Name of the group to retrieve
     * @return an OidcGroup
     * @since 1.8.0
     */
    public OidcGroup getOidcGroup(final String name) {
        final Query<OidcGroup> query = pm.newQuery(OidcGroup.class, "name == :name");
        query.setParameters(name);
        return executeAndCloseUnique(query);
    }

    /**
     * This method dynamically assigns team membership to the specified user from
     * the list of OpenID Connect groups the user is a member of. The method will look
     * up any {@link MappedOidcGroup}s and ensure the user is only a member of the
     * teams that have a mapping to an OpenID Connect group for which the user is a member.
     * @param user the OpenID Connect user to sync team membership for
     * @param groupNames a list of OpenID Connect groups the user is a member of
     * @return a refreshed OidcUser object
     * @since 1.8.0
     */
    public OidcUser synchronizeTeamMembership(final OidcUser user, final List<String> groupNames) {
        LOGGER.debug("Synchronizing team membership for OpenID Connect user " + user.getUsername());
        return callInTransaction(() -> {
            final List<Team> removeThese = new ArrayList<>();
            if (user.getTeams() != null) {
                for (final Team team : user.getTeams()) {
                    LOGGER.debug(user.getUsername() + " is a member of team: " + team.getName());
                    if (team.getMappedOidcGroups() != null && !team.getMappedOidcGroups().isEmpty()) {
                        for (final MappedOidcGroup mappedOidcGroup : team.getMappedOidcGroups()) {
                            LOGGER.debug(mappedOidcGroup.getGroup().getName() + " is mapped to team: " + team.getName());
                            if (!groupNames.contains(mappedOidcGroup.getGroup().getName())) {
                                LOGGER.debug(mappedOidcGroup.getGroup().getName() + " is not identified in the List of groups specified. Queuing removal of membership for user " + user.getUsername());
                                removeThese.add(team);
                            }
                        }
                    } else {
                        LOGGER.debug(team.getName() + " does not have any mapped OpenID Connect groups. Queuing removal of " + user.getUsername() + " from team: " + team.getName());
                        removeThese.add(team);
                    }
                }
            }

            for (final Team team : removeThese) {
                LOGGER.debug("Removing user: " + user.getUsername() + " from team: " + team.getName());
                removeUserFromTeam(user, team);
            }

            for (final String groupName : groupNames) {
                final OidcGroup group = getOidcGroup(groupName);
                if (group == null) {
                    LOGGER.debug("Unknown OpenID Connect group " + groupName);
                    continue;
                }

                for (final MappedOidcGroup mappedOidcGroup : getMappedOidcGroups(group)) {
                    LOGGER.debug("Adding user: " + user.getUsername() + " to team: " + mappedOidcGroup.getTeam().getName());
                    addUserToTeam(user, mappedOidcGroup.getTeam());
                }
            }

            return user;
        });
    }

    /**
     * This method adds the specified user to teams with the specified names. It does not
     * remove the user from any teams, and silently ignores references to teams that do not exist.
     * @param user the OpenID Connect user to sync team membership for
     * @param teamNames a list of teams the user is a member of
     * @return a refreshed OidcUser object
     * @since 2.2.5
     */
    public OidcUser addUserToTeams(final OidcUser user, final List<String> teamNames) {
        LOGGER.debug("Synchronizing team membership for OpenID Connect user " + user.getUsername());

        return callInTransaction(() -> {
            for (final String teamName : teamNames) {
                Team team = getTeam(teamName);
                if (team == null) {
                    LOGGER.warn("Cannot add user " + user.getUsername() + " to team " + teamName + ", because no team with that name exists");
                } else {
                    LOGGER.debug("Adding user: " + user.getUsername() + " to team: " + teamName);
                    addUserToTeam(user, team);
                }
            }

            return user;
        });
    }

    /**
     * Retrieves an LdapUser containing the specified username. If the username
     * does not exist, returns null.
     * @param username The username to retrieve
     * @return an LdapUser
     * @since 1.0.0
     */
    public LdapUser getLdapUser(final String username) {
        final Query<LdapUser> query = pm.newQuery(LdapUser.class, "username == :username");
        query.setParameters(username);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a complete list of all LdapUser objects, in ascending order by username.
     * @return a list of LdapUsers
     * @since 1.0.0
     */
    public List<LdapUser> getLdapUsers() {
        final Query<LdapUser> query = pm.newQuery(LdapUser.class);
        query.setOrdering("username asc");
        return executeAndCloseList(query);
    }

    /**
     * Creates a new LdapUser object with the specified username.
     * @param username The username of the new LdapUser. This must reference an existing username in the directory service
     * @return an LdapUser
     * @since 1.0.0
     */
    public LdapUser createLdapUser(final String username) {
        final LdapUser createdUser = callInTransaction(() -> {
            final var user = new LdapUser();
            user.setUsername(username);
            user.setDN("Syncing...");
            return pm.makePersistent(user);
        });
        EventService.getInstance().publish(new LdapSyncEvent(createdUser.getUsername()));
        return createdUser;
    }

    /**
     * Updates the specified LdapUser.
     * @param transientUser the optionally detached LdapUser object to update.
     * @return an LdapUser
     * @since 1.0.0
     */
    public LdapUser updateLdapUser(final LdapUser transientUser) {
        return callInTransaction(() -> {
            final LdapUser user = getObjectById(LdapUser.class, transientUser.getId());
            user.setDN(transientUser.getDN());
            return user;
        });
    }

    /**
     * This method dynamically assigns team membership to the specified user from
     * the list of LDAP group DN's the user is a member of. The method will look
     * up any {@link MappedLdapGroup}s and ensure the user is only a member of the
     * teams that have a mapping to an LDAP group for which the user is a member.
     * @param user the LDAP user to sync team membership for
     * @param groupDNs a list of LDAP group DNs the user is a member of
     * @return a refreshed LdapUser object
     * @since 1.4.0
     */
    public LdapUser synchronizeTeamMembership(final LdapUser user, final List<String> groupDNs) {
        LOGGER.debug("Synchronizing team membership for " + user.getUsername());
        return callInTransaction(() -> {
            final List<Team> removeThese = new ArrayList<>();
            if (user.getTeams() != null) {
                for (final Team team : user.getTeams()) {
                    LOGGER.debug(user.getUsername() + " is a member of team: " + team.getName());
                    if (team.getMappedLdapGroups() != null) {
                        for (final MappedLdapGroup mappedLdapGroup : team.getMappedLdapGroups()) {
                            LOGGER.debug(mappedLdapGroup.getDn() + " is mapped to team: " + team.getName());
                            if (!groupDNs.contains(mappedLdapGroup.getDn())) {
                                LOGGER.debug(mappedLdapGroup.getDn() + " is not identified in the List of group DNs specified. Queuing removal of membership for user " + user.getUsername());
                                removeThese.add(team);
                            }
                        }
                    } else {
                        LOGGER.debug(team.getName() + " does not have any mapped LDAP groups. Queuing removal of " + user.getUsername() + " from team: " + team.getName());
                        removeThese.add(team);
                    }
                }
            }
            for (final Team team: removeThese) {
                LOGGER.debug("Removing user: " + user.getUsername() + " from team: " + team.getName());
                removeUserFromTeam(user, team);
            }
            for (final String groupDN: groupDNs) {
                for (final MappedLdapGroup mappedLdapGroup: getMappedLdapGroups(groupDN)) {
                    LOGGER.debug("Adding user: " + user.getUsername() + " to team: " + mappedLdapGroup.getTeam());
                    addUserToTeam(user, mappedLdapGroup.getTeam());
                }
            }
            return user;
        });
    }

    /**
     * Creates a new ManagedUser object.
     * @param username The username for the user
     * @param passwordHash The hashed password.
     * @return a ManagedUser
     * @since 1.0.0
     */
    public ManagedUser createManagedUser(final String username, final String passwordHash) {
        return createManagedUser(username, null, null, passwordHash, false, false, false);
    }

    /**
     * Creates a new ManagedUser object.
     * @param username The username for the user
     * @param fullname The fullname of the user
     * @param email The users email address
     * @param passwordHash The hashed password
     * @param forcePasswordChange Whether or not user needs to change password on next login or not
     * @param nonExpiryPassword Whether or not the users password ever expires or not
     * @param suspended Whether or not user being created is suspended or not
     * @return a ManagedUser
     * @since 1.1.0
     */
    public ManagedUser createManagedUser(final String username, final String fullname, final String email,
                                         final String passwordHash, final boolean forcePasswordChange,
                                         final boolean nonExpiryPassword, final boolean suspended) {
        return callInTransaction(() -> {
            final var user = new ManagedUser();
            user.setUsername(username);
            user.setFullname(fullname);
            user.setEmail(email);
            user.setPassword(passwordHash);
            user.setForcePasswordChange(forcePasswordChange);
            user.setNonExpiryPassword(nonExpiryPassword);
            user.setSuspended(suspended);
            user.setLastPasswordChange(new Date());
            return pm.makePersistent(user);
        });
    }

    /**
     * Updates the specified ManagedUser.
     * @param transientUser the optionally detached ManagedUser object to update.
     * @return an ManagedUser
     * @since 1.0.0
     */
    public ManagedUser updateManagedUser(final ManagedUser transientUser) {
        return callInTransaction(() -> {
            final ManagedUser user = getObjectById(ManagedUser.class, transientUser.getId());
            user.setFullname(transientUser.getFullname());
            user.setEmail(transientUser.getEmail());
            user.setForcePasswordChange(transientUser.isForcePasswordChange());
            user.setNonExpiryPassword(transientUser.isNonExpiryPassword());
            user.setSuspended(transientUser.isSuspended());
            if (transientUser.getPassword() != null) {
                if (!user.getPassword().equals(transientUser.getPassword())) {
                    user.setLastPasswordChange(new Date());
                }
                user.setPassword(transientUser.getPassword());
            }
            return user;
        });
    }

    /**
     * Returns a ManagedUser with the specified username. If the username
     * does not exist, returns null.
     * @param username The username to retrieve
     * @return a ManagedUser
     * @since 1.0.0
     */
    public ManagedUser getManagedUser(final String username) {
        final Query<ManagedUser> query = pm.newQuery(ManagedUser.class, "username == :username");
        query.setParameters(username);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a complete list of all ManagedUser objects, in ascending order by username.
     * @return a List of ManagedUsers
     * @since 1.0.0
     */
    public List<ManagedUser> getManagedUsers() {
        final Query<ManagedUser> query = pm.newQuery(ManagedUser.class);
        query.setOrdering("username asc");
        return executeAndCloseList(query);
    }

    /**
     * Resolves a UserPrincipal. Default order resolution is to first match
     * on ManagedUser then on LdapUser and finally on OidcUser. This may be
     * configurable in a future release.
     * @param username the username of the principal to retrieve
     * @return a UserPrincipal if found, null if not found
     * @since 1.0.0
     */
    public UserPrincipal getUserPrincipal(String username) {
        UserPrincipal principal = getManagedUser(username);
        if (principal != null) {
            return principal;
        }
        principal = getLdapUser(username);
        if (principal != null) {
            return principal;
        }
        return getOidcUser(username);
    }

    /**
     * Creates a new Team with the specified name. If createApiKey is true,
     * then {@link #createApiKey} is invoked and a cryptographically secure
     * API key is generated.
     * @param name The name of the team
     * @param createApiKey whether to create an API key for the team
     * @deprecated `createApiKey` is deprecated and will be removed in future versions
     * @return a Team
     * @since 1.0.0
     */
    public Team createTeam(final String name, final boolean createApiKey) {
        return createTeam(name);
    }

    /**
     * Creates a new Team with the specified name.
     * @param name The name of the team
     * @return a Team
     * @since 3.2.0
     */
    public Team createTeam(final String name) {
        return callInTransaction(() -> {
            final var team = new Team();
            team.setName(name);
            //todo assign permissions
            pm.makePersistent(team);
            return team;
        });
    }

    /**
     * Returns a complete list of all Team objects, in ascending order by name.
     * @return a List of Teams
     * @since 1.0.0
     */
    public List<Team> getTeams() {
        final Query<Team> query = pm.newQuery(Team.class);
        query.getFetchPlan().addGroup(Team.FetchGroup.ALL.name());
        query.setOrdering("name asc");
        return executeAndCloseList(query);
    }

    /**
     * Returns a Team containing the specified name. If the name
     * does not exist, returns null.
     * @param name Name of the team to retrieve
     * @return a Team
     * @since 2.2.5
     */
    public Team getTeam(final String name) {
        final Query<Team> query = pm.newQuery(Team.class, "name == :name");
        query.setParameters(name);
        return executeAndCloseUnique(query);
    }

    /**
     * Updates the specified Team.
     * @param transientTeam the optionally detached Team object to update
     * @return a Team
     * @since 1.0.0
     */
    public Team updateTeam(final Team transientTeam) {
        return callInTransaction(() -> {
            final Team team = getObjectByUuid(Team.class, transientTeam.getUuid());
            team.setName(transientTeam.getName());
            //todo assign permissions
            return team;
        });
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
        return callInTransaction(() -> {
            List<Team> teams = user.getTeams();
            boolean found = false;
            if (teams == null) {
                teams = new ArrayList<>();
            }
            for (final Team t: teams) {
                if (team.getUuid().equals(t.getUuid())) {
                    found = true;
                }
            }
            if (!found) {
                teams.add(team);
                user.setTeams(teams);
                return true;
            }
            return false;
        });
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
        return callInTransaction(() -> {
            final List<Team> teams = user.getTeams();
            if (teams == null) {
                return false;
            }
            boolean found = false;
            for (final Team t: teams) {
                if (team.getUuid().equals(t.getUuid())) {
                    found = true;
                }
            }
            if (found) {
                teams.remove(team);
                user.setTeams(teams);
                return true;
            }
            return false;
        });
    }

    /**
     * Creates a Permission object.
     * @param name The name of the permission
     * @param description the permissions description
     * @return a Permission
     * @since 1.1.0
     */
    public Permission createPermission(final String name, final String description) {
        return callInTransaction(() -> {
            final var permission = new Permission();
            permission.setName(name);
            permission.setDescription(description);
            return pm.makePersistent(permission);
        });
    }

    /**
     * Retrieves a Permission by its name.
     * @param name The name of the permission
     * @return a Permission
     * @since 1.1.0
     */
    public Permission getPermission(final String name) {
        final Query<Permission> query = pm.newQuery(Permission.class, "name == :name");
        query.setParameters(name);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a list of all Permissions defined in the system.
     * @return a List of Permission objects
     * @since 1.1.0
     */
    public List<Permission> getPermissions() {
        final Query<Permission> query = pm.newQuery(Permission.class);
        query.setOrdering("name asc");
        return executeAndCloseList(query);
    }

    /**
     * Determines the effective permissions for the specified user by collecting
     * a List of all permissions assigned to the user either directly, or through
     * team membership.
     * @param user the user to retrieve permissions for
     * @return a List of Permission objects
     * @deprecated Use {@link #getEffectivePermissions(Principal)} instead.
     * @since 1.1.0
     */
    @Deprecated(forRemoval = true, since = "3.2.0")
    public List<Permission> getEffectivePermissions(UserPrincipal user) {
        final LinkedHashSet<Permission> permissions = new LinkedHashSet<>();
        if (user.getPermissions() != null) {
            permissions.addAll(user.getPermissions());
        }
        if (user.getTeams() != null) {
            for (final Team team: user.getTeams()) {
                final List<Permission> teamPermissions = getObjectById(Team.class, team.getId()).getPermissions();
                if (teamPermissions != null) {
                    permissions.addAll(teamPermissions);
                }
            }
        }
        return new ArrayList<>(permissions);
    }

    /**
     * Retrieve the effective permissions of a {@link Principal}.
     *
     * @param principal The {@link Principal} to retrieve permissions for.
     * @return Permissions of {@code principal}
     * @since 3.2.0
     */
    public Set<String> getEffectivePermissions(final Principal principal) {
        return switch (principal) {
            case ApiKey apiKey -> getEffectivePermissions(apiKey);
            case LdapUser ldapUser -> getEffectivePermissions(ldapUser);
            case ManagedUser managedUser -> getEffectivePermissions(managedUser);
            case OidcUser oidcUser -> getEffectivePermissions(oidcUser);
            default -> Collections.emptySet();
        };
    }

    private Set<String> getEffectivePermissions(final ApiKey apiKey) {
        final Query<?> query = pm.newQuery(Query.SQL, /* language=SQL */ """
                SELECT "PERMISSION"."NAME"
                  FROM "APIKEY"
                 INNER JOIN "APIKEYS_TEAMS"
                    ON "APIKEYS_TEAMS"."APIKEY_ID" = "APIKEY"."ID"
                 INNER JOIN "TEAM"
                    ON "TEAM"."ID" = "APIKEYS_TEAMS"."TEAM_ID"
                 INNER JOIN "TEAMS_PERMISSIONS"
                    ON "TEAMS_PERMISSIONS"."TEAM_ID" = "TEAM"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "TEAMS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "APIKEY"."ID" = :apiKeyId
                """);
        query.setNamedParameters(Map.of("apiKeyId", apiKey.getId()));
        return Set.copyOf(executeAndCloseResultList(query, String.class));
    }

    private Set<String> getEffectivePermissions(final LdapUser ldapUser) {
        final Query<?> query = pm.newQuery(Query.SQL, /* language=SQL */ """
                SELECT "PERMISSION"."NAME"
                  FROM "LDAPUSER"
                 INNER JOIN "LDAPUSERS_TEAMS"
                    ON "LDAPUSERS_TEAMS"."LDAPUSER_ID" = "LDAPUSER"."ID"
                 INNER JOIN "TEAM"
                    ON "TEAM"."ID" = "LDAPUSERS_TEAMS"."TEAM_ID"
                 INNER JOIN "TEAMS_PERMISSIONS"
                    ON "TEAMS_PERMISSIONS"."TEAM_ID" = "TEAM"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "TEAMS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "LDAPUSER"."ID" = :ldapUserId
                 UNION ALL
                SELECT "PERMISSION"."NAME"
                  FROM "LDAPUSER"
                 INNER JOIN "LDAPUSERS_PERMISSIONS"
                    ON "LDAPUSERS_PERMISSIONS"."LDAPUSER_ID" = "LDAPUSER"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "LDAPUSERS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "LDAPUSER"."ID" = :ldapUserId
                """);
        query.setNamedParameters(Map.of("ldapUserId", ldapUser.getId()));
        return Set.copyOf(executeAndCloseResultList(query, String.class));
    }

    private Set<String> getEffectivePermissions(final ManagedUser managedUser) {
        final Query<?> query = pm.newQuery(Query.SQL, /* language=SQL */ """
                SELECT "PERMISSION"."NAME"
                  FROM "MANAGEDUSER"
                 INNER JOIN "MANAGEDUSERS_TEAMS"
                    ON "MANAGEDUSERS_TEAMS"."MANAGEDUSER_ID" = "MANAGEDUSER"."ID"
                 INNER JOIN "TEAM"
                    ON "TEAM"."ID" = "MANAGEDUSERS_TEAMS"."TEAM_ID"
                 INNER JOIN "TEAMS_PERMISSIONS"
                    ON "TEAMS_PERMISSIONS"."TEAM_ID" = "TEAM"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "TEAMS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "MANAGEDUSER"."ID" = :managedUserId
                 UNION ALL
                SELECT "PERMISSION"."NAME"
                  FROM "MANAGEDUSER"
                 INNER JOIN "MANAGEDUSERS_PERMISSIONS"
                    ON "MANAGEDUSERS_PERMISSIONS"."MANAGEDUSER_ID" = "MANAGEDUSER"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "MANAGEDUSERS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "MANAGEDUSER"."ID" = :managedUserId
                """);
        query.setNamedParameters(Map.of("managedUserId", managedUser.getId()));
        return Set.copyOf(executeAndCloseResultList(query, String.class));
    }

    private Set<String> getEffectivePermissions(final OidcUser oidcUser) {
        final Query<?> query = pm.newQuery(Query.SQL, /* language=SQL */ """
                SELECT "PERMISSION"."NAME"
                  FROM "OIDCUSER"
                 INNER JOIN "OIDCUSERS_TEAMS"
                    ON "OIDCUSERS_TEAMS"."OIDCUSERS_ID" = "OIDCUSER"."ID"
                 INNER JOIN "TEAM"
                    ON "TEAM"."ID" = "OIDCUSERS_TEAMS"."TEAM_ID"
                 INNER JOIN "TEAMS_PERMISSIONS"
                    ON "TEAMS_PERMISSIONS"."TEAM_ID" = "TEAM"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "TEAMS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "OIDCUSER"."ID" = :oidcUserId
                 UNION ALL
                SELECT "PERMISSION"."NAME"
                  FROM "OIDCUSER"
                 INNER JOIN "OIDCUSERS_PERMISSIONS"
                    ON "OIDCUSERS_PERMISSIONS"."OIDCUSER_ID" = "OIDCUSER"."ID"
                 INNER JOIN "PERMISSION"
                    ON "PERMISSION"."ID" = "OIDCUSERS_PERMISSIONS"."PERMISSION_ID"
                 WHERE "OIDCUSER"."ID" = :oidcUserId
                """);
        query.setNamedParameters(Map.of("oidcUserId", oidcUser.getId()));
        return Set.copyOf(executeAndCloseResultList(query, String.class));
    }

    /**
     * Determines if the specified UserPrincipal has been assigned the specified permission.
     * @param user the UserPrincipal to query
     * @param permissionName the name of the permission
     * @return true if the user has the permission assigned, false if not
     * @since 1.0.0
     */
    public boolean hasPermission(final UserPrincipal user, String permissionName) {
        return hasPermission(user, permissionName, false);
    }

    /**
     * Determines if the specified UserPrincipal has been assigned the specified permission.
     * @param user the UserPrincipal to query
     * @param permissionName the name of the permission
     * @param includeTeams if true, will query all Team membership assigned to the user for the specified permission
     * @return true if the user has the permission assigned, false if not
     * @since 1.0.0
     */
    public boolean hasPermission(final UserPrincipal user, String permissionName, boolean includeTeams) {
        Query<?> query;
        if (user instanceof final ManagedUser managedUser) {
            query = pm.newQuery(Permission.class, "name == :permissionName && managedUsers.contains(user) && user.id == :userId");
            query.declareVariables("alpine.model.ManagedUser user");
            query.setParameters(permissionName, managedUser.getId());
        } else if (user instanceof final LdapUser ldapUser) {
            query = pm.newQuery(Permission.class, "name == :permissionName && ldapUsers.contains(user) && user.id == :userId");
            query.declareVariables("alpine.model.LdapUser user");
            query.setParameters(permissionName, ldapUser.getId());
        } else if (user instanceof final OidcUser oidcUser) {
            query = pm.newQuery(Permission.class, "name == :permissionName && oidcUsers.contains(user) && user.id == :userId");
            query.declareVariables("alpine.model.OidcUser user");
            query.setParameters(permissionName, oidcUser.getId());
        } else {
            LOGGER.warn("Unrecognized principal class %s; Unable to verify permissions".formatted(user.getClass()));
            return false;
        }
        query.setResult("count(id)");
        final long count = query.executeResultUnique(Long.class);
        if (count > 0) {
            return true;
        }
        if (includeTeams) {
            for (final Team team: user.getTeams()) {
                if (hasPermission(team, permissionName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the specified Team has been assigned the specified permission.
     * @param team the Team to query
     * @param permissionName the name of the permission
     * @return true if the team has the permission assigned, false if not
     * @since 1.0.0
     */
    public boolean hasPermission(final Team team, String permissionName) {
        final Query<Permission> query = pm.newQuery(Permission.class, "name == :permissionName && teams.contains(team) && team.id == :teamId");
        query.declareVariables("alpine.model.Team team");
        query.setParameters(permissionName, team.getId());
        query.setResult("count(id)");
        return executeAndCloseResultUnique(query, Long.class) > 0;
    }

    /**
     * Determines if the specified ApiKey has been assigned the specified permission.
     * @param apiKey the ApiKey to query
     * @param permissionName the name of the permission
     * @return true if the apiKey has the permission assigned, false if not
     * @since 1.1.1
     */
    public boolean hasPermission(final ApiKey apiKey, String permissionName) {
        if (apiKey.getTeams() == null) {
            return false;
        }
        for (final Team team: apiKey.getTeams()) {
            final List<Permission> teamPermissions = getObjectById(Team.class, team.getId()).getPermissions();
            for (final Permission permission: teamPermissions) {
                if (permission.getName().equals(permissionName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves a MappedLdapGroup object for the specified Team and LDAP group.
     * @param team a Team object
     * @param dn a String representation of Distinguished Name
     * @return a MappedLdapGroup if found, or null if no mapping exists
     * @since 1.4.0
     */
    public MappedLdapGroup getMappedLdapGroup(final Team team, final String dn) {
        final Query<MappedLdapGroup> query = pm.newQuery(MappedLdapGroup.class, "team == :team && dn == :dn");
        query.setParameters(team, dn);
        return executeAndCloseUnique(query);
    }

    /**
     * Retrieves a List of MappedLdapGroup objects for the specified Team.
     * @param team a Team object
     * @return a List of MappedLdapGroup objects
     * @since 1.4.0
     */
    public List<MappedLdapGroup> getMappedLdapGroups(final Team team) {
        final Query<MappedLdapGroup> query = pm.newQuery(MappedLdapGroup.class, "team == :team");
        query.setParameters(team);
        return executeAndCloseList(query);
    }

    /**
     * Retrieves a List of MappedLdapGroup objects for the specified DN.
     * @param dn a String representation of Distinguished Name
     * @return a List of MappedLdapGroup objects
     * @since 1.4.0
     */
    public List<MappedLdapGroup> getMappedLdapGroups(final String dn) {
        final Query<MappedLdapGroup> query = pm.newQuery(MappedLdapGroup.class, "dn == :dn");
        query.setParameters(dn);
        return executeAndCloseList(query);
    }

    /**
     * Determines if the specified Team is mapped to the specified LDAP group.
     * @param team a Team object
     * @param dn a String representation of Distinguished Name
     * @return true if a mapping exists, false if not
     * @since 1.4.0
     */
    public boolean isMapped(final Team team, final String dn) {
        return getMappedLdapGroup(team, dn) != null;
    }

    /**
     * Creates a MappedLdapGroup object.
     * @param team The team to map
     * @param dn the distinguished name of the LDAP group to map
     * @return a MappedLdapGroup
     * @since 1.4.0
     */
    public MappedLdapGroup createMappedLdapGroup(final Team team, final String dn) {
        return callInTransaction(() -> {
            final var mapping = new MappedLdapGroup();
            mapping.setTeam(team);
            mapping.setDn(dn);
            return pm.makePersistent(mapping);
        });
    }

    /**
     * Creates a MappedOidcGroup object.
     * @param team The team to map
     * @param group The OIDC group to map
     * @return a MappedOidcGroup
     * @since 1.8.0
     */
    public MappedOidcGroup createMappedOidcGroup(final Team team, final OidcGroup group) {
        return callInTransaction(() -> {
            final var mapping = new MappedOidcGroup();
            mapping.setTeam(team);
            mapping.setGroup(group);
            return pm.makePersistent(mapping);
        });
    }

    /**
     * Retrieves a MappedOidcGroup object for the specified Team and OIDC group.
     * @param team a Team object
     * @param group a OidcGroup object
     * @return a MappedOidcGroup if found, or null if no mapping exists
     * @since 1.8.0
     */
    public MappedOidcGroup getMappedOidcGroup(final Team team, final OidcGroup group) {
        final Query<MappedOidcGroup> query = pm.newQuery(MappedOidcGroup.class, "team == :team && group == :group");
        query.setParameters(team, group);
        return executeAndCloseUnique(query);
    }

    /**
     * Retrieves a List of MappedOidcGroup objects for the specified Team.
     * @param team The team to retrieve mappings for
     * @return a List of MappedOidcGroup objects
     * @since 1.8.0
     */
    public List<MappedOidcGroup> getMappedOidcGroups(final Team team) {
        final Query<MappedOidcGroup> query = pm.newQuery(MappedOidcGroup.class, "team == :team");
        query.setParameters(team);
        return executeAndCloseList(query);
    }

    /**
     * Retrieves a List of MappedOidcGroup objects for the specified group.
     * @param group The group to retrieve mappings for
     * @return a List of MappedOidcGroup objects
     * @since 1.8.0
     */
    public List<MappedOidcGroup> getMappedOidcGroups(final OidcGroup group) {
        final Query<MappedOidcGroup> query = pm.newQuery(MappedOidcGroup.class, "group == :group");
        query.setParameters(group);
        return executeAndCloseList(query);
    }

    /**
     * Determines if the specified Team is mapped to the specified OpenID Connect group.
     * @param team a Team object
     * @param group a OidcGroup object
     * @return true if a mapping exists, false if not
     * @since 1.8.0
     */
    public boolean isOidcGroupMapped(final Team team, final OidcGroup group) {
        return getMappedOidcGroup(team, group) != null;
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
            callInTransaction(() -> {
                final var log = new EventServiceLog();
                log.setSubscriberClass(clazz.getCanonicalName());
                log.setStarted(new Timestamp(new Date().getTime()));
                return pm.makePersistent(log);
            });
        }
        return null;
    }

    /**
     * Updates a EventServiceLog.
     * @param eventServiceLog the EventServiceLog to update
     * @return an updated EventServiceLog
     */
    public EventServiceLog updateEventServiceLog(EventServiceLog eventServiceLog) {
        if (eventServiceLog == null) {
            return null;
        }

        return callInTransaction(() -> {
            final EventServiceLog log = getObjectById(EventServiceLog.class, eventServiceLog.getId());
            if (log != null) {
                log.setCompleted(new Timestamp(new Date().getTime()));
                return log;
            } else {
                return null;
            }
        });
    }

    /**
     * Returns the most recent log entry for the specified Subscriber.
     * If no log entries are found, this method will return null.
     * @param clazz The LoggableSubscriber class to query on
     * @return a EventServiceLog
     * @since 1.0.0
     */
    public EventServiceLog getLatestEventServiceLog(final Class<LoggableSubscriber> clazz) {
        final Query<EventServiceLog> query = pm.newQuery(EventServiceLog.class, "eventClass == :clazz");
        query.setParameters(clazz);
        query.setOrdering("completed desc");
        query.setRange(0, 1);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a ConfigProperty with the specified groupName and propertyName.
     * @param groupName the group name of the config property
     * @param propertyName the name of the property
     * @return a ConfigProperty object
     * @since 1.3.0
     */
    public ConfigProperty getConfigProperty(final String groupName, final String propertyName) {
        final Query<ConfigProperty> query = pm.newQuery(ConfigProperty.class, "groupName == :groupName && propertyName == :propertyName");
        query.setParameters(groupName, propertyName);
        return executeAndCloseUnique(query);
    }

    /**
     * Returns a list of ConfigProperty objects with the specified groupName.
     * @param groupName the group name of the properties
     * @return a List of ConfigProperty objects
     * @since 1.3.0
     */
    public List<ConfigProperty> getConfigProperties(final String groupName) {
        final Query<ConfigProperty> query = pm.newQuery(ConfigProperty.class, "groupName == :groupName");
        query.setParameters(groupName);
        query.setOrdering("propertyName asc");
        return executeAndCloseList(query);
    }

    /**
     * Returns a list of ConfigProperty objects.
     * @return a List of ConfigProperty objects
     * @since 1.3.0
     */
    public List<ConfigProperty> getConfigProperties() {
        final Query<ConfigProperty> query = pm.newQuery(ConfigProperty.class);
        query.setOrdering("groupName asc, propertyName asc");
        return executeAndCloseList(query);
    }

    /**
     * Creates a ConfigProperty object.
     * @param groupName the group name of the property
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     * @param propertyType the type of property
     * @param description a description of the property
     * @return a ConfigProperty object
     * @since 1.3.0
     */
    public ConfigProperty createConfigProperty(final String groupName, final String propertyName,
                                               final String propertyValue, final ConfigProperty.PropertyType propertyType,
                                               final String description) {
        return callInTransaction(() -> {
            final ConfigProperty configProperty = new ConfigProperty();
            configProperty.setGroupName(groupName);
            configProperty.setPropertyName(propertyName);
            configProperty.setPropertyValue(propertyValue);
            configProperty.setPropertyType(propertyType);
            configProperty.setDescription(description);
            return pm.makePersistent(configProperty);
        });
    }

}
