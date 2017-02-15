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
package alpine.persistence;

import alpine.Config;
import alpine.auth.ApiKeyGenerator;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.EventService;
import alpine.model.ApiKey;
import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.Team;
import alpine.model.UserPrincipal;
import alpine.util.UuidUtil;
import javax.jdo.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QueryManager extends AlpineQueryManager {

    private static final boolean ENFORCE_AUTHORIZATION = Config.getInstance().getPropertyAsBoolean(Config.Key.ENFORCE_AUTHORIZATION);

    @SuppressWarnings("unchecked")
    public ApiKey getApiKey(String key) {
        Query query = pm.newQuery(ApiKey.class, "key == :key");
        List<ApiKey> result = (List<ApiKey>)query.execute (key);
        return result.size() == 0 ? null : result.get(0);
    }

    public ApiKey regenerateApiKey(ApiKey apiKey) {
        pm.currentTransaction().begin();
        apiKey.setKey(UuidUtil.stripHyphens(UUID.randomUUID().toString()));
        pm.currentTransaction().commit();
        apiKey = pm.getObjectById(ApiKey.class, apiKey.getId());
        return apiKey;
    }

    public ApiKey createApiKey(Team team) {
        Set<Team> teams = new HashSet<>();
        teams.add(team);
        pm.currentTransaction().begin();
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(ApiKeyGenerator.generate());
        apiKey.setTeams(teams);
        pm.makePersistent(apiKey);
        pm.currentTransaction().commit();
        return pm.getObjectById(ApiKey.class, apiKey.getId());
    }

    @SuppressWarnings("unchecked")
    public LdapUser getLdapUser(String username) {
        Query query = pm.newQuery(LdapUser.class, "username == :username");
        List<LdapUser> result = (List<LdapUser>)query.execute(username);
        return result.size() == 0 ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<LdapUser> getLdapUsers() {
        Query query = pm.newQuery(LdapUser.class);
        query.setOrdering("username asc");
        return (List<LdapUser>)query.execute();
    }

    public LdapUser createLdapUser(String username) {
        pm.currentTransaction().begin();
        LdapUser user = new LdapUser();
        user.setUsername(username);
        user.setDN("Syncing...");
        //todo - Implement lookup/sync service that automatically obtains and updates DN, or in the case of incorrect or deleted entries, mark DN as 'INVALID'
        pm.makePersistent(user);
        pm.currentTransaction().commit();
        EventService.getInstance().publish(new LdapSyncEvent(user.getUsername()));
        return getObjectById(LdapUser.class, user.getId());
    }

    public LdapUser updateLdapUser(LdapUser transientUser) {
        LdapUser user = getObjectById(LdapUser.class, transientUser.getId());
        pm.currentTransaction().begin();
        user.setDN(transientUser.getDN());
        pm.currentTransaction().commit();
        return pm.getObjectById(LdapUser.class, user.getId());
    }

    public ManagedUser createManagedUser(String username, String password) {
        pm.currentTransaction().begin();
        ManagedUser user = new ManagedUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setSuspended(false);
        pm.makePersistent(user);
        pm.currentTransaction().commit();
        return getObjectById(ManagedUser.class, user.getId());
    }

    @SuppressWarnings("unchecked")
    public ManagedUser getManagedUser(String username) {
        Query query = pm.newQuery(ManagedUser.class, "username == :username");
        List<ManagedUser> result = (List<ManagedUser>)query.execute(username);
        return result.size() == 0 ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<ManagedUser> getManagedUsers() {
        Query query = pm.newQuery(ManagedUser.class);
        query.setOrdering("username asc");
        return (List<ManagedUser>)query.execute();
    }


    public Team createTeam(String name, boolean createApiKey) {
        pm.currentTransaction().begin();
        Team team = new Team();
        team.setName(name);
        //todo assign permissions
        team.setUuid(UUID.randomUUID().toString());
        pm.makePersistent(team);
        pm.currentTransaction().commit();
        if (createApiKey) {
            createApiKey(team);
        }
        return getObjectByUuid(Team.class, team.getUuid(), Team.FetchGroup.ALL.getName());
    }

    @SuppressWarnings("unchecked")
    public List<Team> getTeams() {
        pm.getFetchPlan().addGroup(Team.FetchGroup.ALL.getName());
        Query query = pm.newQuery(Team.class);
        query.setOrdering("name asc");
        return (List<Team>)query.execute();
    }

    public Team updateTeam(Team transientTeam) {
        Team team = getObjectByUuid(Team.class, transientTeam.getUuid());
        pm.currentTransaction().begin();
        team.setName(transientTeam.getName());
        //todo assign permissions
        pm.currentTransaction().commit();
        return pm.getObjectById(Team.class, team.getId());
    }

    public boolean addUserToTeam(UserPrincipal user, Team team) {
        List<Team> teams = user.getTeams();
        boolean found = false;
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

    public boolean removeUserFromTeam(UserPrincipal user, Team team) {
        List<Team> teams = user.getTeams();
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

}

