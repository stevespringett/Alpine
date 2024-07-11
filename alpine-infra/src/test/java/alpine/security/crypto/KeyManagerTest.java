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
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;

public class KeyManagerTest {

    @Test
    public void keyPairTest() throws Exception {
        KeyPair keyPair = KeyManager.getInstance().generateKeyPair();
        Assertions.assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
        Assertions.assertEquals("PKCS#8", keyPair.getPrivate().getFormat());
        KeyManager.getInstance().save(keyPair);
        Assertions.assertTrue(KeyManager.getInstance().keyPairExists());
        Assertions.assertEquals(keyPair, KeyManager.getInstance().getKeyPair());
    }

    @Test
    public void secretKeyTest() throws Exception {
        SecretKey secretKey = KeyManager.getInstance().generateSecretKey();
        Assertions.assertEquals("AES", secretKey.getAlgorithm());
        Assertions.assertEquals("RAW", secretKey.getFormat());
        KeyManager.getInstance().save(secretKey);
        Assertions.assertTrue(KeyManager.getInstance().secretKeyExists());
        Assertions.assertEquals(secretKey, KeyManager.getInstance().getSecretKey());
    }

    @Test
    public void saveAndLoadSecretKeyInLegacyFormatTest() throws Exception {
        final SecretKey secretKey = KeyManager.getInstance().generateSecretKey();
        KeyManager.getInstance().save(secretKey);
        final SecretKey loadedKey = KeyManager.getInstance().loadSecretKey();
        Assertions.assertArrayEquals(secretKey.getEncoded(), loadedKey.getEncoded());
    }

    @Test
    public void saveAndLoadSecretKeyInEncodedFormatTest() throws Exception {
        final SecretKey secretKey = KeyManager.getInstance().generateSecretKey();
        KeyManager.getInstance().saveEncoded(secretKey);
        final SecretKey loadedKey = KeyManager.getInstance().loadEncodedSecretKey();
        Assertions.assertArrayEquals(secretKey.getEncoded(), loadedKey.getEncoded());
    }

    @Test
    public void secretKeyHasOldFormatTest() throws Exception {
        final SecretKey secretKey = KeyManager.getInstance().generateSecretKey();
        KeyManager.getInstance().save(secretKey);
        Assertions.assertTrue(KeyManager.getInstance().secretKeyHasOldFormat());
        KeyManager.getInstance().saveEncoded(secretKey);
        Assertions.assertFalse(KeyManager.getInstance().secretKeyHasOldFormat());
    }

}
