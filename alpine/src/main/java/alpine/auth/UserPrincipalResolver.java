/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.auth;

import alpine.model.LdapUser;
import alpine.model.ManagedUser;
import alpine.model.UserPrincipal;
import alpine.persistence.AlpineQueryManager;


/**
 * Resolves a UserPrincipal based on the pre-configured resolution order.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class UserPrincipalResolver {

    private String username;
    private UserPrincipal principal;

    public UserPrincipalResolver(String username) {
        this.username = username;
    }

    /**
     * Resolves a UserPrincipal. Default order resolution is to first match
     * on ManagedUser then on LdapUser. This may be configurable in a future
     * release.
     * @return a UserPrincipal if found, null if not found
     * @since 1.0.0
     */
    public UserPrincipal resolve() {
        try (AlpineQueryManager qm = new AlpineQueryManager()) {
            UserPrincipal principal = qm.getManagedUser(username);
            if (principal != null) {
                this.principal = principal;
                return principal;
            }
            return this.principal = qm.getLdapUser(username);
        }
    }

    /**
     * Returns whether or not the resolved UserPrincipal is an LdapUser or not.
     * Requires that {@link #resolve()} is called first.
     * @return true if LdapUser, false if not
     * @since 1.0.0
     */
    public boolean isLdapUser() {
        return principal != null && principal instanceof LdapUser;
    }

    /**
     * Returns whether or not the resolved UserPrincipal is a ManagedUser or not.
     * Requires that {@link #resolve()} is called first.
     * @return true if ManagedUser, false if not
     * @since 1.0.0
     */
    public boolean isManagedUser() {
        return principal != null && principal instanceof ManagedUser;
    }

}
