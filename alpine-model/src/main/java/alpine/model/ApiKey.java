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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.security.Principal;
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
    private String key;

    @Persistent(table = "APIKEYS_TEAMS", defaultFetchGroup = "true")
    @Join(column = "APIKEY_ID")
    @Element(column = "TEAM_ID")
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "name ASC"))
    @JsonIgnore
    private List<Team> teams;

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
     * Masks all key characters except the prefix and last four characters with *. If the key does not have the
     * currently configured prefix, do not return it.
     * @return Masked key.
     */
    public String getMaskedKey() {
        final StringBuilder maskedKey = new StringBuilder();

        // if key does not have the current prefix, do not return a prefix
        if (key.startsWith(prefix))
            maskedKey.append(prefix);

        // mask all characters except the last four
        maskedKey.append("*".repeat(key.length() - maskedKey.length() - 4));
        maskedKey.append(key.substring(key.length() - 4));

        return maskedKey.toString();
    }

    /**
     * Do not use - only here to satisfy Principal implementation requirement.
     * @deprecated use {@link UserPrincipal#getUsername()}
     * @return a String presentation of the username
     */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return getMaskedKey();
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

}

