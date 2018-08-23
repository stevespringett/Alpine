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
package alpine.model;

import alpine.Config;
import javax.inject.Singleton;
import java.io.Serializable;

/**
 * A value object describing the name of the application, version, and the timestamp when it was built.
 * This class can be used as-is, or extended. The {@link alpine.resources.VersionResource} uses this
 * class in it's JSON response.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@Singleton
public class About implements Serializable {

    private static final long serialVersionUID = -7573425245706188307L;

    private static final String APPLICATION = Config.getInstance().getApplicationName();
    private static final String VERSION = Config.getInstance().getApplicationVersion();
    private static final String TIMESTAMP = Config.getInstance().getApplicationBuildTimestamp();
    private static final String UUID = Config.getInstance().getApplicationBuildUuid();

    private static final String FRAMEWORK_NAME = Config.getInstance().getFrameworkName();
    private static final String FRAMEWORK_VERSION = Config.getInstance().getFrameworkVersion();
    private static final String FRAMEWORK_TIMESTAMP = Config.getInstance().getFrameworkBuildTimestamp();
    private static final String FRAMEWORK_UUID = Config.getInstance().getFrameworkBuildUuid();


    public String getApplication() {
        return APPLICATION;
    }

    public String getVersion() {
        return VERSION;
    }

    public String getTimestamp() {
        return TIMESTAMP;
    }

    public String getUuid() {
        return UUID;
    }

    public Framework getFramework() {
        return new Framework();
    }

    private static class Framework {

        public String getName() {
            return FRAMEWORK_NAME;
        }

        public String getVersion() {
            return FRAMEWORK_VERSION;
        }

        public String getTimestamp() {
            return FRAMEWORK_TIMESTAMP;
        }

        public String getUuid() {
            return FRAMEWORK_UUID;
        }
    }

}
