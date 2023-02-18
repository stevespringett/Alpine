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

public class BooleanUtilTest {

    @Test
    void valueOfTest() {
        Assertions.assertTrue(BooleanUtil.valueOf("TruE"));
        Assertions.assertTrue(BooleanUtil.valueOf("1"));
        Assertions.assertFalse(BooleanUtil.valueOf("3"));
        Assertions.assertFalse(BooleanUtil.valueOf("0"));
        Assertions.assertFalse(BooleanUtil.valueOf("TTRUE"));
        Assertions.assertFalse(BooleanUtil.valueOf(""));
        Assertions.assertFalse(BooleanUtil.valueOf(null));
    }

    @Test
    void isNullTest() {
        Object o = new Object();
        Object n = null;
        Assertions.assertFalse(BooleanUtil.isNull(o));
        Assertions.assertTrue(BooleanUtil.isNull(n));
    }

    @Test
    void isNotNullTest() {
        Object o = new Object();
        Object n = null;
        Assertions.assertTrue(BooleanUtil.isNotNull(o));
        Assertions.assertFalse(BooleanUtil.isNotNull(n));
    }
}
