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

import alpine.security.crypto.KeyManager;
import org.junit.Assert;
import org.junit.Test;
import javax.crypto.SecretKey;
import java.security.KeyPair;

public class KeyManagerTest {

    @Test
    public void keyPairTest() throws Exception {
        KeyPair keyPair = KeyManager.getInstance().generateKeyPair();
        Assert.assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
        Assert.assertEquals("PKCS#8", keyPair.getPrivate().getFormat());
        KeyManager.getInstance().save(keyPair);
        Assert.assertTrue(KeyManager.getInstance().keyPairExists());
        Assert.assertEquals(keyPair, KeyManager.getInstance().getKeyPair());
    }

    @Test
    public void secretKeyTest() throws Exception {
        SecretKey secretKey = KeyManager.getInstance().generateSecretKey();
        Assert.assertEquals("AES", secretKey.getAlgorithm());
        Assert.assertEquals("RAW", secretKey.getFormat());
        KeyManager.getInstance().save(secretKey);
        Assert.assertTrue(KeyManager.getInstance().secretKeyExists());
        Assert.assertEquals(secretKey, KeyManager.getInstance().getSecretKey());
    }
}
