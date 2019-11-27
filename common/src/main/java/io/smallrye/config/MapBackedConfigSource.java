package io.smallrye.config;

import java.util.Collections;
import java.util.Map;

import io.smallrye.config.utils.ConfigSourceUtil;

public abstract class MapBackedConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -7159956218217228877L;

    private final Map<String, String> properties;

    public MapBackedConfigSource(String name, Map<String, String> propertyMap) {
        this(name, propertyMap, 100);
    }

    public MapBackedConfigSource(String name, Map<String, String> propertyMap, int defaultOrdinal) {
        super(name, ConfigSourceUtil.getOrdinalFromMap(propertyMap, defaultOrdinal));
        properties = propertyMap;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }
}
