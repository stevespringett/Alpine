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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ApiKeyTest {
    final String prefix = Config.getInstance().getProperty(Config.AlpineKey.API_KEY_PREFIX);

    @Test
    public void idTest() {
        ApiKey key = new ApiKey();
        key.setId(123L);
        Assertions.assertEquals(123L, key.getId());
    }

    @Test
    public void keyTest() {
        {
            ApiKey key = new ApiKey();
            key.setKey("12345678901234567890");
            Assertions.assertEquals("12345678901234567890", key.getKey());
            Assertions.assertEquals("****************7890", key.getName());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey(prefix + "12345678901234567890");
            Assertions.assertEquals(prefix + "12345678901234567890", key.getKey());
            Assertions.assertEquals(prefix + "****************7890", key.getName());
        }
    }

    @Test
    public void maskTest() {
        {
            ApiKey key = new ApiKey();
            key.setKey("12345678901234567890");
            Assertions.assertEquals("****************7890", key.getMaskedKey());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey("1234ABCabc+_=!?-*");
            Assertions.assertEquals("*************!?-*", key.getMaskedKey());
        }
        {
            ApiKey key = new ApiKey();
            key.setKey("1234");
            Assertions.assertEquals("1234", key.getMaskedKey());
        }
        {
            // test with prefix
            ApiKey key = new ApiKey();
            key.setKey(prefix + "1234567890");
            Assertions.assertEquals(prefix + "******7890", key.getMaskedKey());
        }
    }

    @Test
    public void teamsTest() {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team());
        ApiKey key = new ApiKey();
        key.setTeams(teams);
        Assertions.assertEquals(teams, key.getTeams());
        Assertions.assertEquals(1, key.getTeams().size());
    }
}
