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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Persistable object representing an OpenID Connect user.
 *
 * @since 1.8.0
 */
@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OidcUser implements Serializable, Principal, UserPrincipal {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name = "OIDCUSER_USERNAME_IDX")
    @Column(name = "USERNAME", allowsNull = "false")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The username must not contain control characters")
    private String username;

    @Persistent
    @Column(name = "SUBJECT_IDENTIFIER")
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The subject identifier must not contain control characters")
    private String subjectIdentifier;

    @Persistent
    @Column(name = "EMAIL", allowsNull = "true")
    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The email address must not contain control characters")
    private String email;

    @Persistent(table = "OIDCUSERS_TEAMS", defaultFetchGroup = "true")
    @Join(column = "OIDCUSERS_ID")
    @Element(column = "TEAM_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    private List<Team> teams;

    @Persistent(table = "OIDCUSERS_PERMISSIONS", defaultFetchGroup = "true")
    @Join(column = "OIDCUSER_ID")
    @Element(column = "PERMISSION_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    private List<Permission> permissions;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    public String getSubjectIdentifier() {
        return subjectIdentifier;
    }

    public void setSubjectIdentifier(final String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public List<Team> getTeams() {
        return teams;
    }

    @Override
    public void setTeams(final List<Team> teams) {
        this.teams = teams;
    }

    @Override
    public List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(final List<Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Do not use - only here to satisfy Principal implementation requirement.
     *
     * @return the value of {@link #getUsername()}
     * @deprecated use {@link OidcUser#getUsername()}
     */
    @Deprecated
    @JsonIgnore
    @Override
    public String getName() {
        return getUsername();
    }

}
