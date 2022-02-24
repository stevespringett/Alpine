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

import alpine.model.UserPrincipal;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A collection of methods that help integrate user Gravatar's.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class GravatarUtil {

    /**
     * Private constructor
     */
    private GravatarUtil() { }

    /**
     * Generates a hash value from the specified email address.
     * Returns null if emailAddress is empty or null.
     * @param emailAddress the email address to generate a hash from
     * @return a hash value of the specified email address
     * @since 1.0.0
     */
    public static String generateHash(String emailAddress) {
        if (StringUtils.isBlank(emailAddress)) {
            return null;
        }
        return DigestUtils.md5Hex(emailAddress.trim().toLowerCase()).toLowerCase();
    }

    /**
     * Generates a Gravatar URL for the specified principal. If
     * the principal does not have an email address or the email
     * address does not have a Gravatar, will fallback to using
     * the mystery-man image.
     * @param userPrincipal the user principal to generate a Gravatar URL from
     * @return a Gravatar URL for the specified principal
     * @since 1.0.0
     */
    public static String getGravatarUrl(UserPrincipal userPrincipal) {
        return getGravatarUrl(userPrincipal.getEmail());
    }

    /**
     * Generates a Gravatar URL for the specified principal. If
     * the principal does not have an email address or the email
     * address does not have a Gravatar, will fallback to using
     * the mystery-man image.
     * @param userPrincipal the user principal to generate a Gravatar URL from
     * @param size the size of the image
     * @return a Gravatar URL for the specified principal
     * @since 1.0.0
     */
    public static String getGravatarUrl(UserPrincipal userPrincipal, int size) {
        return getGravatarUrl(userPrincipal.getEmail(), size);
    }

    /**
     * Generates a Gravatar URL for the specified email address. If
     * the email address is blank or does not have a Gravatar, will
     * fallback to usingthe mystery-man image.
     * @param emailAddress the email address to generate the Gravatar URL from
     * @return a Gravatar URL for the specified email address
     * @since 1.0.0
     */
    public static String getGravatarUrl(String emailAddress) {
        String hash = generateHash(emailAddress);
        if (hash == null) {
            return "https://www.gravatar.com/avatar/00000000000000000000000000000000" + ".jpg?d=mm";
        } else {
            return "https://www.gravatar.com/avatar/" + hash + ".jpg?d=mm";
        }
    }

    /**
     * Generates a Gravatar URL for the specified email address. If
     * the email address is blank or does not have a Gravatar, will
     * fallback to usingthe mystery-man image.
     * @param emailAddress the email address to generate the Gravatar URL from
     * @param size the size of the image
     * @return a Gravatar URL for the specified email address
     * @since 1.0.0
     */
    public static String getGravatarUrl(String emailAddress, int size) {
        return getGravatarUrl(emailAddress) + "&s=" + size;
    }

}
