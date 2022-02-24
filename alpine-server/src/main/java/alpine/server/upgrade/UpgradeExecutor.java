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
package alpine.server.upgrade;

import alpine.common.logging.Logger;
import alpine.common.util.VersionComparator;
import alpine.persistence.AlpineQueryManager;
import alpine.server.util.DbUtil;

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
@SuppressWarnings("unused")
public class UpgradeExecutor {

    private final static Logger LOGGER = Logger.getLogger(UpgradeExecutor.class);

    private final AlpineQueryManager qm;

    /**
     * Constructs a new UpgradeExecutor object.
     *
     * @param qm an AlpineQueryManager (or superclass) object
     * @since 1.2.0
     */
    public UpgradeExecutor(final AlpineQueryManager qm) {
        this.qm = qm;
    }

    /**
     * Performs the execution of upgrades in the order defined by the specified array.
     *
     * @param classes the upgrade classes
     * @throws UpgradeException if errors are encountered
     * @since 1.2.0
     */
    public void executeUpgrades(final List<Class<? extends UpgradeItem>> classes) throws UpgradeException {
        final Connection connection = getConnection(qm);
        final UpgradeMetaProcessor installedUpgrades = new UpgradeMetaProcessor(connection);
        DbUtil.initPlatformName(connection); // Initialize DbUtil

        // First, we need to ensure the schema table is populated on a clean install
        // But we do so without passing any version.
        try {
            installedUpgrades.updateSchemaVersion(null);
        } catch (SQLException e) {
            LOGGER.error("Failed to update schema version", e);
            return;
        }

        for (final Class<? extends UpgradeItem> upgradeClass : classes) {
            try {
                @SuppressWarnings("unchecked")
                final Constructor constructor = upgradeClass.getConstructor();
                final UpgradeItem upgradeItem = (UpgradeItem) constructor.newInstance();

                if (upgradeItem.shouldUpgrade(qm, connection)) {
                    if (!installedUpgrades.hasUpgradeRan(upgradeClass)) {
                        LOGGER.info("Upgrade class " + upgradeClass.getName() + " about to run.");
                        final long startTime = System.currentTimeMillis();
                        upgradeItem.executeUpgrade(qm, connection);
                        final long endTime = System.currentTimeMillis();
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

    /**
     * This connection should never be closed.
     */
    private Connection getConnection(AlpineQueryManager aqm) {
        final JDOConnection jdoConnection = aqm.getPersistenceManager().getDataStoreConnection();
        if (jdoConnection != null) {
            if (jdoConnection.getNativeConnection() instanceof Connection) {
                return (Connection)jdoConnection.getNativeConnection();
            }
        }
        return null;
    }
}