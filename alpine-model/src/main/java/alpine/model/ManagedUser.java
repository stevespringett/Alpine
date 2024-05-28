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
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * Persistable object representing an ManagedUser.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedUser implements Serializable, Principal, UserPrincipal {

    private static final long serialVersionUID = 7944779964068911025L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name = "MANAGEDUSER_USERNAME_IDX")
    @Column(name = "USERNAME")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The username must not contain control characters")
    private String username;

    @Persistent
    @Column(name = "PASSWORD", allowsNull = "false")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The password must not contain control characters")
    @JsonIgnore
    private String password;

    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The new password must not contain control characters")
    private transient String newPassword; // not persisted

    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The confirm password must not contain control characters")
    private transient String confirmPassword; // not persisted

    @Persistent
    @Column(name = "LAST_PASSWORD_CHANGE", allowsNull = "false")
    @NotNull
    private Date lastPasswordChange;

    @Persistent
    @Column(name = "FULLNAME")
    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The full name must not contain control characters")
    private String fullname;

    @Persistent
    @Column(name = "EMAIL")
    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The email address must not contain control characters")
    private String email;

    @Persistent
    @Column(name = "SUSPENDED")
    private boolean suspended;

    @Persistent
    @Column(name = "FORCE_PASSWORD_CHANGE")
    private boolean forcePasswordChange;

    @Persistent
    @Column(name = "NON_EXPIRY_PASSWORD")
    private boolean nonExpiryPassword;

    @Persistent(table = "MANAGEDUSERS_TEAMS", defaultFetchGroup = "true")
    @Join(column = "MANAGEDUSER_ID")
    @Element(column = "TEAM_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    private List<Team> teams;

    @Persistent(table = "MANAGEDUSERS_PERMISSIONS", defaultFetchGroup = "true")
    @Join(column = "MANAGEDUSER_ID")
    @Element(column = "PERMISSION_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    private List<Permission> permissions;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Date getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(Date lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public boolean isNonExpiryPassword() {
        return nonExpiryPassword;
    }

    public void setNonExpiryPassword(boolean nonExpiryPassword) {
        this.nonExpiryPassword = nonExpiryPassword;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Do not use - only here to satisfy Principal implementation requirement.
     * @deprecated use {@link ManagedUser#getUsername()}
     * @return the value of {@link #getUsername()}
     */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return getUsername();
    }

}
