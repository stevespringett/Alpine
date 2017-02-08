/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.util;

import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;

/**
 * A collection of useful UUID utilities.
 *
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class UuidUtil {

    private static final Pattern uuidPattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private UuidUtil() { }

    /**
     * Inserts hyphens in a valid 32 character UUID containing no hyphens.
     *
     * @since 1.0.0
     */
    public static String insertHyphens(String uuidWithoutHyphens) {
        return uuidWithoutHyphens.replaceFirst( "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5" );
    }

    /**
     * Removes hyphens from a 36 character UUID.
     *
     * @since 1.0.0
     */
    public static String stripHyphens(String uuid) {
        return uuid.replaceAll("-", "");
    }

    /**
     * Determines if the specified string is a valid UUID.
     *
     * @since 1.0.0
     */
    public static boolean isValidUUID(String uuid) {
        return !StringUtils.isEmpty(uuid) && uuidPattern.matcher(uuid).matches();
    }

}