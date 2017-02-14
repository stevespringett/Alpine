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
package alpine;

import alpine.logging.Logger;
import alpine.util.SystemUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Config class is responsible for reading the application.properties file
 *
 * @since 1.0.0
 */
public class Config {

    private static final Logger logger = Logger.getLogger(Config.class);
    private static final String propFile = "application.properties";
    private static Config instance;
    private static Properties properties;

    public enum Key {
        APPLICATION_NAME         ("application.name",                 "Unknown Alpine Application"),
        APPLICATION_VERSION      ("application.version",              "0.0.0"),
        APPLICATION_TIMESTAMP    ("application.timestamp",            "1970-01-01 00:00:00"),
        EVENT_THREADS            ("alpine.event.threads",             4),
        DATA_DIRECTORY           ("alpine.data.directory",            "~/.alpine"),
        DATABASE_MODE            ("alpine.database.mode",             "embedded"),
        DATABASE_PORT            ("alpine.database.port",             9092),
        ENFORCE_AUTHENTICATION   ("alpine.enforce.authentication",    true),
        ENFORCE_AUTHORIZATION    ("alpine.enforce.authorization",     true),
        BCRYPT_ROUNDS            ("alpine.bcrypt.rounds",             14),
        LDAP_ENABLED             ("alpine.ldap.enabled",              false),
        LDAP_SERVER_URL          ("alpine.ldap.server.url",           null),
        LDAP_DOMAIN              ("alpine.ldap.domain",               null),
        LDAP_BASEDN              ("alpine.ldap.basedn",               null),
        LDAP_BIND_USERNAME       ("alpine.ldap.bind.username",        null),
        LDAP_BIND_PASSWORD       ("alpine.ldap.bind.password",        null),
        LDAP_ATTRIBUTE_MAIL      ("alpine.ldap.attribute.mail",       "mail"),
        HTTP_PROXY_ADDRESS       ("alpine.http.proxy.address",        null),
        HTTP_PROXY_PORT          ("alpine.http.proxy.port",           null),
        WATCHDOG_LOGGING_INTERVAL("alpine.watchdog.logging.interval", 0);

        String propertyName;
        Object defaultValue;
        private Key(String item, Object defaultValue) {
            this.propertyName = item;
            this.defaultValue = defaultValue;
        }
    }

    /**
     * Returns an instance of the Config object
     * @return a Config object
     * @since 1.0.0
     */
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        if (properties == null) {
            instance.init();
        }
        return instance;
    }

    /**
     * Initialize the Config object. This method should only be called once.
     */
    private void init() {
        if (properties != null) {
            return;
        }

        logger.info("Initializing Configuration");
        properties = new Properties();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(in);
        } catch (IOException e) {
            logger.error("Unable to load " + propFile);
        }
    }

    /**
     * Returns the fully qualified path to the configured data directory.
     * Expects a fully qualified path or a path starting with ~/
     *
     * Defaults to ~/.alpine if data directory is not specified.
     *
     * @since 1.0.0
     */
    public File getDataDirectorty() {
        String prop = getProperty(Key.DATA_DIRECTORY);
        if (prop.startsWith("~" + File.separator)) {
            prop = SystemUtil.getUserHome() + prop.substring(1);
        }
        return new File(prop).getAbsoluteFile();
    }

    /**
     * Return the configured value for the specified Key
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.0.0
     */
    public String getProperty(Key key) {
        if (key.defaultValue == null) {
            return properties.getProperty(key.propertyName);
        } else {
            return properties.getProperty(key.propertyName, String.valueOf(key.defaultValue));
        }
    }

    /**
     * @since 1.0.0
     */
    public int getPropertyAsInt(Key key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            logger.error("Error parsing number from property: " + key.name());
            return -1;
        }
    }

    /**
     * @since 1.0.0
     */
    public long getPropertyAsLong(Key key) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (NumberFormatException e) {
            logger.error("Error parsing number from property: " + key.name());
            return -1;
        }
    }

    /**
     * @since 1.0.0
     */
    public boolean getPropertyAsBoolean(Key key) {
        return "true".equalsIgnoreCase(getProperty(key));
    }

    /**
     * @since 1.0.0
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Determins is unit tests are enabled by checking if the 'alpine.unittests.enabled'
     * system property is set to true or false.
     * @since 1.0.0
     */
    public static boolean isUnitTestsEnabled() {
        return Boolean.valueOf(System.getProperty("alpine.unittests.enabled", "false"));
    }

    /**
     * Enables unit tests by setting 'alpine.unittests.enabled' system property to true.
     * @since 1.0.0
     */
    public static void enableUnitTests() {
        System.setProperty("alpine.unittests.enabled", "true");
    }

}