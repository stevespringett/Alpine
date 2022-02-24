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

import alpine.common.util.VersionComparator;
import alpine.persistence.AlpineQueryManager;

import java.sql.Connection;

/**
 * A base abstract UpgradeItem that all UpgradeItem's should extend (for convenience).
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public abstract class AbstractUpgradeItem implements UpgradeItem {

    public boolean shouldUpgrade(final AlpineQueryManager queryManager, final Connection connection) {
        final UpgradeMetaProcessor installedUpgrades = new UpgradeMetaProcessor(connection);
        final VersionComparator currentVersion = installedUpgrades.getSchemaVersion();

        // This should not happen, but if it does, something bad has already happened. do not proceed.
        if (currentVersion == null) {
            return false;
        }

        final VersionComparator version = new VersionComparator(this.getSchemaVersion());
        return version.isNewerThan(currentVersion);
    }

}