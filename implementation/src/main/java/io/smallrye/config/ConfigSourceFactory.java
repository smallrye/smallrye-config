package io.smallrye.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

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

    /**
     * A {@code ConfigSourceFactory} that accepts a {@link ConfigMapping} interface to configure the
     * {@link ConfigSource}.
     *
     * @param <MAPPING> {@link ConfigMapping} interface.
     */
    interface ConfigurableConfigSourceFactory<MAPPING> extends ConfigSourceFactory {
        @Override
        @SuppressWarnings("unchecked")
        default Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
            Type typeArgument = ((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];

            List<String> profiles = new ArrayList<>(context.getProfiles());
            Collections.reverse(profiles);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .withProfiles(profiles)
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withSources(context.getConfigSources())
                    .withMapping((Class<? extends MAPPING>) typeArgument)
                    .build();

            MAPPING mapping = config.getConfigMapping((Class<? extends MAPPING>) typeArgument);

            return getConfigSources(context, mapping);
        }

        Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, MAPPING config);
    }
}
