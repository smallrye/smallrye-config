package io.smallrye.config.source.yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class YamlConfigSourceProvider implements ConfigSourceProvider {

    private static final String META_INF_MICROPROFILE_CONFIG_RESOURCE = "META-INF/microprofile-config.yaml";
    private static final String WEB_INF_MICROPROFILE_CONFIG_RESOURCE = "WEB-INF/classes/META-INF/microprofile-config.yaml";

    static Optional<ConfigSource> getConfigSource(ClassLoader classLoader, String resource, int ordinal) {
        try {
            return Optional.of(new YamlConfigSource(resource, classLoader.getResourceAsStream(resource), ordinal));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>(2);
        getConfigSource(classLoader, META_INF_MICROPROFILE_CONFIG_RESOURCE, YamlConfigSource.ORDINAL + 10)
                .ifPresent(configSources::add);
        getConfigSource(classLoader, WEB_INF_MICROPROFILE_CONFIG_RESOURCE, YamlConfigSource.ORDINAL)
                .ifPresent(configSources::add);
        return Collections.unmodifiableList(configSources);
    }
}
