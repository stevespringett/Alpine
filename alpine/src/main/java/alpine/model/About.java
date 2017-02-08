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
package alpine.model;

import alpine.Config;
import java.io.Serializable;

public class About implements Serializable {

    private static final long serialVersionUID = -7573425245706188307L;

    private static final String application = Config.getInstance().getProperty(Config.Key.APPLICATION_NAME);
    private static final String version = Config.getInstance().getProperty(Config.Key.APPLICATION_VERSION);
    private static final String timestamp = Config.getInstance().getProperty(Config.Key.APPLICATION_TIMESTAMP);


    public String getApplication() {
        return application;
    }

    public String getVersion() {
        return version;
    }

    public String getTimestamp() {
        return timestamp;
    }

}