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

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * A collection of useful UUID utilities.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public final class UuidUtil {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    /**
     * Private constructor
     */
    private UuidUtil() { }

    /**
     * Inserts hyphens in a valid 32 character UUID containing no hyphens.
     * @param uuidWithoutHyphens a UUID without separating hyphens
     * @return a UUID (as a String) containing hyphens
     * @since 1.0.0
     */
    public static String insertHyphens(String uuidWithoutHyphens) {
        return uuidWithoutHyphens.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
    }

    /**
     * Removes hyphens from a 36 character UUID.
     * @param uuid the UUID to strip hyphens from
     * @return a String of the UUID without hyphens
     * @since 1.0.0
     */
    public static String stripHyphens(String uuid) {
        return uuid.replaceAll("-", "");
    }

    /**
     * Determines if the specified string is a valid UUID.
     * @param uuid the UUID to evaluate
     * @return true if UUID is valid, false if invalid
     * @since 1.0.0
     */
    public static boolean isValidUUID(String uuid) {
        return !StringUtils.isEmpty(uuid) && UUID_PATTERN.matcher(uuid).matches();
    }

}
