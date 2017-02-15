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
package com.example.event;

import alpine.event.LdapSyncEvent;
import alpine.event.framework.EventService;
import alpine.tasks.LdapSyncTask;
import com.example.tasks.TaskScheduler;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class EventSubsystemInitializer implements ServletContextListener {

    // Starts the EventService
    private static final EventService EVENT_SERVICE = EventService.getInstance();

    public void contextInitialized(ServletContextEvent event) {
        EVENT_SERVICE.subscribe(LdapSyncEvent.class, LdapSyncTask.class);

        TaskScheduler.getInstance();
    }

    public void contextDestroyed(ServletContextEvent event) {
        TaskScheduler.getInstance().shutdown();

        EVENT_SERVICE.unsubscribe(LdapSyncTask.class);
        EVENT_SERVICE.shutdown();
    }
}