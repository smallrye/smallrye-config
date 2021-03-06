package io.smallrye.config.source.hocon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;

public class HoconConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
    @Override
    protected String[] getFileExtensions() {
        return new String[] {
                "conf"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new HoconConfigSource(url, ordinal);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        return new ArrayList<>(loadConfigSources("META-INF/microprofile-config.conf", HoconConfigSource.ORDINAL, classLoader));
    }
}
