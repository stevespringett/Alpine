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

import alpine.Config;
import alpine.model.ManagedUser;
import org.mindrot.jbcrypt.BCrypt;

/**
 * A wrapper around BCrypt used to hash and check password validity.
 *
 * @since 1.0.0
 */
public class PasswordService {

    private static final int ROUNDS = Config.getInstance().getPropertyAsInt(Config.Key.BCRYPT_ROUNDS);

    private PasswordService() { }

    /**
     * Generates a salt using the configured number of rounds (determined by {@link Config.Key#BCRYPT_ROUNDS)
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

}
