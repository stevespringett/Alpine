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
package alpine.test.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory {@link ConfigSource} intended for usage in tests via {@link ConfigPropertyRule}.
 *
 * @since 3.2.0
 */
class InMemoryConfigSource implements ConfigSource {

    private static final Map<String, String> PROPERTIES = new ConcurrentHashMap<>();

    static void setProperties(final Map<String, String> properties) {
        PROPERTIES.putAll(properties);
    }

    static void setProperty(final String key, final String value) {
        PROPERTIES.put(key, value);
    }

    static void clear() {
        PROPERTIES.clear();
    }

    @Override
    public int getOrdinal() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return PROPERTIES.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return PROPERTIES.get(propertyName);
    }

    @Override
    public String getName() {
        return InMemoryConfigSource.class.getSimpleName();
    }

}
