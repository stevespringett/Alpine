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

public class VersionComparatorTest {

    @Test
    public void isNewerThanTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assertions.assertFalse(vc.isNewerThan(new VersionComparator("2.1.0")));
        Assertions.assertFalse(vc.isNewerThan(new VersionComparator("2.2.0")));
        Assertions.assertFalse(vc.isNewerThan(new VersionComparator("3.0.0")));
        Assertions.assertTrue(vc.isNewerThan(new VersionComparator("2.0.0")));
        Assertions.assertTrue(vc.isNewerThan(new VersionComparator("1.9.0")));
    }

    @Test
    public void isOlderThanTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assertions.assertFalse(vc.isOlderThan(new VersionComparator("2.1.0")));
        Assertions.assertTrue(vc.isOlderThan(new VersionComparator("2.2.0")));
        Assertions.assertTrue(vc.isOlderThan(new VersionComparator("3.0.0")));
        Assertions.assertFalse(vc.isOlderThan(new VersionComparator("2.0.0")));
        Assertions.assertFalse(vc.isOlderThan(new VersionComparator("1.9.0")));
    }

    @Test
    public void equalsTest() {
        VersionComparator vc = new VersionComparator("2.1.0");
        Assertions.assertTrue(vc.equals(new VersionComparator("2.1.0")));
        Assertions.assertFalse(vc.equals(new VersionComparator("2.2.0")));
        Assertions.assertFalse(vc.equals(new VersionComparator("3.0.0")));
        Assertions.assertFalse(vc.equals(new VersionComparator("2.0.0")));
        Assertions.assertFalse(vc.equals(new VersionComparator("1.9.0")));
    }

    @Test
    public void getterTest() {
        VersionComparator vc = new VersionComparator("2.1.0-SNAPSHOT.5");
        Assertions.assertEquals(2, vc.getMajor());
        Assertions.assertEquals(1, vc.getMinor());
        Assertions.assertEquals(0, vc.getRevision());
        Assertions.assertEquals(5, vc.getPrereleaseNumber());
    }
}
