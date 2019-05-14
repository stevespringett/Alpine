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

public class VersionComparatorTest {

    @Test
    public void isNewerThanTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assert.assertFalse(vc.isNewerThan(new VersionComparator("2.1.0")));
        Assert.assertFalse(vc.isNewerThan(new VersionComparator("2.2.0")));
        Assert.assertFalse(vc.isNewerThan(new VersionComparator("3.0.0")));
        Assert.assertTrue(vc.isNewerThan(new VersionComparator("2.0.0")));
        Assert.assertTrue(vc.isNewerThan(new VersionComparator("1.9.0")));
    }

    @Test
    public void isOlderThanTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assert.assertFalse(vc.isOlderThan(new VersionComparator("2.1.0")));
        Assert.assertTrue(vc.isOlderThan(new VersionComparator("2.2.0")));
        Assert.assertTrue(vc.isOlderThan(new VersionComparator("3.0.0")));
        Assert.assertFalse(vc.isOlderThan(new VersionComparator("2.0.0")));
        Assert.assertFalse(vc.isOlderThan(new VersionComparator("1.9.0")));
    }

    @Test
    public void equalsTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assert.assertTrue(vc.equals(new VersionComparator("2.1.0")));
        Assert.assertFalse(vc.equals(new VersionComparator("2.2.0")));
        Assert.assertFalse(vc.equals(new VersionComparator("3.0.0")));
        Assert.assertFalse(vc.equals(new VersionComparator("2.0.0")));
        Assert.assertFalse(vc.equals(new VersionComparator("1.9.0")));
    }

    @Test
    public void getterTest() {
        VersionComparator vc = new VersionComparator("2.1.0-SNAPSHOT.5");
        Assert.assertEquals(2, vc.getMajor());
        Assert.assertEquals(1, vc.getMinor());
        Assert.assertEquals(0, vc.getRevision());
        Assert.assertEquals(5, vc.getPrereleaseNumber());
    }
}
