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

import org.junit.Assert;
import org.junit.Test;

public class BooleanUtilTest {

    @Test
    public void valueOfTest() {
        Assert.assertTrue(BooleanUtil.valueOf("TruE"));
        Assert.assertTrue(BooleanUtil.valueOf("1"));
        Assert.assertFalse(BooleanUtil.valueOf("3"));
        Assert.assertFalse(BooleanUtil.valueOf("0"));
        Assert.assertFalse(BooleanUtil.valueOf("TTRUE"));
        Assert.assertFalse(BooleanUtil.valueOf(""));
        Assert.assertFalse(BooleanUtil.valueOf(null));
    }

    @Test
    public void isNullTest() {
        Object o = new Object();
        Object n = null;
        Assert.assertFalse(BooleanUtil.isNull(o));
        Assert.assertTrue(BooleanUtil.isNull(n));
    }

    @Test
    public void isNotNullTest() {
        Object o = new Object();
        Object n = null;
        Assert.assertTrue(BooleanUtil.isNotNull(o));
        Assert.assertFalse(BooleanUtil.isNotNull(n));
    }
}
