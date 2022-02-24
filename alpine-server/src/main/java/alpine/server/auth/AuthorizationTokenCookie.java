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
 * An specific type of cookie designed to contain a JWT token.
 *
 * @since 1.5.0
 */
public class AuthorizationTokenCookie extends AlpineCookie {

    public static final String COOKIE_NAME = "Authorization-Token";

    public AuthorizationTokenCookie(String token) {
        super(COOKIE_NAME, token);
    }

    public AuthorizationTokenCookie(String token, String path) {
        super(COOKIE_NAME, token, path, null);
    }

    public AuthorizationTokenCookie(String token, String path, String domain) {
        super(COOKIE_NAME, token, path, domain);
    }
}
