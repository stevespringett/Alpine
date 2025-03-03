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
package alpine.common.util;

import alpine.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RestoreEnvironmentVariables;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.lang.reflect.Method;

class ThreadUtilTest {

    private static Method configReloadMethod;

    @BeforeAll
    public static void setUp() throws Exception {
        configReloadMethod = Config.class.getDeclaredMethod("reload");
        configReloadMethod.setAccessible(true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        configReloadMethod.invoke(Config.getInstance());
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        configReloadMethod.invoke(Config.getInstance()); // Ensure we're not affecting other tests.
    }

    @Test
    @RestoreEnvironmentVariables
    @SetEnvironmentVariable(key = "ALPINE_WORKER_THREADS", value = "10")
    void determineNumberOfWorkerThreadsStaticTest() throws Exception {
        configReloadMethod.invoke(Config.getInstance());

        Assertions.assertEquals(10, ThreadUtil.determineNumberOfWorkerThreads());
    }

    @Test
    @RestoreEnvironmentVariables
    @SetEnvironmentVariable(key = "ALPINE_WORKER_THREADS", value = "0")
    void determineNumberOfWorkerThreadsDynamicTest() throws Exception {
        configReloadMethod.invoke(Config.getInstance());

        Assertions.assertTrue(ThreadUtil.determineNumberOfWorkerThreads() > 0);
    }

}
