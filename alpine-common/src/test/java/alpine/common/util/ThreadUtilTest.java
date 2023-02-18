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

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Test;

public class ThreadUtilTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    void determineNumberOfWorkerThreadsTest() {
        // TODO: FIX ME
        //environmentVariables.set("ALPINE_WORKER_THREADS", "10");
        //Assert.assertEquals(10, ThreadUtil.determineNumberOfWorkerThreads());
        //environmentVariables.set("ALPINE_WORKER_THREADS", "0");
        //Assert.assertTrue(ThreadUtil.determineNumberOfWorkerThreads() > 0);
    }
}
