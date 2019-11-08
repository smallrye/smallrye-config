package io.smallrye.config;

import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_100;
import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;

import java.util.Collections;
import java.util.Map;

public abstract class MapBackedConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -7159956218217228877L;

    private final Map<String, String> properties;

    public MapBackedConfigSource(String name, Map<String, String> propertyMap) {
        super(name, Integer.parseInt(propertyMap.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_100)));
        properties = propertyMap;
    }

    public MapBackedConfigSource(String name, Map<String, String> propertyMap, int defaultOrdinal) {
        super(name, Integer.parseInt(propertyMap.getOrDefault(CONFIG_ORDINAL_KEY, String.valueOf(defaultOrdinal))));
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
