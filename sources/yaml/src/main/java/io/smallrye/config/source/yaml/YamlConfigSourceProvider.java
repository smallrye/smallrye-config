package io.smallrye.config.source.yaml;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;

public class YamlConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
    @Override
    public String[] getFileExtensions() {
        return new String[] {
                "yaml",
                "yml"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new YamlConfigSource(url, ordinal);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        final List<ConfigSource> sources = new ArrayList<>();
        sources.addAll(loadConfigSources("META-INF/microprofile-config.yaml", 110, classLoader));
        sources.addAll(loadConfigSources("META-INF/microprofile-config.yml", 110, classLoader));
        return sources;
    }
}
