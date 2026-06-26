package io.smallrye.config.source.toml;

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

public class TomlConfigSourceLoader extends AbstractLocationConfigSourceLoader {
    @Override
    protected String[] getFileExtensions() {
        return new String[] {
                "toml"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new TomlConfigSource(url, ordinal);
    }

    public static class InClassPath extends TomlConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            List<ConfigSource> configSources = new ArrayList<>();
            configSources.addAll(loadConfigSources("application.toml", 255, classLoader));
            configSources.addAll(loadConfigSources("META-INF/microprofile-config.toml", 110, classLoader));
            return configSources;
        }

        @Override
        protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
            return emptyList();
        }
    }

    public static class InFileSystem extends TomlConfigSourceLoader implements ConfigSourceProvider {
        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources(Paths.get(System.getProperty("user.dir"), "config", "application.toml").toUri().toString(),
                    265, classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return emptyList();
        }
    }
}
