package io.smallrye.config;

import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newArrayConverter;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;

import java.net.URI;
import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

/**
 * This {@code AbstractLocationConfigSourceFactory} allows to initialize additional config locations with the
 * configuration {@link SmallRyeConfig#SMALLRYE_CONFIG_LOCATIONS}. The configuration support multiple
 * locations separated by a comma and each must represent a valid {@link URI}.
 */
@Experimental("Loads additional config locations")
public abstract class AbstractLocationConfigSourceFactory extends AbstractLocationConfigSourceLoader
        implements ConfigSourceFactory {

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue(SMALLRYE_CONFIG_LOCATIONS);
        if (value.getValue() == null) {
            return Collections.emptyList();
        }

        return loadConfigSources(newArrayConverter(STRING_CONVERTER, String[].class).convert(value.getValue()),
                value.getConfigSourceOrdinal());
    }
}
