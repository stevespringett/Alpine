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
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

@PersistenceCapable
public class ApiKey implements Serializable, Principal {

    private static final long serialVersionUID = 1582714693932260365L;

    @PrimaryKey
    @Persistent(valueStrategy=IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name="APIKEY_IDX")
    @Column(name="APIKEY", allowsNull="false")
    private String key;

    @Persistent(table="APIKEYS_TEAMS", defaultFetchGroup="true")
    @Join(column="APIKEY_ID")
    @Element(column="TEAM_ID")
    @JsonIgnore
    private Set<Team> teams;

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
     * Do not use - only here to satisfy Principal implementation requirement
     * @deprecated use {@link LdapUser#getUsername()}
     */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return getKey();
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }

}

