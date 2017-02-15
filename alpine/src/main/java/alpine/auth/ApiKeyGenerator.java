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
package alpine.auth;

import java.security.SecureRandom;

public class ApiKeyGenerator {

    private static char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();

    private ApiKeyGenerator() {}

    /**
     * Generates a 40 character cryptographically secure API key
     *
     * @since 1.0.0
     */
    public static String generate() {
        return generate(40);
    }

    private static String generate(int chars) {
        SecureRandom secureRandom = new SecureRandom();
        char[] buff = new char[chars];
        for (int i = 0; i < chars; ++i) {
            if ((i % 10) == 0) {
                secureRandom.setSeed(secureRandom.nextLong());
            }
            buff[i] = VALID_CHARACTERS[secureRandom.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(buff);
    }
}
