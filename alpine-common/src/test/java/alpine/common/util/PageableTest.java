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

import java.util.ArrayList;
import java.util.List;

public class PageableTest {

    @Test
    public void pageableTest() {
        List list = new ArrayList<>();
        for (int i=1; i<=1240; i++) {
            list.add(String.valueOf(i));
        }
        Pageable pageable = new Pageable(100, list);
        Assert.assertEquals(1, pageable.getCurrentPage());
        Assert.assertEquals(13, pageable.getTotalPages());
        Assert.assertEquals(100, pageable.getPageSize());
        Assert.assertTrue(pageable.hasMorePages());
        Assert.assertFalse(pageable.isPaginationComplete());

        for (int i=1; i<=13; i++) {
            if (i <= 12) {
                Assert.assertEquals(100, pageable.getPaginatedList().size());
                Assert.assertTrue(pageable.hasMorePages());
            } else if (i == 13) {
                Assert.assertEquals(40, pageable.getPaginatedList().size());
                Assert.assertFalse(pageable.hasMorePages());
            }
            Assert.assertEquals(i, pageable.getCurrentPage());
            pageable.nextPage();
            if (i <= 12) {
                Assert.assertFalse(pageable.isPaginationComplete());
            } else if (i == 13) {
                Assert.assertTrue(pageable.isPaginationComplete());
            }
        }
    }
}
