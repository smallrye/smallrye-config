package io.smallrye.config;

import java.io.Serial;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.AbstractConfigSource;

public final class DefaultValuesConfigSource extends AbstractConfigSource {
    @Serial
    private static final long serialVersionUID = -6386021034957868328L;

    public static final String NAME = DefaultValuesConfigSource.class.getSimpleName();
    public static final int ORDINAL = Integer.MIN_VALUE;

    private final Defaults defaults;
    private final Set<String> names;
    private final boolean hasProfiledName;

    DefaultValuesConfigSource(final Defaults defaults) {
        super(NAME, ORDINAL);
        this.defaults = defaults;
        this.names = names(defaults.getProperties());
        this.hasProfiledName = hasProfiledName(defaults.getProperties());
    }

    @Deprecated(forRemoval = true)
    public DefaultValuesConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        super(validateName(name), ordinal);
        this.defaults = new Defaults();
        this.defaults.add(properties);
        this.names = names(properties);
        this.hasProfiledName = hasProfiledName(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return names;
    }

    public String getValue(final String propertyName) {
        if (!hasProfiledName && !propertyName.isEmpty() && propertyName.charAt(0) == '%') {
            return null;
        }
        return defaults.get(propertyName);
    }

    // We need a way to add defaults after SmallRyeConfig initializes, due to MP @ConfigProperties
    void addDefaults(final Defaults defaults) {
        this.defaults.add(defaults.getProperties());
    }

    private static String validateName(final String name) {
        if (NAME.equals(name)) {
            throw ConfigMessages.msg.defaultValuesConfigSourceNameReserved(name);
        }
        return name;
    }

    private static boolean hasProfiledName(final Map<String, String> properties) {
        for (String name : properties.keySet()) {
            if (!name.isEmpty() && name.charAt(0) == '%') {
                return true;
            }
        }
        return false;
    }

    private static Set<String> names(Map<String, String> properties) {
        Set<String> names = new HashSet<>(properties.size() / 2);
        for (Entry<String, String> entry : properties.entrySet()) {
            if (!entry.getKey().isEmpty() && entry.getValue() != null) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    public static class Defaults extends PropertyNamesMatcher<String> {

    }
}
