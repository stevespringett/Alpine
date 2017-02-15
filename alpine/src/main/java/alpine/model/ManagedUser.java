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

@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagedUser implements Serializable, Principal, UserPrincipal {

    private static final long serialVersionUID = 7944779964068911025L;

    @PrimaryKey
    @Persistent(valueStrategy= IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Unique(name="MANAGEDUSER_USERNAME_IDX")
    @Column(name="USERNAME")
    private String username;

    @Persistent
    @Column(name="PASSWORD", allowsNull="false")
    private String password;

    @Persistent
    @Column(name="SUSPENDED")
    private boolean suspended;

    @Persistent(table="MANAGEDUSERS_TEAMS", defaultFetchGroup="true")
    @Join(column="MANAGEDUSER_ID")
    @Element(column="TEAM_ID")
    @Order(extensions=@Extension(vendorName="datanucleus", key="list-ordering", value="name ASC"))
    private List<Team> teams;

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

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    /**
     * Do not use - only here to satisfy Principal implementation requirement
     * @deprecated use {@link ManagedUser#getUsername()}
     */
    @Deprecated
    @JsonIgnore
    public String getName() {
        return getUsername();
    }

}
