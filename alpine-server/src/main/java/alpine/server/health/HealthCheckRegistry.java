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
package alpine.server.health;

import org.eclipse.microprofile.health.HealthCheck;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A global registry for {@link HealthCheck}s.
 * <p>
 * Used by {@link alpine.server.servlets.HealthServlet} to lookup registered checks.
 *
 * @since 2.3.0
 */
public class HealthCheckRegistry {

    private static final HealthCheckRegistry INSTANCE = new HealthCheckRegistry();

    private final Map<String, HealthCheck> checks;

    public HealthCheckRegistry() {
        checks = new ConcurrentHashMap<>();
    }

    public static HealthCheckRegistry getInstance() {
        return INSTANCE;
    }

    public Map<String, HealthCheck> getChecks() {
        return Collections.unmodifiableMap(checks);
    }

    public void register(final String name, final HealthCheck check) {
        checks.put(name, check);
    }

}
