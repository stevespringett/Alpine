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

    private static JDOPersistenceManagerFactory pmf;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Initializing persistence framework");

        final String driverPath = Config.getInstance().getProperty(Config.AlpineKey.DATABASE_DRIVER_PATH);
        if (driverPath != null) {
            Config.getInstance().expandClasspath(driverPath);
        }

        pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JdoProperties.get(), "Alpine");

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
            pmf = (JDOPersistenceManagerFactory)JDOHelper.getPersistenceManagerFactory(JdoProperties.unit(), "Alpine");
        }
        if (pmf == null) {
            throw new IllegalStateException("Context is not initialized yet.");
        }
        return pmf.getPersistenceManager();
    }

}
