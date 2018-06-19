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
package alpine.upgrade;

import alpine.logging.Logger;
import alpine.persistence.AlpineQueryManager;
import alpine.util.DbUtil;
import alpine.util.VersionComparator;
import javax.jdo.datastore.JDOConnection;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Executes upgrades by first checking if execution is necessary and if the specific upgrade
 * has already been executed. If an upgrade is necessary and the specific upgrade being requested
 * has not previously executed, the upgrade will be processed and the schema version will be upgraded.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public class UpgradeExecutor {

    private final static Logger LOGGER = Logger.getLogger(UpgradeExecutor.class);

    private AlpineQueryManager qm;
    private Connection connection;

    /**
     * Constructs a new UpgradeExecutor object.
     *
     * @param qm an AlpineQueryManager (or superclass) object
     * @since 1.2.0
     */
    public UpgradeExecutor(AlpineQueryManager qm) {
        this.qm = qm;
        JDOConnection jdoConnection = qm.getPersistenceManager().getDataStoreConnection();
        if (jdoConnection != null) {
            if (jdoConnection.getNativeConnection() instanceof Connection) {
                connection = (Connection)jdoConnection.getNativeConnection();
            }
        }
    }

    /**
     * Performs the execution of upgrades in the order defined by the specified array.
     *
     * @param classes the upgrade classes
     * @throws UpgradeException if errors are encountered
     * @since 1.2.0
     */
    public void executeUpgrades(List<Class<? extends UpgradeItem>> classes) throws UpgradeException {
        UpgradeMetaProcessor installedUpgrades = new UpgradeMetaProcessor(connection);

        DbUtil.initPlatformName(connection); // Initialize DbUtil

        // First, we need to ensure the schema table is populated on a clean install
        // But we do so without passing any version.
        try {
            installedUpgrades.updateSchemaVersion(null);
        } catch (SQLException e) {
            LOGGER.error("Failed to update schema version", e);
            return;
        }

        for (Class<? extends UpgradeItem> upgradeClass : classes) {
            try {
                @SuppressWarnings("unchecked")
                Constructor constructor = upgradeClass.getConstructor();
                UpgradeItem upgradeItem = (UpgradeItem) constructor.newInstance();

                if (upgradeItem.shouldUpgrade(qm, connection)) {
                    if (!installedUpgrades.hasUpgradeRan(upgradeClass)) {
                        LOGGER.info("Upgrade class " + upgradeClass.getName() + " about to run.");
                        long startTime = System.currentTimeMillis();
                        upgradeItem.executeUpgrade(qm, connection);
                        long endTime = System.currentTimeMillis();
                        installedUpgrades.installUpgrade(upgradeClass, startTime, endTime);
                        installedUpgrades.updateSchemaVersion(new VersionComparator(upgradeItem.getSchemaVersion()));
                        LOGGER.info("Completed running upgrade class " + upgradeClass.getName() + " in " + (endTime - startTime) + " ms.");
                    } else {
                        LOGGER.debug("Upgrade class " + upgradeClass.getName() + " has already ran, skipping.");
                    }
                } else {
                    LOGGER.debug("Upgrade class " + upgradeClass.getName() + " does not need to run.");
                }
            } catch (Exception e) {
                DbUtil.rollback(connection);
                LOGGER.error("Error in executing upgrade class: " + upgradeClass.getName(), e);
                throw new UpgradeException(e);
            }
        }
    }

}