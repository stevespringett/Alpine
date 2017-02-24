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
package alpine.util;

/**
 * A collection of useful Boolean utilities
 *
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class BooleanUtil {

    private BooleanUtil() {
    }

    /**
     * Determines if the specified string contains 'true' or '1'.
     *
     * @since 1.0.0
     */
    public static boolean valueOf(String value) {
        return (value != null) && (value.trim().equalsIgnoreCase("true") || value.trim().equals("1"));
    }

    /**
     * @since 1.0.0
     */
    public static boolean isTrue(boolean value) {
        return value;
    }

    /**
     * @since 1.0.0
     */
    public static boolean isFalse(boolean value) {
        return !value;
    }

    /**
     * @since 1.0.0
     */
    public static boolean isNull(Object o) {
        return o == null;
    }

    /**
     * @since 1.0.0
     */
    public static boolean isNotNull(Object o) {
        return o != null;
    }

}