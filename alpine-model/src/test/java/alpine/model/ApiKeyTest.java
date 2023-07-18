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

import alpine.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ApiKeyTest {
    final String prefix = Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);

    @Test
    public void idTest() {
        ApiKey key = new ApiKey();
        key.setId(123L);
        Assert.assertEquals(123L, key.getId());
    }

    @Test
    public void keyTest() {
        {
            ApiKey key = new ApiKey();
            key.setKey("12345678901234567890");
            Assert.assertEquals("12345678901234567890", key.getKey());
            Assert.assertEquals("****************7890", key.getName());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey(prefix + "12345678901234567890");
            Assert.assertEquals(prefix + "12345678901234567890", key.getKey());
            Assert.assertEquals(prefix + "****************7890", key.getName());
        }
    }

    @Test
    public void maskTest() {
        {
            ApiKey key = new ApiKey();
            key.setKey("12345678901234567890");
            Assert.assertEquals("****************7890", key.getMaskedKey());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey("1234ABCabc+_=!?-*");
            Assert.assertEquals("*************!?-*", key.getMaskedKey());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey("1234");
            Assert.assertEquals("1234", key.getMaskedKey());
        }
        {
            // test with prefix
            ApiKey key = new ApiKey();
            key.setKey(prefix + "1234567890");
            Assert.assertEquals(prefix + "******7890", key.getMaskedKey());
        }
    }

    @Test
    public void teamsTest() {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team());
        ApiKey key = new ApiKey();
        key.setTeams(teams);
        Assert.assertEquals(teams, key.getTeams());
        Assert.assertEquals(1, key.getTeams().size());
    }
}
