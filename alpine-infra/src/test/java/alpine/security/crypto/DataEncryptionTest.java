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
package alpine.security.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;

public class DataEncryptionTest {

    private static SecretKey secretKey;

    @BeforeAll
    public static void beforeClass() throws Exception {
        final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.init(256, random);
        secretKey = keyGen.generateKey();
    }

    @Test
    public void encryptAndDecryptAsBytes1Test() throws Exception {
        byte[] bytes = DataEncryption.encryptAsBytes(secretKey, "This is encrypted text");
        Assertions.assertTrue(bytes.length > 0);
        Assertions.assertEquals("This is encrypted text", new String(DataEncryption.decryptAsBytes(secretKey, bytes)));
    }

    @Test
    public void encryptAndDecryptAsBytes2Test() throws Exception {
        byte[] bytes = DataEncryption.encryptAsBytes("This is encrypted text");
        Assertions.assertTrue(bytes.length > 0);
        Assertions.assertEquals("This is encrypted text", new String(DataEncryption.decryptAsBytes(bytes)));
    }

    @Test
    public void encryptAndDecryptAsString1Test() throws Exception {
        String enc = DataEncryption.encryptAsString(secretKey, "This is encrypted text");
        Assertions.assertTrue(enc.length() > 0);
        Assertions.assertEquals("This is encrypted text", DataEncryption.decryptAsString(secretKey, enc));
    }

    @Test
    public void encryptAndDecryptAsString2Test() throws Exception {
        String enc = DataEncryption.encryptAsString("This is encrypted text");
        Assertions.assertTrue(enc.length() > 0);
        Assertions.assertEquals("This is encrypted text", DataEncryption.decryptAsString(enc));
    }
}
