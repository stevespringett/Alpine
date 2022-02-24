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

/**
 * A collection of useful Thread utilities.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public final class ThreadUtil {

    /**
     * Default constructor
     */
    private ThreadUtil() { }

    /**
     * Calculates the number of worker threads to use. Minimum return value is 1.
     * @return the number of worker threads
     * @since 1.0.0
     */
    public static int determineNumberOfWorkerThreads() {
        final int threads = Config.getInstance().getPropertyAsInt(Config.AlpineKey.WORKER_THREADS);
        if (threads > 0) {
            return threads;
        } else if (threads == 0) {
            final int cores = SystemUtil.getCpuCores();
            final int multiplier = Config.getInstance().getPropertyAsInt(Config.AlpineKey.WORKER_THREAD_MULTIPLIER);
            if (multiplier > 0) {
                return cores * multiplier;
            } else {
                return cores;
            }
        }
        return 1; // We have to have a minimum of 1 thread
    }
}
