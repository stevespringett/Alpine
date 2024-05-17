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
package com.example.persistence;

import alpine.auth.PasswordService;
import alpine.model.ManagedUser;
import alpine.model.Team;
import alpine.persistence.AlpineQueryManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * This class simply creates objects on startup. The example application uses
 * an in-memory database so therefore we want to populate that database with
 * a known set of objects in order to demo certain functionality.
 *
 * @author Steve Springett
 */
public class DefaultObjectCreator implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (AlpineQueryManager qm = new AlpineQueryManager()) {
            final ManagedUser admin = qm.createManagedUser("admin", new String(PasswordService.createHash("admin".toCharArray())));
            final Team defaultTeam = qm.createTeam("Default Team", true);
            qm.addUserToTeam(admin, defaultTeam);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Intentionally empty to satisfy interface
    }
}
