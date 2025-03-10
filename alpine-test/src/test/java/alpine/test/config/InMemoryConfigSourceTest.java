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
package alpine.test.config;

import alpine.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryConfigSourceTest {

    @Test
    @SuppressWarnings("deprecation")
    void test() {
        assertNull(Config.getInstance().getProperty("foo.bar"));

        InMemoryConfigSource.setProperty("foo.bar", "baz");
        InMemoryConfigSource.setProperty("oof.rab", "${foo.bar}");
        assertEquals("baz", Config.getInstance().getProperty("foo.bar"));
        assertEquals("baz", Config.getInstance().getProperty("oof.rab"));

        InMemoryConfigSource.clear();
        assertNull(Config.getInstance().getProperty("foo.bar"));
        assertNull(Config.getInstance().getProperty("oof.rab"));
    }

}