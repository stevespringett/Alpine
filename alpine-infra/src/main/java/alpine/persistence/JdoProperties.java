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
package alpine.persistence;

import alpine.Config;
import org.datanucleus.PropertyNames;
import org.datanucleus.store.rdbms.RDBMSPropertyNames;
import java.util.Properties;

/**
 * This class provides runtime constants for JDO properties.
 *
 * @since 1.4.3
 */
public final class JdoProperties {

    private JdoProperties() { }

    /**
     * @return The pre-populated {@link Properties} for {@link javax.jdo.PersistenceManagerFactory} creation
     * @deprecated Assemble DataNucleus properties in context of {@link javax.jdo.PersistenceManagerFactory} creation instead
     */
    @Deprecated(forRemoval = true)
    public static Properties get() {
        final Properties properties = new Properties();
        properties.put("javax.jdo.option.ConnectionURL", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL));
        properties.put("javax.jdo.option.ConnectionDriverName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER));
        properties.put("javax.jdo.option.ConnectionUserName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME));
        properties.put("javax.jdo.option.ConnectionPassword", Config.getInstance().getPropertyOrFile(Config.AlpineKey.DATABASE_PASSWORD));
        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_POOL_ENABLED)) {
            properties.put(PropertyNames.PROPERTY_CONNECTION_POOLINGTYPE, "HikariCP");
            properties.put(RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MAX_POOL_SIZE, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MAX_SIZE));
            properties.put(RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_IDLE_TIMEOUT, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_IDLE_TIMEOUT));
            properties.put(RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MAX_LIFETIME, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MAX_LIFETIME));
            properties.put(RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MIN_IDLE, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MIN_IDLE));
        }
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_DATABASE, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, "create");
        properties.put(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL, "true");
        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED)) {
            properties.put(PropertyNames.PROPERTY_ENABLE_STATISTICS, "true");
        }
        return properties;
    }

    public static Properties unit() {
        final Properties properties = new Properties();
        properties.put("javax.jdo.option.PersistenceUnitName", "Alpine");
        properties.put("javax.jdo.option.ConnectionURL", "jdbc:h2:mem:alpine");
        properties.put("javax.jdo.option.ConnectionDriverName", "org.h2.Driver");
        properties.put("javax.jdo.option.ConnectionUserName", "sa");
        properties.put("javax.jdo.option.ConnectionPassword", "");
        properties.put("javax.jdo.option.Mapping", "h2");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_DATABASE, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, "true");
        properties.put(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, "create");
        properties.put(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL, "true");
        properties.put(PropertyNames.PROPERTY_RETAIN_VALUES, "true");
        properties.putAll(Config.getInstance().getPassThroughProperties("datanucleus"));
        return properties;
    }
}
