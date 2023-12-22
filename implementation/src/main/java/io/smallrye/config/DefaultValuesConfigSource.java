package io.smallrye.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

public final class DefaultValuesConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -6386021034957868328L;

    private final Map<String, String> properties;

    private final Map<PropertyName, String> wildcards;

    public DefaultValuesConfigSource(final Map<String, String> properties) {
        this(properties, "DefaultValuesConfigSource", Integer.MIN_VALUE);
    }

    public DefaultValuesConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        super(name, ordinal);
        this.properties = new HashMap<>();
        this.wildcards = new HashMap<>();
        addDefaults(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    public String getValue(final String propertyName) {
        return properties.getOrDefault(propertyName, wildcards.get(new PropertyName(propertyName)));
    }

    void addDefaults(final Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            addDefault(entry.getKey(), entry.getValue());
        }
    }

    void addDefault(final String name, final String value) {
        if (name.indexOf('*') == -1) {
            this.properties.putIfAbsent(name, value);
        } else {
            this.wildcards.put(new PropertyName(name), value);
        }
    }
}
