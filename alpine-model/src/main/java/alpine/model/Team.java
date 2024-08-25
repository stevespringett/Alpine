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
package alpine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Persistable object representing a Team.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@PersistenceCapable
@FetchGroups({
        @FetchGroup(name = "ALL", members = {
                @Persistent(name = "uuid"),
                @Persistent(name = "name"),
                @Persistent(name = "apiKeys"),
                @Persistent(name = "ldapUsers"),
                @Persistent(name = "managedUsers"),
                @Persistent(name = "oidcUsers"),
                @Persistent(name = "mappedLdapGroups"),
                @Persistent(name = "mappedOidcGroups"),
                @Persistent(name = "permissions")
        })
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Team implements Serializable {

    private static final long serialVersionUID = 6938424919898277944L;

    /**
     * Provides an enum that defines the JDO fetchgroups this class defines.
     */
    public enum FetchGroup {
        ALL
    }

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent(customValueStrategy = "uuid")
    @Unique(name = "TEAM_UUID_IDX")
    @Column(name = "UUID", jdbcType = "VARCHAR", length = 36, allowsNull = "false")
    @NotNull
    private UUID uuid;

    @Persistent
    @Column(name = "NAME", jdbcType = "VARCHAR", length = 255, allowsNull = "false")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The team name must not contain control characters")
    private String name;

    @Persistent(mappedBy = "teams")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
    private List<ApiKey> apiKeys;

    @Persistent(mappedBy = "teams")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    private List<LdapUser> ldapUsers;

    @Persistent(mappedBy = "teams")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    private List<ManagedUser> managedUsers;

    @Persistent(mappedBy = "teams")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    private List<OidcUser> oidcUsers;

    @Persistent(mappedBy = "team")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "dn ASC"))
    private List<MappedLdapGroup> mappedLdapGroups;

    @Persistent(mappedBy = "team")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
    private List<MappedOidcGroup> mappedOidcGroups;

    @Persistent(table = "TEAMS_PERMISSIONS", defaultFetchGroup = "true")
    @Join(column = "TEAM_ID")
    @Element(column = "PERMISSION_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    private List<Permission> permissions;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
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

    public List<OidcUser> getOidcUsers() {
        return oidcUsers;
    }

    public void setOidcUsers(List<OidcUser> oidcUsers) {
        this.oidcUsers = oidcUsers;
    }

    public List<MappedLdapGroup> getMappedLdapGroups() {
        return mappedLdapGroups;
    }

    public void setMappedLdapGroups(List<MappedLdapGroup> mappedLdapGroups) {
        this.mappedLdapGroups = mappedLdapGroups;
    }

    public List<MappedOidcGroup> getMappedOidcGroups() {
        return mappedOidcGroups;
    }

    public void setMappedOidcGroups(List<MappedOidcGroup> mappedOidcGroups) {
        this.mappedOidcGroups = mappedOidcGroups;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
