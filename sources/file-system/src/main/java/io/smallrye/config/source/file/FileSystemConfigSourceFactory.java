package io.smallrye.config.source.file;

import java.util.Collections;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;

public class FileSystemConfigSourceFactory implements ConfigSourceFactory {
    public static final String SMALLRYE_CONFIG_SOURCE_FILE_LOCATION = "smallrye.config.source.file.location";

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue(SMALLRYE_CONFIG_SOURCE_FILE_LOCATION);
        if (value == null || value.getValue() == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new FileSystemConfigSource(value.getValue()));
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(290);
    }
}
