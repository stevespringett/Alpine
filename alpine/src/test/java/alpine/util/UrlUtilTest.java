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
package alpine.util;

import org.junit.Assert;
import org.junit.Test;
import java.net.URL;

public class UrlUtilTest {

    @Test
    public void normalizationStringTest() {
        Assert.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com"));
        Assert.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com/"));
        Assert.assertEquals("http://www.example.com", UrlUtil.normalize("http://www.example.com//////"));
    }

    @Test
    public void normalizationURLTest() throws Exception {
        Assert.assertEquals("http://www.example.com", new URL(UrlUtil.normalize("http://www.example.com")).toExternalForm());
        Assert.assertEquals("http://www.example.com", new URL(UrlUtil.normalize("http://www.example.com/")).toExternalForm());
        Assert.assertEquals("http://www.example.com", new URL(UrlUtil.normalize("http://www.example.com//////")).toExternalForm());
    }
}
