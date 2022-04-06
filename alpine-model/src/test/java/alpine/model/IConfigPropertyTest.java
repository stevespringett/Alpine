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

import org.junit.Assert;
import org.junit.Test;

public class IConfigPropertyTest {

    @Test
    public void testEnums() {
        Assert.assertEquals("BOOLEAN", IConfigProperty.PropertyType.BOOLEAN.name());
        Assert.assertEquals("INTEGER", IConfigProperty.PropertyType.INTEGER.name());
        Assert.assertEquals("NUMBER", IConfigProperty.PropertyType.NUMBER.name());
        Assert.assertEquals("STRING", IConfigProperty.PropertyType.STRING.name());
        Assert.assertEquals("ENCRYPTEDSTRING", IConfigProperty.PropertyType.ENCRYPTEDSTRING.name());
        Assert.assertEquals("TIMESTAMP", IConfigProperty.PropertyType.TIMESTAMP.name());
        Assert.assertEquals("URL", IConfigProperty.PropertyType.URL.name());
        Assert.assertEquals("UUID", IConfigProperty.PropertyType.UUID.name());
    }
}
