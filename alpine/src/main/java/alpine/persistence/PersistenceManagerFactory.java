/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.persistence;

import alpine.Config;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Properties;

/**
 * Initializes the JDO persistence manager on server startup.
 *
 * @since 1.0.0
 */
public class PersistenceManagerFactory implements ServletContextListener {

    // The following properties are used for unit tests
    private static final Properties jdoOverrides;
    static {
        jdoOverrides = new Properties();
        jdoOverrides.put("javax.jdo.option.ConnectionURL", "jdbc:h2:mem:alpine");
        jdoOverrides.put("javax.jdo.option.ConnectionDriverName", "org.h2.Driver");
        jdoOverrides.put("javax.jdo.option.ConnectionUserName", "sa");
        jdoOverrides.put("javax.jdo.option.ConnectionPassword", "");
        jdoOverrides.put("javax.jdo.option.Mapping", "h2");
        jdoOverrides.put("datanucleus.connectionPoolingType", "DBCP");
        jdoOverrides.put("datanucleus.schema.autoCreateSchema", "true");
        jdoOverrides.put("datanucleus.schema.autoCreateTables", "true");
        jdoOverrides.put("datanucleus.schema.autoCreateColumns", "true");
        jdoOverrides.put("datanucleus.schema.autoCreateConstraints", "true");
        jdoOverrides.put("datanucleus.query.jdoql.allowAll", "true");
        jdoOverrides.put("datanucleus.NontransactionalRead", "true");
        jdoOverrides.put("datanucleus.NontransactionalWrite", "true");
    }

    private static javax.jdo.PersistenceManagerFactory pmf;

    public void contextInitialized(ServletContextEvent event) {
        pmf = JDOHelper.getPersistenceManagerFactory("Alpine");
    }

    public void contextDestroyed(ServletContextEvent event) {
        pmf.close();
    }

    public static PersistenceManager createPersistenceManager() {
        if (Config.isUnitTestsEnabled()) {
            pmf = JDOHelper.getPersistenceManagerFactory(jdoOverrides, "Alpine");
        }
        if (pmf == null) {
            throw new IllegalStateException("Context is not initialized yet.");
        }
        return pmf.getPersistenceManager();
    }

}