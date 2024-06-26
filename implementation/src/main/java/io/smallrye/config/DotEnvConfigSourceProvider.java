package io.smallrye.config;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.common.utils.ConfigSourceUtil;

public class DotEnvConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
    private final String location;

    public DotEnvConfigSourceProvider() {
        this(getDotEnvFile(".env"));
    }

    public DotEnvConfigSourceProvider(final String location) {
        this.location = location;
    }

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new EnvConfigSource(ConfigSourceUtil.urlToMap(url), ordinal) {
            @Override
            public String getName() {
                return super.getName() + "[source=" + url + "]";
            }
        };
    }

    @Override
    public List<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        return loadConfigSources(location, 295, forClassLoader);
    }

    public static List<ConfigSource> dotEnvSources(final ClassLoader classLoader) {
        return new DotEnvConfigSourceProvider().getConfigSources(classLoader);
    }

    public static List<ConfigSource> dotEnvSources(final String location, final ClassLoader classLoader) {
        return new DotEnvConfigSourceProvider(location).getConfigSources(classLoader);
    }

    private static String getDotEnvFile(final String filename) {
        Path dotEnvFile = Paths.get(System.getProperty("user.dir"), filename);
        return Files.isDirectory(dotEnvFile) ? null : dotEnvFile.toUri().toString();
    }
}
