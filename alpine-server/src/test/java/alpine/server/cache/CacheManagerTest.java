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
package alpine.server.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CacheManagerTest {

    @Test
    public void putGetMaintTest() {
        CacheManager cacheManager = CacheManager.getInstance();
        for (int i = 1; i <= 1100; i++) {
            cacheManager.put("key-" + i , "value-" + i);
        }
        cacheManager.maintenance(String.class);
        for (int i = 1; i <= 100; i++) {
            Assertions.assertNull(cacheManager.get(String.class, "key-" + i));
        }
        for (int i = 101; i <= 1100; i++) {
            Assertions.assertEquals("value-" + i, cacheManager.get(String.class, "key-" + i));
        }
    }

    @Test
    public void removeTest() {
        CacheManager cacheManager = CacheManager.getInstance();

        cacheManager.put("CacheManagerTest.removeTest", "testValue");
        Assertions.assertEquals("testValue", cacheManager.get(String.class, "CacheManagerTest.removeTest"));

        cacheManager.remove(String.class, "CacheManagerTest.removeTest");
        Assertions.assertNull(cacheManager.get(String.class, "CacheManagerTest.removeTest"));
    }

}
