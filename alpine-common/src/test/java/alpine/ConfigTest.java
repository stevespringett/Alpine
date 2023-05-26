package alpine;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @After
    public void tearDown() {
        Config.reset();
    }

    @AfterClass
    public static void tearDownClass() {
        Config.getInstance().init(); // Ensure we're not affecting other tests
    }

    @Test
    public void testGetPassThroughPropertiesEmpty() {
        Config.getInstance().init();

        assertThat(Config.getInstance().getPassThroughProperties("datanucleus")).isEmpty();
    }

    @Test
    public void testGetPassThroughProperties() {
        final URL propertiesUrl = ConfigTest.class.getResource("/Config_testGetPassThroughProperties.properties");
        assertThat(propertiesUrl).isNotNull();

        System.setProperty("alpine.application.properties", propertiesUrl.getPath());

        Config.getInstance().init();

        environmentVariables.set("ALPINE_FOO", "fromEnv1");
        environmentVariables.set("ALPINE_DATANUCLEUS", "fromEnv2");
        environmentVariables.set("ALPINE_DATANUCLEUS_FOO", "fromEnv3");
        environmentVariables.set("ALPINE_DATANUCLEUS_FOO_BAR", "fromEnv4");
        environmentVariables.set("ALPINE_DATA_NUCLEUS_FOO", "fromEnv5");
        environmentVariables.set("DATANUCLEUS_FOO", "fromEnv6");
        environmentVariables.set("ALPINE_DATANUCLEUS_FROM_ENV", "fromEnv7");
        environmentVariables.set("alpine_datanucleus_from_env_lowercase", "fromEnv8");
        environmentVariables.set("Alpine_DataNucleus_From_Env_MixedCase", "fromEnv9");

        assertThat(Config.getInstance().getPassThroughProperties("datanucleus"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "datanucleus.foo", "fromEnv3", // ENV takes precedence over properties
                        "datanucleus.foo.bar", "fromEnv4", // ENV takes precedence over properties
                        "datanucleus.from.env", "fromEnv7",
                        "datanucleus.from.props", "fromProps7"
                ));
    }

}