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

import alpine.Config;
import alpine.common.validation.RegexSequence;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
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
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * Persistable object representing an ApiKey.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@PersistenceCapable
public class ApiKey implements Serializable, Principal {

    private static final long serialVersionUID = 1582714693932260365L;
    private static final String prefix = Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);
    public static final int PUBLIC_ID_LENGTH = 5;
    public static final int API_KEY_LENGTH = 32;
    public static int FULL_KEY_LENGTH = prefix.length() + PUBLIC_ID_LENGTH + API_KEY_LENGTH;
    public static int LEGACY_FULL_KEY_LENGTH = prefix.length() + API_KEY_LENGTH;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name = "APIKEY_IDX")
    @Column(name = "APIKEY", allowsNull = "false")
    @NotNull
    @Size(min = 32, max = 255)
    @Pattern(regexp = RegexSequence.Definition.WORD_CHARS,
            message = "The API key must contain only alpha, numeric and/or underscore characters")
    @JsonIgnore
    private String key;

    @Persistent
    @Column(name = "COMMENT")
    @Size(max = 255)
    private String comment;

    @Persistent
    @Column(name = "CREATED")
    private Date created;

    @Persistent
    @Column(name = "LAST_USED")
    private Date lastUsed;

    @Persistent(table = "APIKEYS_TEAMS", defaultFetchGroup = "true")
    @Join(column = "APIKEY_ID")
    @Element(column = "TEAM_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    @JsonIgnore
    private List<Team> teams;

    @Persistent
    @Unique(name = "APIKEY_PUBLIC_IDX")
    @Size(min = PUBLIC_ID_LENGTH, max = PUBLIC_ID_LENGTH)
    @Column(name = "PUBLIC_ID", allowsNull = "false")
    private String publicId;

    @Persistent
    @Column(name = "IS_LEGACY", allowsNull = "false", defaultValue = "false")
    private boolean isLegacy = false;

    /**
     * Is set to the clearTextKey, if it's (re)generated,
     * so the user can use this to authenticate.
     * On all other requests, this will be null
     */
    @Schema(nullable = true)
    private transient String clearTextKey;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Masks all key characters except the prefix and the public ID with *.
     *
     * @return Masked key.
     */
    public String getMaskedKey() {
        final String maskedKey = prefix + publicId + "*".repeat(API_KEY_LENGTH);
        return maskedKey;
    }

    /**
     * Gets part of key, which should be hashed.
     *
     * @param key The key to get from
     * @return only hashable key
     */
    public static String getOnlyKey(String key) {
        var startKey = prefix.length() + PUBLIC_ID_LENGTH;
        return key.substring(startKey);
    }

    /**
     * Gets part of key, which should be hashed, as a byte Array.
     *
     * @param key The key to get from
     * @return only hashable key
     */
    public static byte[] getOnlyKeyAsBytes(String key) {
        return getOnlyKey(key).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Gets Public Id from a full key.
     *
     * @param key The key to get from
     * @return Public ID
     */
    public static String getPublicId(String key) {
        var startPublicId = prefix.length();
        var endPublicId = startPublicId + PUBLIC_ID_LENGTH;
        return key.substring(startPublicId, endPublicId);
    }

    /**
     * Do not use - only here to satisfy Principal implementation requirement.
     *
     * @return a String presentation of the username
     * @deprecated use {@link UserPrincipal#getUsername()}
     */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return getMaskedKey();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(final Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicID) {
        this.publicId = publicID;
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    public void setLegacy(boolean isLegacy) {
        this.isLegacy = isLegacy;
    }

    public String getClearTextKey() {
        return clearTextKey;
    }

    public void setClearTextKey(String clearTextKey) {
        this.clearTextKey = clearTextKey;
    }
}
