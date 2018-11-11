/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.util;

import org.junit.Assert;
import org.junit.Test;

public class JavaVersionTest {

    @Test
    public void testDefaultConstructor() {
        JavaVersion version = new JavaVersion();
        Assert.assertTrue(version.getMajor() > 0);
        Assert.assertTrue(version.getMinor() >= 0);
        Assert.assertTrue(version.getUpdate() > 0);
    }

    @Test
    public void testOracleJava8Versioning() {
        JavaVersion version = new JavaVersion("1.8.0_171-b11");
        Assert.assertEquals(8, version.getMajor());
        Assert.assertEquals(0, version.getMinor());
        Assert.assertEquals(171, version.getUpdate());
    }

    @Test
    // https://github.com/DependencyTrack/dependency-track/issues/223
    public void testDebianOpenJDK8Versioning() {
        JavaVersion version = new JavaVersion("1.8.0_181-8u181-b13-1~deb9u1-b13");
        Assert.assertEquals(8, version.getMajor());
        Assert.assertEquals(0, version.getMinor());
        Assert.assertEquals(181, version.getUpdate());
    }
}
