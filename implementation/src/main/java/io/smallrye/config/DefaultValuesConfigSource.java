package io.smallrye.config;

import static io.smallrye.config.common.utils.ConfigSourceUtil.hasProfiledName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.common.AbstractConfigSource;

public final class DefaultValuesConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -6386021034957868328L;

    public static final String NAME = "DefaultValuesConfigSource";
    public static final int ORDINAL = Integer.MIN_VALUE;

    private final Map<String, String> properties;
    private final Map<PropertyName, String> wildcards;
    private final boolean hasProfiledName;

    public DefaultValuesConfigSource(final Map<String, String> properties) {
        this(properties, NAME, ORDINAL);
    }

    public DefaultValuesConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        super(name, ordinal);
        this.properties = new HashMap<>();
        this.wildcards = new HashMap<>();
        addDefaults(properties);
        this.hasProfiledName = hasProfiledName(getPropertyNames());
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    public String getValue(final String propertyName) {
        if (!hasProfiledName && !propertyName.isEmpty() && propertyName.charAt(0) == '%') {
            return null;
        }
        String value = properties.get(propertyName);
        if (value == null) {
            value = wildcards.get(new PropertyName(propertyName));
        }
        return value;
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
            this.wildcards.putIfAbsent(new PropertyName(name), value);
        }
    }
}
