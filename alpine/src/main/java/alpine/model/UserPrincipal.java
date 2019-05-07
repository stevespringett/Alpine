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

import java.util.List;

/**
 * Defines Alpine UserPrincipal.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public interface UserPrincipal {

    /**
     * The database id of the principal.
     * @return a long of the unique id
     */
    long getId();

    /**
     * Specifies the database id of the principal.
     * @param id a long of the unique id
     */
    void setId(long id);

    /**
     * The username of the principal.
     * @return a String of the username
     */
    String getUsername();

    /**
     * Specifies the username of the principal.
     * @param username the username of the principal
     */
    void setUsername(String username);

    /**
     * The email address of the principal.
     * @return a String of the email address
     */
    String getEmail();

    /**
     * Specifies the email address of the principal.
     * @param email the email address of the principal
     */
    void setEmail(String email);

    /**
     * A list of teams the principal is a member of.
     * @return a List of Team objects
     */
    List<Team> getTeams();

    /**
     * Specifies the teams the principal is a member of.
     * @param teams a List of Team objects
     */
    void setTeams(List<Team> teams);

    /**
     * A list of permissions the principal has.
     * @return a List of Permissions objects
     */
    List<Permission> getPermissions();

    /**
     * Specifies the permissions the principal should have.
     * @param permissions a List of Permission objects
     */
    void setPermissions(List<Permission> permissions);

    /**
     * Use of this method may be necessary to satisfy {@link java.security.Principal}
     * requirements, but the implementation should not be used and should return
     * the same value as {@link #getUsername}.
     * @return a String of the username
     */
    String getName();
}
