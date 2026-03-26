package io.smallrye.config;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.AbstractConfigSource;

public final class DefaultValuesConfigSource extends AbstractConfigSource {
    @Serial
    private static final long serialVersionUID = -6386021034957868328L;

    public static final String NAME = "DefaultValuesConfigSource";
    public static final int ORDINAL = Integer.MIN_VALUE;

    private final Map<String, String> properties;
    private final List<WildcardEntry> wildcards;
    private final boolean hasProfiledName;

    DefaultValuesConfigSource(final Map<String, String> properties) {
        this(NAME, ORDINAL, properties);
    }

    public DefaultValuesConfigSource(final Map<String, String> properties, final String name, final int ordinal) {
        this(validateName(name), ordinal, properties);
    }

    private DefaultValuesConfigSource(final String name, final int ordinal, final Map<String, String> properties) {
        super(name, ordinal);
        this.properties = new HashMap<>();
        this.wildcards = new ArrayList<>();
        boolean hasProfiledName = false;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            String value = entry.getValue();
            if (propertyName.indexOf('*') == -1) {
                this.properties.put(propertyName, value);
            } else {
                addWildcard(propertyName, value, false);
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
            for (int i = 0; i < wildcards.size(); i++) {
                WildcardEntry entry = wildcards.get(i);
                if (PropertyName.equals(propertyName, entry.pattern)) {
                    return entry.value;
                }
            }
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
            addWildcard(name, value, true);
        }
    }

    private void addWildcard(final String name, final String value, final boolean onlyIfAbsent) {
        for (int i = 0; i < wildcards.size(); i++) {
            if (name.equals(wildcards.get(i).pattern)) {
                if (!onlyIfAbsent) {
                    wildcards.set(i, new WildcardEntry(name, value));
                }
                return;
            }
        }
        // Insert in sorted position (descending by pattern length for specificity)
        int insertPos = 0;
        while (insertPos < wildcards.size() && wildcards.get(insertPos).pattern.length() >= name.length()) {
            insertPos++;
        }
        wildcards.add(insertPos, new WildcardEntry(name, value));
    }

    private static String validateName(final String name) {
        if (NAME.equals(name)) {
            throw ConfigMessages.msg.defaultValuesConfigSourceNameReserved(name);
        }
        return name;
    }

    private record WildcardEntry(String pattern, String value) {
    }
}
