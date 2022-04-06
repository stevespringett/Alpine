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
package alpine.common.util;

/**
 * A collection of useful Boolean utilities.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public final class BooleanUtil {

    /**
     * Private constructor
     */
    private BooleanUtil() {
    }

    /**
     * Determines if the specified string contains 'true' or '1'
     * @param value a String representation of a boolean to convert
     * @return a boolean
     * @since 1.0.0
     */
    public static boolean valueOf(String value) {
        return (value != null) && (value.trim().equalsIgnoreCase("true") || value.trim().equals("1"));
    }

    /**
     * Determines if the specified object is null or not.
     * @param o the object to evaluate
     * @return true if null, false if not null
     * @since 1.0.0
     */
    public static boolean isNull(Object o) {
        return o == null;
    }

    /**
     * Determines if the specified object is null or not.
     * @param o the object to evaluate
     * @return true if not null, false if null
     * @since 1.0.0
     */
    public static boolean isNotNull(Object o) {
        return o != null;
    }

}
