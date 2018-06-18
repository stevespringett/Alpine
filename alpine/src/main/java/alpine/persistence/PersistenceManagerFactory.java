/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.persistence;

import alpine.Config;
import alpine.logging.Logger;
import alpine.model.InstalledUpgrades;
import alpine.model.SchemaVersion;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Initializes the JDO persistence manager on server startup.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class PersistenceManagerFactory implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(PersistenceManagerFactory.class);

    // The following properties are used for unit tests
    private static final Properties JDO_OVERRIDES;
    static {
        JDO_OVERRIDES = new Properties();
        JDO_OVERRIDES.put("javax.jdo.option.ConnectionURL", "jdbc:h2:mem:alpine");
        JDO_OVERRIDES.put("javax.jdo.option.ConnectionDriverName", "org.h2.Driver");
        JDO_OVERRIDES.put("javax.jdo.option.ConnectionUserName", "sa");
        JDO_OVERRIDES.put("javax.jdo.option.ConnectionPassword", "");
        JDO_OVERRIDES.put("javax.jdo.option.Mapping", "h2");
        JDO_OVERRIDES.put("datanucleus.connectionPoolingType", "HikariCP");
        JDO_OVERRIDES.put("datanucleus.schema.autoCreateDatabase", "true");
        JDO_OVERRIDES.put("datanucleus.schema.autoCreateTables", "true");
        JDO_OVERRIDES.put("datanucleus.schema.autoCreateColumns", "true");
        JDO_OVERRIDES.put("datanucleus.schema.autoCreateConstraints", "true");
        JDO_OVERRIDES.put("datanucleus.generateSchema.database.mode", "create");
        JDO_OVERRIDES.put("datanucleus.query.jdoql.allowAll", "true");
    }

    private static final Properties JDO_PROPERTIES;
    static {
        JDO_PROPERTIES = new Properties();
        JDO_PROPERTIES.put("javax.jdo.option.ConnectionURL", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_URL));
        JDO_PROPERTIES.put("javax.jdo.option.ConnectionDriverName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER));
        JDO_PROPERTIES.put("javax.jdo.option.ConnectionUserName", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_USERNAME));
        JDO_PROPERTIES.put("javax.jdo.option.ConnectionPassword", Config.getInstance().getProperty(Config.AlpineKey.DATABASE_PASSWORD));
        JDO_PROPERTIES.put("datanucleus.connectionPoolingType", "HikariCP");
        JDO_PROPERTIES.put("datanucleus.schema.autoCreateDatabase", "true");
        JDO_PROPERTIES.put("datanucleus.schema.autoCreateTables", "true");
        JDO_PROPERTIES.put("datanucleus.schema.autoCreateColumns", "true");
        JDO_PROPERTIES.put("datanucleus.schema.autoCreateConstraints", "true");
        JDO_PROPERTIES.put("datanucleus.generateSchema.database.mode", "create");
        JDO_PROPERTIES.put("datanucleus.query.jdoql.allowAll", "true");
    }

    private static JDOPersistenceManagerFactory pmf;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Initializing persistence framework");

        final String driverPath = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER_PATH);
        if (driverPath != null) {
            Config.getInstance().expandClasspath(driverPath);
        }

        pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JDO_PROPERTIES, "Alpine");

        // Ensure that the UpgradeMetaProcessor and SchemaVersion tables are created NOW, not dynamically at runtime.
        final PersistenceNucleusContext ctx = pmf.getNucleusContext();
        final Set<String> classNames = new HashSet<>();
        classNames.add(InstalledUpgrades.class.getCanonicalName());
        classNames.add(SchemaVersion.class.getCanonicalName());
        ((SchemaAwareStoreManager)ctx.getStoreManager()).createSchemaForClasses(classNames, new Properties());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOGGER.info("Shutting down persistence framework");
        pmf.close();
    }

    /**
     * Creates a new JDO PersistenceManager.
     * @return a PersistenceManager
     */
    public static PersistenceManager createPersistenceManager() {
        if (Config.isUnitTestsEnabled()) {
            pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JDO_OVERRIDES, "Alpine");
        }
        if (pmf == null) {
            throw new IllegalStateException("Context is not initialized yet.");
        }
        return pmf.getPersistenceManager();
    }

}
