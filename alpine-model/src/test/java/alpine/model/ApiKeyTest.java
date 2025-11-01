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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

public class ApiKeyTest {

    @Test
    public void maskTest() {
        final var apiKey = new ApiKey();
        apiKey.setPublicId("TL1xa");
        assertThat(apiKey.getMaskedKey()).isEqualTo("alpine_TL1xa********************************");
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

    @Test
    public void permissionsTest() {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(new Permission());
        ApiKey user = new ApiKey();
        user.setPermissions(permissions);
        Assertions.assertEquals(permissions, user.getPermissions());
        Assertions.assertEquals(1, user.getPermissions().size());
    }
}
