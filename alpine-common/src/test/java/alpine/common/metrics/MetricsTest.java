package alpine.common.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    public static class TestMeterRegistryCustomizer implements MeterRegistryCustomizer {

        @Override
        public void accept(final MeterRegistry meterRegistry) {
            meterRegistry.config().meterFilter(MeterFilter.commonTags(Tags.of("commonKey", "commonValue")));
        }

    }

    @Test
    public void testCustomized() {
        var registry = Metrics.customized(new SimpleMeterRegistry());
       
        Gauge.builder("foo", () -> 123)
                .tag("foo", "bar")
                .register(registry);

        assertThat(registry.getMetersAsString())
                .isEqualTo("foo(GAUGE)[commonKey='commonValue', foo='bar']; value=123.0");
    }

}