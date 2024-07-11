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

import java.sql.Timestamp;
import java.util.Date;

public class InstalledUpgradesTest {

    @Test
    public void idTest() {
        InstalledUpgrades upgrades = new InstalledUpgrades();
        upgrades.setId(123L);
        Assertions.assertEquals(123L, upgrades.getId());
    }

    @Test
    public void upgradeClassTest() {
        InstalledUpgrades upgrades = new InstalledUpgrades();
        upgrades.setUpgradeClass("com.example.UpgradeClass");
        Assertions.assertEquals("com.example.UpgradeClass", upgrades.getUpgradeClass());
    }

    @Test
    public void startedTest() {
        Timestamp ts = Timestamp.from(new Date().toInstant());
        InstalledUpgrades upgrades = new InstalledUpgrades();
        upgrades.setStartTime(ts);
        Assertions.assertEquals(ts, upgrades.getStartTime());
    }

    @Test
    public void endTimeTest() {
        Timestamp ts = Timestamp.from(new Date().toInstant());
        InstalledUpgrades upgrades = new InstalledUpgrades();
        upgrades.setEndTime(ts);
        Assertions.assertEquals(ts, upgrades.getEndTime());
    }
}
