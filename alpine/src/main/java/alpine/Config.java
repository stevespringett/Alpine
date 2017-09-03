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
package alpine;

import alpine.logging.Logger;
import alpine.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Config class is responsible for reading the application.properties file.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class);
    private static final String ALPINE_APP_PROP = "alpine.application.properties";
    private static final String PROP_FILE = "application.properties";
    private static Config instance;
    private static Properties properties;

    public interface Key {

        /**
         * The name of the property.
         * @return String of the property name
         */
        String getPropertyName();

        /**
         * The default value of the property if not found.
         * @return the default value
         */
        Object getDefaultValue();
    }

    public enum AlpineKey implements Key {
        APPLICATION_NAME         ("application.name",                 "Unknown Alpine Application"),
        APPLICATION_VERSION      ("application.version",              "0.0.0"),
        APPLICATION_TIMESTAMP    ("application.timestamp",            "1970-01-01 00:00:00"),
        WORKER_THREADS           ("alpine.worker.threads",            0),
        WORKER_THREAD_MULTIPLIER ("alpine.worker.thread.multiplier",  4),
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

        private String propertyName;
        private Object defaultValue;
        AlpineKey(String item, Object defaultValue) {
            this.propertyName = item;
            this.defaultValue = defaultValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    /**
     * Returns an instance of the Config object.
     * @return a Config object
     * @since 1.0.0
     */
    public static Config getInstance() {
        if (instance == null) {
            LOGGER.info(StringUtils.repeat("-", 80));
            instance = new Config();
            instance.init();
            LOGGER.info(StringUtils.repeat("-", 80));
            LOGGER.info("Application: " + instance.getProperty(AlpineKey.APPLICATION_NAME));
            LOGGER.info("Version:     " + instance.getProperty(AlpineKey.APPLICATION_VERSION));
            LOGGER.info("Built-on:    " + instance.getProperty(AlpineKey.APPLICATION_TIMESTAMP));
            LOGGER.info(StringUtils.repeat("-", 80));
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

        LOGGER.info("Initializing Configuration");
        properties = new Properties();

        final String alpineAppProp = System.getProperty(ALPINE_APP_PROP);
        if (StringUtils.isNotBlank(alpineAppProp)) {
            LOGGER.info("Loading application properties from " + alpineAppProp);
            try {
                properties.load(new FileInputStream(new File(alpineAppProp)));
            } catch (FileNotFoundException e) {
                LOGGER.error("Could not find property file " + alpineAppProp);
            } catch (IOException e) {
                LOGGER.error("Unable to load " + alpineAppProp);
            }
        } else {
            LOGGER.info("System property " + ALPINE_APP_PROP + " not specified. Defaulting to load " + PROP_FILE + " from classpath");
            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(PROP_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.error("Unable to load " + PROP_FILE);
            }
        }
    }

    /**
     * Returns the fully qualified path to the configured data directory.
     * Expects a fully qualified path or a path starting with ~/
     *
     * Defaults to ~/.alpine if data directory is not specified.
     * @return a File object of the data directory
     * @since 1.0.0
     */
    public File getDataDirectorty() {
        String prop = getProperty(AlpineKey.DATA_DIRECTORY);
        if (prop.startsWith("~" + File.separator)) {
            prop = SystemUtil.getUserHome() + prop.substring(1);
        }
        return new File(prop).getAbsoluteFile();
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.0.0
     */
    public String getProperty(Key key) {
        if (key.getDefaultValue() == null) {
            return properties.getProperty(key.getPropertyName());
        } else {
            return properties.getProperty(key.getPropertyName(), String.valueOf(key.getDefaultValue()));
        }
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a int of the value of the configuration
     * @since 1.0.0
     */
    public int getPropertyAsInt(Key key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            LOGGER.error("Error parsing number from property: " + key.getPropertyName());
            return -1;
        }
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a long of the value of the configuration
     * @since 1.0.0
     */
    public long getPropertyAsLong(Key key) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (NumberFormatException e) {
            LOGGER.error("Error parsing number from property: " + key.getPropertyName());
            return -1;
        }
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a boolean of the value of the configuration
     * @since 1.0.0
     */
    public boolean getPropertyAsBoolean(Key key) {
        return "true".equalsIgnoreCase(getProperty(key));
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.0.0
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The String of the key to return the configuration for
     * @param defaultValue The default value if the key cannot be found
     * @return a String of the value of the configuration
     * @since 1.0.0
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Determins is unit tests are enabled by checking if the 'alpine.unittests.enabled'
     * system property is set to true or false.
     * @return true if unit tests are enabled, false if not
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
