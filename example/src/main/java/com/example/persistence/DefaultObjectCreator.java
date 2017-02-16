/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.persistence;

import alpine.auth.PasswordService;
import alpine.model.ManagedUser;
import alpine.model.Team;
import alpine.persistence.QueryManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class simply creates objects on startup. The example application uses
 * an in-memory database so therefore we want to populate that database with
 * a known set of objects in order to demo certain functionality.
 */
public class DefaultObjectCreator implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        try (QueryManager qm = new QueryManager()) {
            ManagedUser admin = qm.createManagedUser("admin", new String(PasswordService.createHash("admin".toCharArray())));
            Team defaultTeam = qm.createTeam("Default Team", true);
            qm.addUserToTeam(admin, defaultTeam);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
    }
}
