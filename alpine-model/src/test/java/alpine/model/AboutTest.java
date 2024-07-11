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
package alpine.model;

import alpine.common.AboutProvider;
import alpine.common.util.UuidUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class AboutTest {

    @Test
    public void getterTest() {
        About about = new About();
        Assertions.assertEquals("Unknown Alpine Application", about.getApplication());
        Assertions.assertEquals("0.0.0", about.getVersion());
        Assertions.assertEquals("1970-01-01 00:00:00", about.getTimestamp());
        Assertions.assertNull(about.getUuid());

        Assertions.assertEquals("alpine-model", about.getFramework().getName());
        Assertions.assertTrue(about.getFramework().getVersion().startsWith("3."));
        Assertions.assertTrue(about.getFramework().getTimestamp().startsWith("20"));
        Assertions.assertTrue(UuidUtil.isValidUUID(about.getFramework().getUuid()));

        final Map<String, Object> providerData = about.getProviderData();
        Assertions.assertEquals(1, providerData.size());
        Assertions.assertNotNull(providerData.get("test"));
        Assertions.assertTrue(providerData.get("test") instanceof Map);
        Assertions.assertEquals("bar", ((Map<String, Object>) providerData.get("test")).get("foo"));
    }

    public static class TestProvider implements AboutProvider {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public Map<String, Object> collect() {
            return Map.of("foo", "bar");
        }

    }

}
