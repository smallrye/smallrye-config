package io.smallrye.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

class ConfigurableConfigSource implements ConfigSource {
    final ConfigSourceFactory factory;

    ConfigurableConfigSource(ConfigSourceFactory factory) {
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

    ConfigSource getSource(ConfigSourceContext context) {
        return factory.getSource(context);
    }
}
