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

public class ByteFormatTest {

    @Test
    public void formatTest() {
        ByteFormat bf = new ByteFormat();
        Assertions.assertEquals("100 bytes", bf.format(100));
        Assertions.assertEquals("999 bytes", bf.format(999L));
    }

    @Test
    public void format2Test() {
        ByteFormat bf = new ByteFormat();
        Assertions.assertEquals("100 bytes", bf.format2(100));
        Assertions.assertEquals("999 bytes", bf.format2(999L));
        Assertions.assertEquals("9.8 KB (10,000 bytes)", bf.format2(10000));
        Assertions.assertEquals("97.7 KB (99,999 bytes)", bf.format2(99999L));
    }
}
