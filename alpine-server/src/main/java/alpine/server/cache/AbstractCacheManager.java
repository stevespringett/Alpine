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

    /**
     * Constructs a new AbstractCacheManager object.
     *
     * @param expiresAfter the number of time units to expire after
     * @param timeUnit the unit of measurement
     * @param maxSize the maximum size of the cache (per object type)
     */
    protected AbstractCacheManager(final long expiresAfter, final TimeUnit timeUnit, final long maxSize) {
        this.expiresAfter = expiresAfter;
        this.timeUnit = timeUnit;
        this.maxSize = maxSize;
    }

    /**
     * Retrieves an object (of the specified class) from cache.
     * @param clazz the class of the object to retrieve from cache
     * @param key the unique identifier of the object to retrieve from cache
     * @param <T> the object type
     * @return the cached object (if found) or null if not found
     * @since 1.5.0
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Class clazz, final String key) {
        final Cache<String, Object> cache = typeMap.get(clazz);
        return (cache == null) ? null : (T) cache.getIfPresent(key);
    }

    /**
     * Retrieves an object (of the specified class) from cache.
     * @param clazz the class of the object to retrieve from cache
     * @param key the unique identifier of the object to retrieve from cache
     * @param mappingFunction the function to call if the object is not present in cache
     * @param <T> the object type
     * @return the cached object (if found) or null if not found and a mappingFunction is not specified
     * @since 1.5.0
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Class clazz, final String key, final Function mappingFunction) {
        final Cache<String, Object> cache = typeMap.get(clazz);
        return (cache == null) ? null : (T) cache.get(key, mappingFunction);
    }

    /**
     * Adds an object to cache.
     * @param key the unique identifier of the object to put into cache
     * @param object the object to put into cache.
     * @since 1.5.0
     */
    public void put(final String key, final Object object) {
        Cache<String, Object> cache = typeMap.get(object.getClass());
        if (cache == null) {
            cache = buildCache();
            typeMap.put(object.getClass(), cache);
        }
        cache.put(key, object);
    }

    /**
     * Remove an object from cache.
     * @param clazz the class of the object to remove from cache
     * @param key the unique identifier of the object to remove from cache
     */
    public void remove(final Class clazz, final String key) {
        Cache<String, Object> cache = typeMap.get(clazz);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * Performs maintenance on the cache. Maintenance is automatically carried out
     * and use of this method is normally not required. However, if maintenance
     * must be performed immediately, use of this method may be called.
     * @param clazz the class of the object to perform maintenance on
     * @since 1.5.0
     */
    public void maintenance(Class clazz) {
        typeMap.get(clazz).cleanUp();
    }

    /**
     * Builds implementation-specific cache.
     * @return a Cache object
     */
    private Cache<String, Object> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expiresAfter, timeUnit)
                .maximumSize(maxSize)
                .build();
    }
}