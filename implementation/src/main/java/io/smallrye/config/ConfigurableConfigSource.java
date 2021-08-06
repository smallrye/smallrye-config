package io.smallrye.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    ConfigSourceFactory getFactory() {
        return factory;
    }

    List<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        return unwrap(context, new ArrayList<>());
    }

    private List<ConfigSource> unwrap(final ConfigSourceContext context, final List<ConfigSource> configSources) {
        for (final ConfigSource configSource : factory.getConfigSources(context)) {
            if (configSource instanceof ConfigurableConfigSource) {
                configSources.addAll(((ConfigurableConfigSource) configSource).getConfigSources(context));
            } else {
                configSources.add(configSource);
            }
        }
        return configSources;
    }
}
