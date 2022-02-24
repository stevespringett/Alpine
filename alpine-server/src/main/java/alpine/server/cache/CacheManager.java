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
package alpine.server.cache;

import java.util.concurrent.TimeUnit;

/**
 * Provides an implementation of a per-class object cache. CacheManager will
 * automatically evoke objects from cache after 60 minutes and holds a maximum
 * of 1000 objects (per-object type).
 * @since 1.5.0
 */
public final class CacheManager extends AbstractCacheManager {

    private static final CacheManager INSTANCE = new CacheManager();

    /**
     * Private constructor.
     */
    private CacheManager() {
        super(60, TimeUnit.MINUTES, 1000);
    }

    public static CacheManager getInstance() {
        return INSTANCE;
    }
}
