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
package alpine.auth;

import javax.ws.rs.core.NewCookie;

/**
 * Alpine Cookies are secure with the HttpOnly, Secure, and Samesite flags enabled.
 * @since 1.5.0
 */
public class AlpineCookie extends NewCookie {

    /**
     * Create a new instance.
     *
     * @param name    the name of the cookie.
     * @param value   the value of the cookie.
     * @param path    the URI path for which the cookie is valid.
     * @param domain  the host domain for which the cookie is valid.
     * @param version the version
     * @param maxAge  the maximum age of the cookie in seconds.
     * @throws IllegalArgumentException if name is {@code null}.
     * @since 1.5.0
     */
    public AlpineCookie(String name, String value, String path, String domain, int version, int maxAge) {
        super(name, value, path, domain, version, null, maxAge, null, true, true);
    }

    /**
     * Create a new instance.
     *
     * @param name    the name of the cookie.
     * @param value   the value of the cookie.
     * @param path    the URI path for which the cookie is valid.
     * @param domain  the host domain for which the cookie is valid.
     * @param maxAge  the maximum age of the cookie in seconds.
     * @throws IllegalArgumentException if name is {@code null}.
     * @since 1.5.0
     */
    public AlpineCookie(String name, String value, String path, String domain, int maxAge) {
        super(name, value, path, domain, 1, null, maxAge, null, true, true);
    }

    /**
     * Create a new instance.
     *
     * @param name    the name of the cookie.
     * @param value   the value of the cookie.
     * @param path    the URI path for which the cookie is valid.
     * @param domain  the host domain for which the cookie is valid.
     * @throws IllegalArgumentException if name is {@code null}.
     * @since 1.5.0
     */
    public AlpineCookie(String name, String value, String path, String domain) {
        super(name, value, path, domain, 1, null, -1, null, true, true);
    }

    /**
     * Create a new instance.
     *
     * @param name    the name of the cookie.
     * @param value   the value of the cookie.
     * @throws IllegalArgumentException if name is {@code null}.
     * @since 1.5.0
     */
    public AlpineCookie(String name, String value) {
        super(name, value, null, null, 1, null, -1, null, true, true);
    }

    /**
     * Convert the cookie to a string suitable for use as the value of the
     * corresponding HTTP header.
     *
     * @return a stringified cookie.
     * @since 1.5.0
     */
    @Override
    public String toString() {
        return super.toString() + ";SameSite=Strict";
    }
}
