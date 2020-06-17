package io.smallrye.config;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public final class DefaultValuesConfigSource implements ConfigSource {
    private final KeyMap<String> defaultValues;

    public DefaultValuesConfigSource(final KeyMap<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    public String getValue(final String s) {
        return defaultValues.findRootValue(s);
    }

    public String getName() {
        return "Default values";
    }

    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
