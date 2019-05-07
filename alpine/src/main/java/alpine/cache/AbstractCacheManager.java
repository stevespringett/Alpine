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
package alpine.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * AbstractCacheManager provides a per-class object cache.
 * @since 1.5.0
 */
public abstract class AbstractCacheManager {

    private final long expiresAfter;
    private final TimeUnit timeUnit;
    private final long maxSize;
    private final ConcurrentHashMap<Class, Cache<String, Object>> typeMap = new ConcurrentHashMap<>();

    protected AbstractCacheManager(final long expiresAfter, final TimeUnit timeUnit, final long maxSize) {
        this.expiresAfter = expiresAfter;
        this.timeUnit = timeUnit;
        this.maxSize = maxSize;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final Class clazz, final String key) {
        final Cache<String, Object> cache = typeMap.get(clazz);
        return (cache == null) ? null : (T) cache.getIfPresent(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final Class clazz, final String key, final Function mappingFunction) {
        final Cache<String, Object> cache = typeMap.get(clazz);
        return (cache == null) ? null : (T) cache.get(key, mappingFunction);
    }

    public void put(final String key, final Object object) {
        Cache<String, Object> cache = typeMap.get(object.getClass());
        if (cache == null) {
            cache = buildCache();
            typeMap.put(object.getClass(), cache);
        }
        cache.put(key, object);
    }

    private Cache<String, Object> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expiresAfter, timeUnit)
                .maximumSize(maxSize)
                .build();
    }
}