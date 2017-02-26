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
package alpine.auth;

import alpine.Config;
import alpine.model.ManagedUser;
import org.mindrot.jbcrypt.BCrypt;

/**
 * A wrapper around BCrypt used to hash and check password validity.
 *
 * @since 1.0.0
 */
public class PasswordService {

    private static final int ROUNDS = Config.getInstance().getPropertyAsInt(Config.AlpineKey.BCRYPT_ROUNDS);

    private PasswordService() { }

    /**
     * Generates a salt using the configured number of rounds (determined by {@link Config.AlpineKey#BCRYPT_ROUNDS)
     * and hashes the password.
     *
     * @since 1.0.0
     */
    public static char[] createHash(char[] password) {
        // Todo: remove String when Jbcrypt supports char[]
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(ROUNDS)).toCharArray();
    }

    /**
     * Hashes the specified password using the specified salt.
     *
     * @since 1.0.0
     */
    public static char[] createHash(char[] password, char[] salt) {
        // Todo: remove String when Jbcrypt supports char[]
        return BCrypt.hashpw(new String(password), new String(salt)).toCharArray();
    }

    /**
     * Checks the validity of the asserted password against a ManagedUsers actual hashed password.
     *
     * @since 1.0.0
     */
    public static boolean matches(char[] assertedPassword, ManagedUser user) {
        // Todo: remove String when Jbcrypt supports char[]
        return BCrypt.checkpw(new String(assertedPassword), new String(user.getPassword()));
    }

    /**
     * Checks the asserted BCrypt formatted hashed password and determines if the password should
     * be rehashed or not. If the BCrypt work factor is increased (from 12 to 14 for example),
     * passwords should be evaluated and if the existing stored hash uses a work factor less than
     * what is configured, then the assertedPassword should be rehashed. The same does not apply
     * in reverse. Stored hashed passwords with a work factor greater than the configured work factor
     * will return false, meaning they should not be rehashed.
     *
     * If the assertedPasswords length is less than the minimum length of a BCrypt hash, this method
     * will return true.
     *
     * @since 1.0.0
     */
    public static boolean shouldRehash(char[] assertedPassword) {
        int rounds;
        if (assertedPassword.length < 59) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(assertedPassword[4]);
        if (assertedPassword[5] != '$') {
            sb.append(assertedPassword[5]);
        }
        rounds = Integer.valueOf(sb.toString());
        return rounds < ROUNDS;
    }

}
