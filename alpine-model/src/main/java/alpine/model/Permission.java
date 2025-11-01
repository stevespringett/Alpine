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

import alpine.common.validation.RegexSequence;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.io.Serializable;
import java.util.List;

/**
 * Persistable object representing a Permission.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Permission implements Serializable {

    private static final long serialVersionUID = 1420020753285692448L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name = "PERMISSION_IDX")
    @Column(name = "NAME", allowsNull = "false")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = RegexSequence.Definition.WORD_CHARS, message = "The permission name must contain only alpha and/or numeric characters")
    private String name;

    @Persistent
    @Column(name = "DESCRIPTION", jdbcType = "CLOB")
    private String description;

    @Persistent(mappedBy = "permissions")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    @JsonIgnore
    private List<Team> teams;

    @Persistent(mappedBy = "permissions")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    @JsonIgnore
    private List<OidcUser> oidcUsers;

    @Persistent(mappedBy = "permissions")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    @JsonIgnore
    private List<LdapUser> ldapUsers;

    @Persistent(mappedBy = "permissions")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "username ASC"))
    @JsonIgnore
    private List<ManagedUser> managedUsers;

    @Persistent(mappedBy = "permissions")
    @JsonIgnore
    private List<ApiKey> apiKeys;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<OidcUser> getOidcUsers() {
        return oidcUsers;
    }

    public void setOidcUsers(List<OidcUser> oidcUsers) {
        this.oidcUsers = oidcUsers;
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

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }
}
