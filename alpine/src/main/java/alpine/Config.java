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
import alpine.util.PathUtil;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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
    private static final String ALPINE_VERSION_PROP_FILE = "alpine.version";
    private static final String APPLICATION_VERSION_PROP_FILE = "application.version";
    private static final Config INSTANCE;
    private static Properties properties;
    private static Properties alpineVersionProperties;
    private static Properties applicationVersionProperties;

    static {
        LOGGER.info(StringUtils.repeat("-", 80));
        INSTANCE = new Config();
        INSTANCE.init();
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("Application: " + INSTANCE.getApplicationName());
        LOGGER.info("Version:     " + INSTANCE.getApplicationVersion());
        LOGGER.info("Built-on:    " + INSTANCE.getApplicationBuildTimestamp());
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("Framework:   " + INSTANCE.getFrameworkName());
        LOGGER.info("Version :    " + INSTANCE.getFrameworkVersion());
        LOGGER.info("Built-on:    " + INSTANCE.getFrameworkBuildTimestamp());
        LOGGER.info(StringUtils.repeat("-", 80));
    }

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
        WORKER_THREADS           ("alpine.worker.threads",            0),
        WORKER_THREAD_MULTIPLIER ("alpine.worker.thread.multiplier",  4),
        DATA_DIRECTORY           ("alpine.data.directory",            "~/.alpine"),
        DATABASE_MODE            ("alpine.database.mode",             "embedded"),
        DATABASE_PORT            ("alpine.database.port",             9092),
        DATABASE_URL             ("alpine.database.url",              "jdbc:h2:mem:alpine"),
        DATABASE_DRIVER          ("alpine.database.driver",           "org.h2.Driver"),
        DATABASE_DRIVER_PATH     ("alpine.database.driver.path",      null),
        DATABASE_USERNAME        ("alpine.database.username",         "sa"),
        DATABASE_PASSWORD        ("alpine.database.password",         ""),
        ENFORCE_AUTHENTICATION   ("alpine.enforce.authentication",    true),
        ENFORCE_AUTHORIZATION    ("alpine.enforce.authorization",     true),
        BCRYPT_ROUNDS            ("alpine.bcrypt.rounds",             14),
        LDAP_ENABLED             ("alpine.ldap.enabled",              false),
        LDAP_SERVER_URL          ("alpine.ldap.server.url",           null),
        LDAP_DOMAIN              ("alpine.ldap.domain",               null),
        LDAP_BASEDN              ("alpine.ldap.basedn",               null),
        LDAP_SECURITY_AUTH       ("alpine.ldap.security.auth",        null),
        LDAP_BIND_USERNAME       ("alpine.ldap.bind.username",        null),
        LDAP_BIND_PASSWORD       ("alpine.ldap.bind.password",        null),
        LDAP_AUTH_USERNAME_FMT   ("alpine.ldap.auth.username.format", null),
        LDAP_ATTRIBUTE_NAME      ("alpine.ldap.attribute.name",       "userPrincipalName"),
        LDAP_ATTRIBUTE_MAIL      ("alpine.ldap.attribute.mail",       "mail"),
        HTTP_PROXY_ADDRESS       ("alpine.http.proxy.address",        null),
        HTTP_PROXY_PORT          ("alpine.http.proxy.port",           null),
        HTTP_PROXY_USERNAME      ("alpine.http.proxy.username",       null),
        HTTP_PROXY_PASSWORD      ("alpine.http.proxy.password",       null),
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
        return INSTANCE;
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

        final String alpineAppProp = PathUtil.resolve(System.getProperty(ALPINE_APP_PROP));
        if (StringUtils.isNotBlank(alpineAppProp)) {
            LOGGER.info("Loading application properties from " + alpineAppProp);
            try (FileInputStream fileInputStream = new FileInputStream(new File(alpineAppProp))) {
                properties.load(fileInputStream);
            } catch (FileNotFoundException e) {
                LOGGER.error("Could not find property file " + alpineAppProp);
            } catch (IOException e) {
                LOGGER.error("Unable to load " + alpineAppProp);
            }
        } else {
            LOGGER.info("System property " + ALPINE_APP_PROP + " not specified");
            LOGGER.info("Loading " + PROP_FILE + " from classpath");
            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(PROP_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.error("Unable to load " + PROP_FILE);
            }
        }
        if (properties.size() == 0) {
            LOGGER.error("A fatal error occurred loading application properties. Please correct the issue and restart the application.");
        }

        alpineVersionProperties = new Properties();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(ALPINE_VERSION_PROP_FILE)) {
            alpineVersionProperties.load(in);
        } catch (IOException e) {
            LOGGER.error("Unable to load " + ALPINE_VERSION_PROP_FILE);
        }
        if (alpineVersionProperties.size() == 0) {
            LOGGER.error("A fatal error occurred loading Alpine version information. Please correct the issue and restart the application.");
        }

        applicationVersionProperties = new Properties();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(APPLICATION_VERSION_PROP_FILE)) {
            applicationVersionProperties.load(in);
        } catch (IOException e) {
            LOGGER.error("Unable to load " + APPLICATION_VERSION_PROP_FILE);
        }
        if (applicationVersionProperties.size() == 0) {
            LOGGER.error("A fatal error occurred loading application version information. Please correct the issue and restart the application.");
        }
    }

    /**
     * Returns the Alpine component name.
     * @return the Alpine name
     * @since 1.0.0
     */
    public String getFrameworkName() {
        return alpineVersionProperties.getProperty("name");
    }

    /**
     * Returns the Alpine version.
     * @return the Alpine version
     * @since 1.0.0
     */
    public String getFrameworkVersion() {
        return alpineVersionProperties.getProperty("version");
    }

    /**
     * Returns the Alpine built timestamp.
     * @return the timestamp in which this version of Alpine was built
     * @since 1.0.0
     */
    public String getFrameworkBuildTimestamp() {
        return alpineVersionProperties.getProperty("timestamp");
    }

    /**
     * Returns the Alpine UUID.
     * @return the UUID unique to this build of Alpine
     * @since 1.3.0
     */
    public String getFrameworkBuildUuid() {
        return alpineVersionProperties.getProperty("uuid");
    }

    /**
     * Returns the Application component name.
     * @return the Application name
     * @since 1.0.0
     */
    public String getApplicationName() {
        return applicationVersionProperties.getProperty("name", "Unknown Alpine Application");
    }

    /**
     * Returns the Application version.
     * @return the Application version
     * @since 1.0.0
     */
    public String getApplicationVersion() {
        return applicationVersionProperties.getProperty("version", "0.0.0");
    }

    /**
     * Returns the Application built timestamp.
     * @return the timestamp in which this version of the Application was built
     * @since 1.0.0
     */
    public String getApplicationBuildTimestamp() {
        return applicationVersionProperties.getProperty("timestamp", "1970-01-01 00:00:00");
    }

    /**
     * Returns the Application UUID.
     * @return the UUID unique to this build of the application
     * @since 1.3.0
     */
    public String getApplicationBuildUuid() {
        return applicationVersionProperties.getProperty("uuid");
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
        final String prop = PathUtil.resolve(getProperty(AlpineKey.DATA_DIRECTORY));
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
     * Extends the runtime classpath to include the files or directories specified.
     * @param paths one or more strings representing a single JAR file or a directory containing JARs.
     * @since 1.0.0
     */
    public void expandClasspath(String... paths) {
        if (paths == null || paths.length == 0) {
            return;
        }
        for (String path: paths) {
            expandClasspath(new File(PathUtil.resolve(path)));
        }
    }

    /**
     * Extends the runtime classpath to include the files or directories specified.
     * @param files one or more File objects representing a single JAR file or a directory containing JARs.
     * @since 1.0.0
     */
    public void expandClasspath(File... files) {
        URLClassLoader urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        for (File file: files) {
            LOGGER.info("Expanding classpath to include: " + file.getAbsolutePath());
            URI fileUri = file.toURI();
            try {
                Method method = urlClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlClassLoader, fileUri.toURL());
            } catch (MalformedURLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Error expanding classpath", e);
            }
        }
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
