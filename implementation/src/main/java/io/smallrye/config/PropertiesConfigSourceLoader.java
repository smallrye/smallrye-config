package io.smallrye.config;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class PropertiesConfigSourceLoader extends AbstractLocationConfigSourceLoader {
    protected final String path;
    protected final int ordinal;

    PropertiesConfigSourceLoader(final String path, final int ordinal) {
        this.path = path;
        this.ordinal = ordinal;
    }

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new PropertiesConfigSource(url, ordinal);
    }

    public static List<ConfigSource> inClassPath(final String path, final int ordinal, final ClassLoader loader) {
        return new InClassPath(path, ordinal).getConfigSources(loader);
    }

    public static List<ConfigSource> inFileSystem(final String path, final int ordinal, final ClassLoader loader) {
        return new InFileSystem(path, ordinal).getConfigSources(loader);
    }

    private static class InClassPath extends PropertiesConfigSourceLoader implements ConfigSourceProvider {
        public InClassPath(final String path, final int ordinal) {
            super(path, ordinal);
        }

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources(path, ordinal, classLoader);
        }

        @Override
        protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
            return emptyList();
        }
    }

    private static class InFileSystem extends PropertiesConfigSourceLoader implements ConfigSourceProvider {
        public InFileSystem(final String path, final int ordinal) {
            super(path, ordinal);
        }

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources(path, ordinal, classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return emptyList();
        }
    }
}
