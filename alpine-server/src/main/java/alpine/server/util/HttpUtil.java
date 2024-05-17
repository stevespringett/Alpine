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
package alpine.server.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This class contains a collection of useful utility methods for various HTTP tasks.
 * @author Steve Springett
 * @since 1.0.0
 */
public final class HttpUtil {

    private HttpUtil() { }

    /**
     * Returns a session attribute as the type of object stored.
     *
     * @param session session where the attribute is stored
     * @param key the attributes key
     * @param <T> the type of object expected
     * @return the requested object
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSessionAttribute(final HttpSession session, final String key) {
        if (session != null) {
            return (T) session.getAttribute(key);
        }
        return null;
    }

    /**
     * Returns a request attribute as the type of object stored.
     *
     * @param request request of the attribute
     * @param key the attributes key
     * @param <T> the type of the object expected
     * @return the requested object
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestAttribute(final HttpServletRequest request, final String key) {
        if (request != null) {
            return (T) request.getAttribute(key);
        }
        return null;
    }

}