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
package alpine;

import alpine.common.logging.Logger;
import alpine.common.util.ByteFormat;
import alpine.common.util.PathUtil;
import alpine.common.util.SystemUtil;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.Priorities;
import io.smallrye.config.ProfileConfigSourceInterceptor;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.smallrye.config.PropertiesConfigSourceLoader.inFileSystem;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static java.util.function.Predicate.not;

/**
 * The Config class is responsible for reading the application.properties file.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class);
    private static final String ALPINE_APP_PROP = "alpine.application.properties";
    private static final String ALPINE_VERSION_PROP_FILE = "alpine.version";
    private static final String APPLICATION_VERSION_PROP_FILE = "application.version";
    private static final Config INSTANCE;
    private static Properties alpineVersionProperties;
    private static Properties applicationVersionProperties;
    private static String systemId;
    private static org.eclipse.microprofile.config.Config delegateConfig;

    static {
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("OS Name:      " + SystemUtil.getOsName());
        LOGGER.info("OS Version:   " + SystemUtil.getOsVersion());
        LOGGER.info("OS Arch:      " + SystemUtil.getOsArchitecture());
        LOGGER.info("CPU Cores:    " + SystemUtil.getCpuCores());
        LOGGER.info("Max Memory:   " + new ByteFormat().minimumFractionDigits(1).format2(SystemUtil.getMaxMemory()));
        LOGGER.info("Java Vendor:  " + SystemUtil.getJavaVendor());
        LOGGER.info("Java Version: " + SystemUtil.getJavaVersion());
        LOGGER.info("Java Home:    " + SystemUtil.getJavaHome());
        LOGGER.info("Java Temp:    " + SystemUtil.getJavaTempDir());
        LOGGER.info("User:         " + SystemUtil.getUserName());
        LOGGER.info("User Home:    " + SystemUtil.getUserHome());
        LOGGER.info(StringUtils.repeat("-", 80));
        INSTANCE = new Config();
        INSTANCE.init();
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("Application:  " + INSTANCE.getApplicationName());
        LOGGER.info("Version:      " + INSTANCE.getApplicationVersion());
        LOGGER.info("Built-on:     " + INSTANCE.getApplicationBuildTimestamp());
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("Framework:    " + INSTANCE.getFrameworkName());
        LOGGER.info("Version :     " + INSTANCE.getFrameworkVersion());
        LOGGER.info("Built-on:     " + INSTANCE.getFrameworkBuildTimestamp());
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
        WORKER_THREADS                         ("alpine.worker.threads",             0),
        WORKER_THREAD_MULTIPLIER               ("alpine.worker.thread.multiplier",   4),
        DATA_DIRECTORY                         ("alpine.data.directory",             "~/.alpine"),
        SECRET_KEY_PATH                        ("alpine.secret.key.path",            null),
        PRIVATE_KEY_PATH                       ("alpine.private.key.path",           null),
        PUBLIC_KEY_PATH                        ("alpine.public.key.path",            null),
        DATABASE_MODE                          ("alpine.database.mode",              "embedded"),
        DATABASE_PORT                          ("alpine.database.port",              9092),
        DATABASE_URL                           ("alpine.database.url",               "jdbc:h2:mem:alpine"),
        DATABASE_DRIVER                        ("alpine.database.driver",            "org.h2.Driver"),
        DATABASE_USERNAME                      ("alpine.database.username",          "sa"),
        DATABASE_PASSWORD                      ("alpine.database.password",          ""),
        DATABASE_PASSWORD_FILE                 ("alpine.database.password.file",     null),
        DATABASE_MIGRATION_ENABLED             ("alpine.database.migration.enabled", true),
        DATABASE_POOL_ENABLED                  ("alpine.database.pool.enabled",      true),
        DATABASE_POOL_TX_ONLY                  ("alpine.database.pool.tx.only",      false),
        @Deprecated
        DATABASE_POOL_MAX_SIZE                 ("alpine.database.pool.max.size",     20),
        DATABASE_POOL_TX_MAX_SIZE              ("alpine.database.pool.tx.max.size", null),        // null for backward compatibility
        DATABASE_POOL_NONTX_MAX_SIZE           ("alpine.database.pool.nontx.max.size", null),     // null for backward compatibility
        @Deprecated
        DATABASE_POOL_IDLE_TIMEOUT             ("alpine.database.pool.idle.timeout", 300000),
        DATABASE_POOL_TX_IDLE_TIMEOUT          ("alpine.database.pool.tx.idle.timeout", null),    // null for backward compatibility
        DATABASE_POOL_NONTX_IDLE_TIMEOUT       ("alpine.database.pool.nontx.idle.timeout", null), // null for backward compatibility
        @Deprecated
        DATABASE_POOL_MIN_IDLE                 ("alpine.database.pool.min.idle",     10),
        DATABASE_POOL_TX_MIN_IDLE              ("alpine.database.pool.tx.min.idle", null),        // null for backward compatibility
        DATABASE_POOL_NONTX_MIN_IDLE           ("alpine.database.pool.nontx.min.idle", null),     // null for backward compatibility
        @Deprecated
        DATABASE_POOL_MAX_LIFETIME             ("alpine.database.pool.max.lifetime", 600000),
        DATABASE_POOL_TX_MAX_LIFETIME          ("alpine.database.pool.tx.max.lifetime", null),    // null for backward compatibility
        DATABASE_POOL_NONTX_MAX_LIFETIME       ("alpine.database.pool.nontx.max.lifetime", null), // null for backward compatibility
        @Deprecated
        DATABASE_POOL_KEEPALIVE_INTERVAL       ("alpine.database.pool.keepalive.interval", 0),
        DATABASE_POOL_TX_KEEPALIVE_INTERVAL    ("alpine.database.pool.tx.keepalive.interval", null),
        DATABASE_POOL_NONTX_KEEPALIVE_INTERVAL ("alpine.database.pool.nontx.keepalive.interval", null),
        ENFORCE_AUTHENTICATION                 ("alpine.enforce.authentication",     true),
        ENFORCE_AUTHORIZATION                  ("alpine.enforce.authorization",      true),
        BCRYPT_ROUNDS                          ("alpine.bcrypt.rounds",              14),
        LDAP_ENABLED                           ("alpine.ldap.enabled",               false),
        LDAP_SERVER_URL                        ("alpine.ldap.server.url",            null),
        LDAP_BASEDN                            ("alpine.ldap.basedn",                null),
        LDAP_SECURITY_AUTH                     ("alpine.ldap.security.auth",         null),
        LDAP_BIND_USERNAME                     ("alpine.ldap.bind.username",         null),
        LDAP_BIND_PASSWORD                     ("alpine.ldap.bind.password",         null),
        LDAP_BIND_PASSWORD_FILE                ("alpine.ldap.bind.password.file",    null),
        LDAP_AUTH_USERNAME_FMT                 ("alpine.ldap.auth.username.format",  null),
        LDAP_ATTRIBUTE_NAME                    ("alpine.ldap.attribute.name",        "userPrincipalName"),
        LDAP_ATTRIBUTE_MAIL                    ("alpine.ldap.attribute.mail",        "mail"),
        LDAP_GROUPS_FILTER                     ("alpine.ldap.groups.filter",         null),
        LDAP_USER_GROUPS_FILTER                ("alpine.ldap.user.groups.filter",    null),
        LDAP_GROUPS_SEARCH_FILTER              ("alpine.ldap.groups.search.filter",  null),
        LDAP_USERS_SEARCH_FILTER               ("alpine.ldap.users.search.filter",   null),
        LDAP_USER_PROVISIONING                 ("alpine.ldap.user.provisioning",     false),
        LDAP_TEAM_SYNCHRONIZATION              ("alpine.ldap.team.synchronization",  false),
        METRICS_ENABLED                        ("alpine.metrics.enabled",            false),
        METRICS_AUTH_USERNAME                  ("alpine.metrics.auth.username",      null),
        METRICS_AUTH_PASSWORD                  ("alpine.metrics.auth.password",      null),
        OIDC_ENABLED                           ("alpine.oidc.enabled",               false),
        OIDC_ISSUER                            ("alpine.oidc.issuer",                null),
        OIDC_CLIENT_ID                         ("alpine.oidc.client.id",             null),
        OIDC_USERNAME_CLAIM                    ("alpine.oidc.username.claim",       "sub"),
        OIDC_USER_PROVISIONING                 ("alpine.oidc.user.provisioning",    false),
        OIDC_TEAM_SYNCHRONIZATION              ("alpine.oidc.team.synchronization", false),
        OIDC_TEAMS_CLAIM                       ("alpine.oidc.teams.claim",       "groups"),
        OIDC_TEAMS_DEFAULT                     ("alpine.oidc.teams.default",         null),
        HTTP_PROXY_ADDRESS                     ("alpine.http.proxy.address",         null),
        HTTP_PROXY_PORT                        ("alpine.http.proxy.port",            null),
        HTTP_PROXY_USERNAME                    ("alpine.http.proxy.username",        null),
        HTTP_PROXY_PASSWORD                    ("alpine.http.proxy.password",        null),
        HTTP_PROXY_PASSWORD_FILE               ("alpine.http.proxy.password.file",   null),
        NO_PROXY                               ("alpine.no.proxy",                   null),
        HTTP_TIMEOUT_CONNECTION                ("alpine.http.timeout.connection",    30),
        HTTP_TIMEOUT_POOL                      ("alpine.http.timeout.pool",          60),
        HTTP_TIMEOUT_SOCKET                    ("alpine.http.timeout.socket",        30),
        CORS_ENABLED                           ("alpine.cors.enabled",               true),
        CORS_ALLOW_ORIGIN                      ("alpine.cors.allow.origin",          "*"),
        CORS_ALLOW_METHODS                     ("alpine.cors.allow.methods",         "GET, POST, PUT, DELETE, OPTIONS"),
        CORS_ALLOW_HEADERS                     ("alpine.cors.allow.headers",         "Origin, Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin, X-Api-Key, X-Total-Count, *"),
        CORS_EXPOSE_HEADERS                    ("alpine.cors.expose.headers",        "Origin, Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin, X-Api-Key, X-Total-Count"),
        CORS_ALLOW_CREDENTIALS                 ("alpine.cors.allow.credentials",     true),
        CORS_MAX_AGE                           ("alpine.cors.max.age",               3600),
        WATCHDOG_LOGGING_INTERVAL              ("alpine.watchdog.logging.interval",  0),
        API_KEY_PREFIX                         ("alpine.api.key.prefix",             "alpine_"),
        AUTH_JWT_TTL_SECONDS                   ("alpine.auth.jwt.ttl.seconds",       7 * 24 * 60 * 60);


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
    void init() {
        if (delegateConfig != null) {
            return;
        }

        LOGGER.info("Initializing Configuration");
        final SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder()
                .forClassLoader(Thread.currentThread().getContextClassLoader())
                // Enable default config sources:
                //
                // | Source                                               | Priority |
                // | :--------------------------------------------------- | :------- |
                // | System properties                                    | 400      |
                // | Environment variables                                | 300      |
                // | ${pwd}/.env file                                     | 295      |
                // | ${pwd}/config/application.properties                 | 260      |
                // | ${classpath}/application.properties                  | 250      |
                // | ${classpath}/META-INF/microprofile-config.properties | 100      |
                //
                // https://smallrye.io/smallrye-config/3.10.2/config/getting-started/#config-sources
                .addDefaultSources()
                // Support expressions.
                // https://smallrye.io/smallrye-config/3.10.2/config/expressions/
                .withInterceptors(new ExpressionConfigSourceInterceptor())
                // Support profiles.
                // https://smallrye.io/smallrye-config/3.10.2/config/profiles/
                .withInterceptors(new ProfileConfigSourceInterceptor(List.of("prod", "dev", "test")))
                // Relocate SmallRye properties to the Alpine prefix for better framework "immersion".
                // https://smallrye.io/smallrye-config/3.10.2/extensions/relocate/
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {

                    @Override
                    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                        // Properties to be relocated are documented here:
                        // https://smallrye.io/smallrye-config/3.10.2/config/configuration/
                        return new RelocateConfigSourceInterceptor(Map.ofEntries(
                                Map.entry(SMALLRYE_CONFIG_PROFILE, "alpine.config.profile"),
                                Map.entry(SMALLRYE_CONFIG_PROFILE_PARENT, "alpine.config.profile.parent"),
                                Map.entry(SMALLRYE_CONFIG_LOCATIONS, "alpine.config.locations"),
                                Map.entry(SMALLRYE_CONFIG_LOG_VALUES, "alpine.config.log.values"),
                                Map.entry(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "alpine.config.mapping.validate-unknown")));
                    }

                    @Override
                    public OptionalInt getPriority() {
                        // Priority must be higher than that of ProfileConfigSourceInterceptor
                        // in order for profile property relocations to work.
                        return OptionalInt.of(Priorities.LIBRARY + 210);
                    }

                })
                .withDefaultValue("alpine.config.profile", "prod")
                // Allow applications to customize the Config via SPI.
                // https://smallrye.io/smallrye-config/3.10.2/config/customizer/
                .addDiscoveredCustomizers();

        // If a custom properties file is specified via "alpine.application.properties" system property,
        // register it as additional config source. The file has a higher priority than any of the default
        // properties sources.
        final String alpineAppProp = PathUtil.resolve(System.getProperty(ALPINE_APP_PROP));
        if (StringUtils.isNotBlank(alpineAppProp)) {
            configBuilder.withSources(inFileSystem(alpineAppProp, 275, Thread.currentThread().getContextClassLoader()));
        }

        delegateConfig = configBuilder.build();

        alpineVersionProperties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(ALPINE_VERSION_PROP_FILE)) {
            alpineVersionProperties.load(in);
        } catch (IOException e) {
            LOGGER.error("Unable to load " + ALPINE_VERSION_PROP_FILE);
        }
        if (alpineVersionProperties.size() == 0) {
            LOGGER.error("A fatal error occurred loading Alpine version information. Please correct the issue and restart the application.");
        }

        applicationVersionProperties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(APPLICATION_VERSION_PROP_FILE)) {
            if (in != null) {
                applicationVersionProperties.load(in);
            } else {
                LOGGER.error("Unable to load (resourceStream is null) " + APPLICATION_VERSION_PROP_FILE);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to load " + APPLICATION_VERSION_PROP_FILE);
        }
        if (applicationVersionProperties.size() == 0) {
            LOGGER.error("A fatal error occurred loading application version information. Please correct the issue and restart the application.");
        }

        final File dataDirectory = getDataDirectorty();
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                LOGGER.warn("""
                        Data directory %s does not exist, and could not be created. \
                        Please ensure that the user running the JVM has sufficient permissions.\
                        """.formatted(dataDirectory.getAbsolutePath()));
            }
        }
        if (!dataDirectory.canRead()) {
            LOGGER.warn("""
                    Data directory %s is not readable. \
                    Please ensure that the user running the JVM has sufficient permissions.\
                    """.formatted(dataDirectory.getAbsolutePath()));
        }
        if (!dataDirectory.canWrite()) {
            LOGGER.warn("""
                    Data directory %s is not writable. \
                    Please ensure that the user running the JVM has sufficient permissions.\
                    """.formatted(dataDirectory.getAbsolutePath()));
        }

        final File systemIdFile = getSystemIdFilePath();
        if (!systemIdFile.exists()) {
            try (OutputStream fos = Files.newOutputStream(systemIdFile.toPath())) {
                fos.write(UUID.randomUUID().toString().getBytes());
            } catch (IOException e) {
                LOGGER.error("An error occurred writing to " + systemIdFile.getAbsolutePath(), e);
            }
        }
        try {
            systemId = new String(Files.readAllBytes(systemIdFile.toPath()));
        } catch (IOException e) {
            LOGGER.error("Unable to read the contents of " + systemIdFile.getAbsolutePath(), e);
        }
    }

    /**
     * @since 3.2.0
     */
    public org.eclipse.microprofile.config.Config getDelegate() {
        return delegateConfig;
    }

    /**
     * @since 3.2.0
     */
    public <T> T getMapping(final Class<T> mappingClass) {
        return delegateConfig.unwrap(SmallRyeConfig.class).getConfigMapping(mappingClass);
    }

    /**
     * Retrieves the path where the system.id is stored
     * @return a File representing the path to the system.id
     * @since 1.8.0
     */
    private File getSystemIdFilePath() {
        return new File(Config.getInstance().getDataDirectorty() + File.separator + "id.system");
    }

    /**
     * Returns the UUID unique to a system deployment.
     * @return the UUID unique to a deployed system
     * @since 1.8.0
     */
    public String getSystemUuid() {
        return systemId;
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
     * Return the configured value for the specified Key. As of v1.4.3, this
     * method will first check if the key has been specified as an environment
     * variable. If it has, the method will return the value. If it hasn't
     * been specified in the environment, it will retrieve the value (and optional
     * default value) from the properties configuration.
     *
     * This method is Docker-friendly in that configuration can be specified via
     * environment variables which is a common method of configuration when
     * configuration files are not easily accessible.
     *
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.0.0
     */
    public String getProperty(Key key) {
        return delegateConfig.getOptionalValue(key.getPropertyName(), String.class)
                .orElseGet(() -> key.getDefaultValue() != null
                        ? String.valueOf(key.getDefaultValue())
                        : null);
    }

    /**
     * Check if key with _FILE postfix appended is a defined property name,
     * either in the environment or in the properties configuration. If yes,
     * the named file is read and its content will be the key's value.
     *
     * This method is defined so that passwords can be read from docker secret.
     *
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.7.0
     */
    public String getPropertyOrFile(AlpineKey key) {
        return delegateConfig.getOptionalValue(key.getPropertyName() + ".file", String.class)
                .map(filePath -> {
                    try {
                        return new String(Files.readAllBytes(new File(PathUtil.resolve(filePath)).toPath())).replaceAll("\\s+", "");
                    } catch (IOException e) {
                        LOGGER.error(filePath + " file doesn't exist or not readable.", e);
                        return null;
                    }
                })
                .or(() -> delegateConfig.getOptionalValue(key.getPropertyName(), String.class))
                .orElse(null);
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
     * @return a list of the comma-separated values of the configuration,
     *         or an empty list otherwise
     * @since 2.2.5
     */
    public List<String> getPropertyAsList(Key key) {
        return delegateConfig.getValues(key.getPropertyName(), String.class).stream()
                .map(String::trim)
                .filter(not(String::isEmpty))
                .collect(Collectors.toList());
    }

    /**
     * Get "pass-through" properties with a given {@code prefix}.
     * <p>
     * Pass-through properties do not have corresponding {@link Config.Key}s.
     * Their main use-case is to allow users to configure certain aspects of libraries and frameworks used by Alpine,
     * without Alpine having to introduce {@link AlpineKey}s for every single option.
     * <p>
     * Properties <strong>must</strong> be prefixed with {@code ALPINE_} (for environment variables) or {@code alpine.}
     * (for {@code application.properties}) respectively. The Alpine prefix will be removed in keys of the returned
     * {@link Map}, but the given {@code prefix} will be retained.
     *
     * @param prefix Prefix of the properties to fetch
     * @return A {@link Map} containing the matched properties
     * @since 2.3.0
     */
    public Map<String, String> getPassThroughProperties(final String prefix) {
        final var passThroughProperties = new HashMap<String, String>();
        for (final String propertyName : delegateConfig.getPropertyNames()) {
            if (!propertyName.startsWith("alpine.%s.".formatted(prefix))) {
                continue;
            }

            final String key = propertyName.replaceFirst("^alpine\\.", "");
            passThroughProperties.put(key, delegateConfig.getValue(propertyName, String.class));
        }
        return passThroughProperties;
    }

    /**
     * Reload the configuration.
     *
     * @since 3.2.0
     */
    static void reload() {
        delegateConfig = null;
        INSTANCE.init();
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The Key to return the configuration for
     * @return a String of the value of the configuration
     * @since 1.0.0
     * @deprecated use {{@link #getProperty(Key)}}
     */
    @Deprecated
    public String getProperty(String key) {
        return delegateConfig.getOptionalValue(key, String.class).orElse(null);
    }

    /**
     * Return the configured value for the specified Key.
     * @param key The String of the key to return the configuration for
     * @param defaultValue The default value if the key cannot be found
     * @return a String of the value of the configuration
     * @since 1.0.0
     * @deprecated use {{@link #getProperty(Key)}
     */
    @Deprecated
    public String getProperty(String key, String defaultValue) {
        return delegateConfig.getOptionalValue(key, String.class).orElse(defaultValue);
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
