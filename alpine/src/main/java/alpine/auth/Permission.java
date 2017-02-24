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

/**
 * Contains Permission constatns. This class is intended to be extended to
 * allow for defining of additional permissions.
 *
 * @since 1.0.0
 */
public abstract class Permission {

    public static final String MANAGE_API_KEYS = "MANAGE_API_KEYS";
    public static final String MANAGE_TEAMS = "MANAGE_TEAMS";
    public static final String MANAGE_USERS = "MANAGE_USERS";

}