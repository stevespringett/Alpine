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
import java.util.Properties;

import static org.datanucleus.PropertyNames.*;
import static org.datanucleus.store.rdbms.RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_IDLE_TIMEOUT;
import static org.datanucleus.store.rdbms.RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MAX_LIFETIME;
import static org.datanucleus.store.rdbms.RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MAX_POOL_SIZE;
import static org.datanucleus.store.rdbms.RDBMSPropertyNames.PROPERTY_CONNECTION_POOL_MIN_IDLE;

/**
 * This class provides runtime constants for JDO properties.
 *
 * @since 1.4.3
 */
public final class JdoProperties {

    private JdoProperties() { }

    public static Properties get() {
        final Properties properties = new Properties();
        properties.put("javax.jdo.option.ConnectionURL", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL));
        properties.put("javax.jdo.option.ConnectionDriverName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER));
        properties.put("javax.jdo.option.ConnectionUserName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME));
        properties.put("javax.jdo.option.ConnectionPassword", Config.getInstance().getPropertyOrFile(Config.AlpineKey.DATABASE_PASSWORD));
        if (Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.DATABASE_POOL_ENABLED)) {
            properties.put(PROPERTY_CONNECTION_POOLINGTYPE, "HikariCP");
            properties.put(PROPERTY_CONNECTION_POOL_MAX_POOL_SIZE, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MAX_SIZE));
            properties.put(PROPERTY_CONNECTION_POOL_IDLE_TIMEOUT, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_IDLE_TIMEOUT));
            properties.put(PROPERTY_CONNECTION_POOL_MAX_LIFETIME, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MAX_LIFETIME));
            properties.put(PROPERTY_CONNECTION_POOL_MIN_IDLE, Config.getInstance().getProperty(Config.AlpineKey.DATABASE_POOL_MIN_IDLE));
        }
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_DATABASE, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_TABLES, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, "true");
        properties.put(PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, "create");
        properties.put(PROPERTY_QUERY_JDOQL_ALLOWALL, "true");
        properties.put(PROPERTY_MULTITHREADED, "true");
        properties.put("javax.jdo.option.Multithreaded", "true");
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
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_DATABASE, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_TABLES, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, "true");
        properties.put(PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, "true");
        properties.put(PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, "create");
        properties.put(PROPERTY_QUERY_JDOQL_ALLOWALL, "true");
        properties.put("javax.jdo.option.Multithreaded", "true");
        properties.put(PROPERTY_MULTITHREADED, "true");
        return properties;
    }
}
