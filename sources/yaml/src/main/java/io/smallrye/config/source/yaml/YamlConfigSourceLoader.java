package io.smallrye.config.source.yaml;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;

public class YamlConfigSourceLoader extends AbstractLocationConfigSourceLoader {
    @Override
    protected String[] getFileExtensions() {
        return new String[] {
                "yaml",
                "yml"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new YamlConfigSource(url, ordinal);
    }

    public static class InClassPath extends YamlConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            List<ConfigSource> configSources = new ArrayList<>();
            configSources.addAll(loadConfigSources("application.yaml", 255, classLoader));
            configSources.addAll(loadConfigSources("application.yml", 255, classLoader));
            configSources.addAll(loadConfigSources("META-INF/microprofile-config.yaml", 110, classLoader));
            configSources.addAll(loadConfigSources("META-INF/microprofile-config.yml", 110, classLoader));
            return configSources;
        }

        @Override
        protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
            return emptyList();
        }
    }

    public static class InFileSystem extends YamlConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            List<ConfigSource> configSources = new ArrayList<>();
            configSources.addAll(loadConfigSources(
                    Paths.get(System.getProperty("user.dir"), "config", "application.yaml").toUri().toString(), 265,
                    classLoader));
            configSources.addAll(
                    loadConfigSources(Paths.get(System.getProperty("user.dir"), "config", "application.yml").toUri().toString(),
                            265, classLoader));
            return configSources;
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return emptyList();
        }
    }
}
