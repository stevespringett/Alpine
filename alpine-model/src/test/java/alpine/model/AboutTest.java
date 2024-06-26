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
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class AboutTest {

    @Test
    public void getterTest() {
        About about = new About();
        Assert.assertEquals("Unknown Alpine Application", about.getApplication());
        Assert.assertEquals("0.0.0", about.getVersion());
        Assert.assertEquals("1970-01-01 00:00:00", about.getTimestamp());
        Assert.assertNull(about.getUuid());

        Assert.assertEquals("alpine-model", about.getFramework().getName());
        Assert.assertTrue(about.getFramework().getVersion().startsWith("3."));
        Assert.assertTrue(about.getFramework().getTimestamp().startsWith("20"));
        Assert.assertTrue(UuidUtil.isValidUUID(about.getFramework().getUuid()));

        final Map<String, Object> providerData = about.getProviderData();
        Assert.assertEquals(1, providerData.size());
        Assert.assertNotNull(providerData.get("test"));
        Assert.assertTrue(providerData.get("test") instanceof Map);
        Assert.assertEquals("bar", ((Map<String, Object>) providerData.get("test")).get("foo"));
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
