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

import java.security.SecureRandom;

/**
 * Class used to securely generate API keys.
 * @author Steve Springett
 * @since 1.0.0
 */
public final class ApiKeyGenerator {

    private static final char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();

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
        return generate(32);
    }

    /**
     * Generates a cryptographically secure API key of the specified length having the configured API key prefix.
     * @param chars the length of the API key to generate not including the prefix length
     * @return a String representation of the API key
     */
    public static String generate(final int chars) {
        final SecureRandom secureRandom = new SecureRandom();
        final char[] buff = new char[chars];
        for (int i = 0; i < chars; ++i) {
            if (i % 10 == 0) {
                secureRandom.setSeed(secureRandom.nextLong());
            }
            buff[i] = VALID_CHARACTERS[secureRandom.nextInt(VALID_CHARACTERS.length)];
        }

        return getApiKeyPrefix() + String.valueOf(buff);
    }

    private static String getApiKeyPrefix() {
        return Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);
    }
}
