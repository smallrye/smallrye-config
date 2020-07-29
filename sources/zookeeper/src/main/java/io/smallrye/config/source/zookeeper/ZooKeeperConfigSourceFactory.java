package io.smallrye.config.source.zookeeper;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class ZooKeeperConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public ConfigSource getConfigSource(final ConfigSourceContext context) {
        return new ZooKeeperConfigSource(context.getValue(ZooKeeperConfigSource.ZOOKEEPER_URL_KEY).getValue(),
                context.getValue(ZooKeeperConfigSource.APPLICATION_ID_KEY).getValue());
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(150);
    }
}
