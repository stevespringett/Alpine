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
package alpine.event;

import alpine.event.framework.Event;

/**
 * An Event implementation specific for LDAP synchronization tasks.
 *
 * see alpine.server.tasks.LdapSyncTask
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class LdapSyncEvent implements Event {

    private String username = null;

    /**
     * Default constructor. This will indicate the all LdapUser objects need to be synchronized.
     */
    public LdapSyncEvent() {
    }

    /**
     * Use of this constructor will indicate that only the specified LdapUser object will need to be synchronized.
     * @param username the username of the LdapUser to syncronize
     */
    public LdapSyncEvent(String username) {
        this.username = username;
    }

    /**
     * Returns the username of the LdapUser to synchronize.
     * This method will return null if the default constructor was used.
     * @return a String of the username
     */
    public String getUsername() {
        return username;
    }

}
