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
package alpine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@PersistenceCapable
@FetchGroups({
        @FetchGroup(name="all", members={
                @Persistent(name="uuid"),
                @Persistent(name="name"),
                @Persistent(name="apiKeys"),
                @Persistent(name="ldapUsers"),
                @Persistent(name="managedUsers")
        })
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Team implements Serializable {

    private static final long serialVersionUID = 6938424919898277944L;

    public enum FetchGroup {
        ALL("all");

        private String fetchGroupName;
        FetchGroup(String fetchGroupName) {
            this.fetchGroupName = fetchGroupName;
        }

        public String getName() {
            return fetchGroupName;
        }
    }

    @PrimaryKey
    @Persistent(valueStrategy= IdGeneratorStrategy.INCREMENT)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name="TEAM_UUID_IDX")
    @Column(name="UUID", jdbcType="VARCHAR", length=36, allowsNull="false")
    @NotNull
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", message = "The uuid must be a valid 36 character UUID")
    private String uuid;

    @Persistent
    @Column(name="NAME", jdbcType="VARCHAR", length=50, allowsNull="false")
    @NotNull
    @Size(min=1, max=255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The team name must not contain control characters")
    private String name;

    @Persistent(mappedBy="teams")
    @Order(extensions=@Extension(vendorName="datanucleus", key="list-ordering", value="id ASC"))
    private List<ApiKey> apiKeys;

    @Persistent(mappedBy="teams")
    @Order(extensions=@Extension(vendorName="datanucleus", key="list-ordering", value="username ASC"))
    private List<LdapUser> ldapUsers;

    @Persistent(mappedBy="teams")
    @Order(extensions=@Extension(vendorName="datanucleus", key="list-ordering", value="username ASC"))
    private List<ManagedUser> managedUsers;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public List<LdapUser> getLdapUsers() {
        return ldapUsers;
    }

    public void setLdapUsers(List<LdapUser> ldapUsers) {
        this.ldapUsers = ldapUsers;
    }

    public List<ManagedUser> getManagedUsers() {
        return managedUsers;
    }

    public void setManagedUsers(List<ManagedUser> managedUsers) {
        this.managedUsers = managedUsers;
    }
}