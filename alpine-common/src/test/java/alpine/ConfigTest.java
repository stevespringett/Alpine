package alpine;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.WithDefault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RestoreEnvironmentVariables;
import org.junitpioneer.jupiter.RestoreSystemProperties;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {

    @AfterEach
    public void tearDown() {
        Config.reset();
    }

    @AfterAll
    public static void tearDownClass() {
        Config.getInstance().init(); // Ensure we're not affecting other tests
    }

    @Test
    public void testGetPassThroughPropertiesEmpty() {
        Config.getInstance().init();

        assertThat(Config.getInstance().getPassThroughProperties("datanucleus")).isEmpty();
    }

    @Test
    @RestoreEnvironmentVariables
    @RestoreSystemProperties
    @SetEnvironmentVariable(key = "ALPINE_FOO", value = "fromEnv1")
    @SetEnvironmentVariable(key = "ALPINE_DATANUCLEUS", value = "fromEnv2")
    @SetEnvironmentVariable(key = "ALPINE_DATANUCLEUS_FOO", value = "fromEnv3")
    @SetEnvironmentVariable(key = "ALPINE_DATANUCLEUS_FOO_BAR", value = "fromEnv4")
    @SetEnvironmentVariable(key = "ALPINE_DATA_NUCLEUS_FOO", value = "fromEnv5")
    @SetEnvironmentVariable(key = "DATANUCLEUS_FOO", value = "fromEnv6")
    @SetEnvironmentVariable(key = "ALPINE_DATANUCLEUS_FROM_ENV", value = "fromEnv7")
    @SetEnvironmentVariable(key = "alpine_datanucleus_from_env_lowercase", value = "fromEnv8")
    @SetEnvironmentVariable(key = "Alpine_DataNucleus_From_Env_MixedCase", value = "fromEnv9")
    @SetEnvironmentVariable(key = "ALPINE_DATANUCLEUS_EXPRESSION_FROM_ENV", value = "${alpine.datanucleus.from.env}")
    public void testGetPassThroughProperties() throws Exception {
        final URL propertiesUrl = ConfigTest.class.getResource("/Config_testGetPassThroughProperties.properties");
        assertThat(propertiesUrl).isNotNull();

        final Path tmpPropertiesFile = Files.createTempFile(null, ".properties");
        Files.copy(propertiesUrl.openStream(), tmpPropertiesFile, StandardCopyOption.REPLACE_EXISTING);

        System.setProperty("alpine.application.properties", tmpPropertiesFile.toUri().toString());

        Config.getInstance().init();

        assertThat(Config.getInstance().getPassThroughProperties("datanucleus"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "datanucleus.foo", "fromEnv3", // ENV takes precedence over properties
                        "datanucleus.foo.bar", "fromEnv4", // ENV takes precedence over properties
                        "datanucleus.from.env", "fromEnv7",
                        "datanucleus.from.props", "fromProps7",
                        "datanucleus.from.env.lowercase", "fromEnv8",
                        "datanucleus.from.env.mixedcase", "fromEnv9",
                        "datanucleus.expression.from.props", "fromEnv3",
                        "datanucleus.expression.from.env", "fromEnv7"
                ));
    }

    @ConfigMapping(prefix = "alpine")
    public interface TestConfig {

        DatabaseConfig database();

        interface DatabaseConfig {

            Optional<String> url();

            @WithDefault("testUser")
            String username();

            Map<String, String> pool();

        }

    }

    public static class ConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {

        @Override
        public void configBuilder(final SmallRyeConfigBuilder configBuilder) {
            configBuilder
                    .withMapping(TestConfig.class)
                    .withValidateUnknown(false);
        }

    }

    @Test
    @RestoreEnvironmentVariables
    @SetEnvironmentVariable(key = "ALPINE_DATABASE_URL", value = "jdbc:h2:mem:alpine")
    @SetEnvironmentVariable(key = "ALPINE_DATABASE_POOL_MAX_SIZE", value = "666")
    void testGetMapping() {
        Config.getInstance().init();

        final var testConfig = Config.getInstance().getMapping(TestConfig.class);
        assertThat(testConfig).isNotNull();
        assertThat(testConfig.database().url()).contains("jdbc:h2:mem:alpine");
        assertThat(testConfig.database().username()).isEqualTo("testUser");
        assertThat(testConfig.database().pool())
                .containsExactlyInAnyOrderEntriesOf(Map.of("max.size", "666"));
    }

}