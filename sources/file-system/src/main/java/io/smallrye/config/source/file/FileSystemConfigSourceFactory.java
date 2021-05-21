package io.smallrye.config.source.file;

import static io.smallrye.config.Converters.newArrayConverter;

import java.util.Collections;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;

public class FileSystemConfigSourceFactory implements ConfigSourceFactory {
    public static final String SMALLRYE_CONFIG_SOURCE_FILE_LOCATIONS = "smallrye.config.source.file.locations";

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue(SMALLRYE_CONFIG_SOURCE_FILE_LOCATIONS);
        if (value == null || value.getValue() == null) {
            return Collections.emptyList();
        }

        return Stream
                .of(newArrayConverter(Converters.getImplicitConverter(String.class), String[].class)
                        .convert(value.getValue()))
                .map(location -> new FileSystemConfigSource(location))
                .collect(Collectors.toList());
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(290);
    }
}
