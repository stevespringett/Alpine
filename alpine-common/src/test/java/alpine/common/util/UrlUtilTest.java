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
package alpine.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class UrlUtilTest {

    @Test
    public void normalizationStringTest() {
        Assertions.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com"));
        Assertions.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com/"));
        Assertions.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com//////"));
    }

    @Test
    public void normalizationURLTest() throws Exception {
        Assertions.assertEquals("http://www.example.com", URI.create(UrlUtil.normalize("http://www.example.com")).toURL().toExternalForm());
        Assertions.assertEquals("http://www.example.com", URI.create(UrlUtil.normalize("http://www.example.com/")).toURL().toExternalForm());
        Assertions.assertEquals("http://www.example.com", URI.create(UrlUtil.normalize("http://www.example.com//////")).toURL().toExternalForm());
    }
}
