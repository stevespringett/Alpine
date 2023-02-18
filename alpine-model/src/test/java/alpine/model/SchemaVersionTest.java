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
package alpine.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SchemaVersionTest {

    @Test
    void idTest() {
        SchemaVersion sv = new SchemaVersion();
        sv.setId(123L);
        Assertions.assertEquals(123L, sv.getId());
    }

    @Test
    void versionTest() {
        SchemaVersion sv = new SchemaVersion();
        sv.setVersion("3.5.0");
        Assertions.assertEquals("3.5.0", sv.getVersion());
    }

}
