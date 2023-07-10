package alpine.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.Consumer;

/**
 * A customizer for Micrometer {@link MeterRegistry}.
 * <p>
 * Customizers must be deployed as service providers in order to be discoverable.
 * Refer to the {@link java.util.ServiceLoader} documentation for details.
 *
 * @since 2.3.0
 */
@FunctionalInterface
public interface MeterRegistryCustomizer extends Consumer<MeterRegistry> {
}
