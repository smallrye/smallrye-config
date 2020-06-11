package io.smallrye.config;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

/**
 * This ConfigSourceFactory allows to initialize a {@link ConfigSource}, with access to the current
 * {@link ConfigSourceContext}.
 * <p>
 *
 * The provided {@link ConfigSource} is initialized in priority order and the current {@link ConfigSourceContext} has
 * access to all previous initialized {@code ConfigSources}. This allows the factory to configure the
 * {@link ConfigSource} with configurations read from higher priority {@code ConfigSources}.
 * <p>
 *
 * Instances of this interface will be discovered by {@link SmallRyeConfigBuilder#withSources(ConfigSourceFactory...)}
 * via the {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/io.smallrye.config.ConfigSourceFactory} which contains the fully qualified class name of the
 * custom {@link ConfigSourceFactory} implementation.
 */
@Experimental("ConfigSource API Enhancements")
public interface ConfigSourceFactory {
    ConfigSource getSource(ConfigSourceContext context);

    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
