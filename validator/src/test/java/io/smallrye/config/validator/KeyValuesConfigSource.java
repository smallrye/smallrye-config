package io.smallrye.config.validator;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class KeyValuesConfigSource implements ConfigSource, Serializable {

    private final Map<String, String> properties = new HashMap<>();

    private KeyValuesConfigSource(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "KeyValuesConfigSource";
    }

    public static ConfigSource config(Map<String, String> properties) {
        return new KeyValuesConfigSource(properties);
    }

    public static ConfigSource config(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues array must be a multiple of 2");
        }

        Map<String, String> props = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.put(keyValues[i], keyValues[i + 1]);
        }
        return new KeyValuesConfigSource(props);
    }
}
