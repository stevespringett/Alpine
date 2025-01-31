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
package alpine.security;

import alpine.Config;
import alpine.model.ApiKey;

import java.security.SecureRandom;

/**
 * Class used to securely generate API keys.
 * @author Steve Springett
 * @since 1.0.0
 */
public final class ApiKeyGenerator {

    private static final char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
    private static final String prefix = Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);
    private static final int PUBLIC_ID_LENGTH = ApiKey.PUBLIC_ID_LENGTH;
    private static final int API_KEY_LENGTH = ApiKey.API_KEY_LENGTH;
    private static final char API_KEY_SEPERATOR  = ApiKey.API_KEY_SEPERATOR;

    /**
     * Private constructor
     */
    private ApiKeyGenerator() { }

    /**
     * Generates a prefixed cryptographically secure API key of 32 characters not including the prefix length.
     *  @return a String representation of the API key
     * @since 1.0.0
     */
    public static String generate() {
        return generate(PUBLIC_ID_LENGTH, API_KEY_LENGTH);
    }

    /**
     * Generates a cryptographically secure API key of the specified length having the configured API key prefix and the public ID.
     * @param pubIdLength the length of the Public ID to generate not including the prefix length
     * @param keyLength the length of the API key to generate not including the prefix length
     * @return a String representation of the API key
     */
    public static String generate(final int pubIdLength, final int keyLength) {
        final SecureRandom secureRandom = new SecureRandom();
        final char[] buff = new char[pubIdLength + keyLength + 1];
        for (int i = 0; i < pubIdLength + keyLength + 1; ++i) {
            if (i % 10 == 0) {
                secureRandom.setSeed(secureRandom.nextLong());
            }
            buff[i] = VALID_CHARACTERS[secureRandom.nextInt(VALID_CHARACTERS.length)];
        }
        buff[pubIdLength] = API_KEY_SEPERATOR;
        var separatorIfNeeded = prefix.endsWith(Character.toString(API_KEY_SEPERATOR)) ? "" : API_KEY_SEPERATOR;

        return prefix + separatorIfNeeded + String.valueOf(buff);
    }
}
