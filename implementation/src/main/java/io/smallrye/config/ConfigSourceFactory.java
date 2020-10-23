package io.smallrye.config;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

/**
 * This {@code ConfigSourceFactory} allows to initialize a {@link ConfigSource}, with access to the current
 * {@link ConfigSourceContext}.
 * <p>
 *
 * The provided {@link ConfigSource} is initialized in priority order and the current {@link ConfigSourceContext} has
 * access to all previous initialized {@code ConfigSources}. This allows the factory to configure the
 * {@link ConfigSource} with all other {@code ConfigSources} available, except for {@code ConfigSources} initialized by
 * another {@code ConfigSourceFactory}.
 * <p>
 *
 * Instances of this interface will be discovered by {@link SmallRyeConfigBuilder#withSources(ConfigSourceFactory...)}
 * via the {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/io.smallrye.config.ConfigSourceFactory} which contains the fully qualified class name of the
 * custom {@link ConfigSourceFactory} implementation.
 */
@Experimental("ConfigSource API Enhancements")
public interface ConfigSourceFactory {
    Iterable<ConfigSource> getConfigSources(ConfigSourceContext context);

    /**
     * Returns the factory priority. This is required, because the factory needs to be sorted before doing
     * initialization. Once the factory is initialized, each a {@link ConfigSource} will use its own ordinal to
     * determine the config lookup order.
     *
     * @return the priority value.
     */
    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
