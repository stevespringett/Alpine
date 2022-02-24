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

package alpine.server.auth;

/**
 * @since 1.8.0
 */
public enum IdentityProvider {

    LOCAL,

    LDAP,

    OPENID_CONNECT;

    /**
     * Returns an IdentityProvider that matches a given name.
     *
     * @param name Name of the IdentityProvider to get
     * @return The matching IdentityProvider, or null when no matching IdentityProvider was found
     */
    public static IdentityProvider forName(final String name) {
        for (final IdentityProvider identityProvider : values()) {
            if (identityProvider.name().equals(name)) {
                return identityProvider;
            }
        }
        return null;
    }

}
