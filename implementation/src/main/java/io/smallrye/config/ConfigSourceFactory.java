package io.smallrye.config;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.annotation.Experimental;

@Experimental("")
public interface ConfigSourceFactory {
    ConfigSource getSource(ConfigSourceContext context);

    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
