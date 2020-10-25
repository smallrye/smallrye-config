package io.smallrye.config.source.zookeeper;

import static io.smallrye.config.source.zookeeper.ZooKeeperConfigSource.APPLICATION_ID_KEY;
import static io.smallrye.config.source.zookeeper.ZooKeeperConfigSource.ZOOKEEPER_URL_KEY;

import java.util.Collections;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class ZooKeeperConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        return Collections.singletonList(new ZooKeeperConfigSource(context.getValue(ZOOKEEPER_URL_KEY).getValue(),
                context.getValue(APPLICATION_ID_KEY).getValue()));
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(150);
    }
}
