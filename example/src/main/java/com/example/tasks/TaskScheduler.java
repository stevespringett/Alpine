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
package com.example.tasks;

import alpine.event.LdapSyncEvent;
import alpine.tasks.AlpineTaskScheduler;

public class TaskScheduler extends AlpineTaskScheduler {

    // Holds an instance of TaskScheduler
    private static final TaskScheduler instance = new TaskScheduler();

    private TaskScheduler() {

        // Creates a new event that executes every 6 hours (21600000) after an initial 10 second (10000) delay
        scheduleEvent(new LdapSyncEvent(), 10000, 21600000);
    }

    /**
     * Return an instance of the TaskScheduler instance
     * @return a TaskScheduler instance
     */
    public static TaskScheduler getInstance() {
        return instance;
    }

}