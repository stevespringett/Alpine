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
package alpine.server.metrics;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.common.metrics.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * @since 2.1.0
 */
public class MetricsInitializer implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(MetricsInitializer.class);

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            LOGGER.info("Registering system metrics");
            new ClassLoaderMetrics().bindTo(Metrics.getRegistry());
            new DiskSpaceMetrics(Config.getInstance().getDataDirectorty()).bindTo(Metrics.getRegistry());
            new JvmGcMetrics().bindTo(Metrics.getRegistry());
            new JvmInfoMetrics().bindTo(Metrics.getRegistry());
            new JvmMemoryMetrics().bindTo(Metrics.getRegistry());
            new JvmThreadMetrics().bindTo(Metrics.getRegistry());
            new ProcessorMetrics().bindTo(Metrics.getRegistry());
            new UptimeMetrics().bindTo(Metrics.getRegistry());
        }
    }

}
