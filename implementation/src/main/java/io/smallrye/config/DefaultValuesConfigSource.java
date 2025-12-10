package io.smallrye.config;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.AbstractConfigSource;

public final class DefaultValuesConfigSource extends AbstractConfigSource {
    @Serial
    private static final long serialVersionUID = -6386021034957868328L;

    public static final String NAME = "DefaultValuesConfigSource";
    public static final int ORDINAL = Integer.MIN_VALUE;

    private final Map<String, String> properties;
    private final Map<PropertyName, String> wildcards;
    private final boolean hasProfiledName;

    DefaultValuesConfigSource(final Map<String, String> properties) {
        this(properties, () -> NAME, ORDINAL);
    }

    public DefaultValuesConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        this(properties, new Supplier<String>() {
            @Override
            public String get() {
                if (NAME.equals(name)) {
                    throw ConfigMessages.msg.defaultValuesConfigSourceNameReserved(name);
                }
                return name;
            }
        }, ordinal);
    }

    private DefaultValuesConfigSource(final Map<String, String> properties, final Supplier<String> name, final int ordinal) {
        super(name.get(), ordinal);
        this.properties = new HashMap<>();
        this.wildcards = new HashMap<>();
        boolean hasProfiledName = false;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            String value = entry.getValue();
            if (propertyName.indexOf('*') == -1) {
                this.properties.put(propertyName, value);
            } else {
                this.wildcards.put(new PropertyName(propertyName), value);
            }

            if (!hasProfiledName && !propertyName.isEmpty() && propertyName.charAt(0) == '%') {
                hasProfiledName = true;
            }
        }
        this.hasProfiledName = hasProfiledName;
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
