package io.smallrye.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class ConfigurableConfigSource implements ConfigSource {
    private final ConfigSourceFactory factory;

    public ConfigurableConfigSource(ConfigSourceFactory factory) {
        this.factory = factory;
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public Set<String> getPropertyNames() {
        return new HashSet<>();
    }

    @Override
    public String getValue(final String propertyName) {
        return null;
    }

    @Override
    public String getName() {
        return factory.getClass().getName();
    }

    @Override
    public int getOrdinal() {
        return factory.getPriority().orElse(DEFAULT_ORDINAL);
    }

    List<ConfigSource> getConfigSources(ConfigSourceContext context) {
        return StreamSupport.stream(factory.getConfigSources(context).spliterator(), false).collect(Collectors.toList());
    }
}
