package io.smallrye.config.examples.configmap;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.source.file.FileSystemConfigSource;

public class ConfigMapConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public ConfigSource getSource(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue("config.map.dir.source");
        if (value == null || value.getValue() == null) {
            throw new IllegalArgumentException("CONFIG_MAP_DIR_SOURCE not defined");
        }

        return new FileSystemConfigSource(value.getValue());
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(200);
    }
}
