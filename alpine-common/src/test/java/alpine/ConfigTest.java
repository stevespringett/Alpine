package alpine;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RestoreEnvironmentVariables;
import org.junitpioneer.jupiter.RestoreSystemProperties;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.net.URL;
import java.util.Map;

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
    public void testGetPassThroughProperties() {
        final URL propertiesUrl = ConfigTest.class.getResource("/Config_testGetPassThroughProperties.properties");
        assertThat(propertiesUrl).isNotNull();

        System.setProperty("alpine.application.properties", propertiesUrl.getPath());

        Config.getInstance().init();

        assertThat(Config.getInstance().getPassThroughProperties("datanucleus"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "datanucleus.foo", "fromEnv3", // ENV takes precedence over properties
                        "datanucleus.foo.bar", "fromEnv4", // ENV takes precedence over properties
                        "datanucleus.from.env", "fromEnv7",
                        "datanucleus.from.props", "fromProps7"
                ));
    }

}