package io.smallrye.config.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.utils.ConfigSourceUtil;

public abstract class MapBackedConfigSource extends AbstractConfigSource {
    private static final long serialVersionUID = -7159956218217228877L;

    private final Map<String, String> properties;

    /**
     * Construct a new instance. The config source will use a default ordinal of {@code 100} and
     * will use the given map as-is (not a copy of it).
     *
     * @param name the config source name
     * @param propertyMap the map to use
     */
    public MapBackedConfigSource(String name, Map<String, String> propertyMap) {
        this(name, propertyMap, ConfigSource.DEFAULT_ORDINAL);
    }

    /**
     * Construct a new instance. The config source will use a default ordinal of {@code 100} and
     * will use a copy of the given map if {@code copy} is {@code true}.
     *
     * @param name the config source name
     * @param propertyMap the map to use
     * @param copy {@code true} to copy the given map, {@code false} otherwise
     */
    public MapBackedConfigSource(String name, Map<String, String> propertyMap, boolean copy) {
        this(name, propertyMap, ConfigSource.DEFAULT_ORDINAL, copy);
    }

    /**
     * Construct a new instance. The config source will use the given default ordinal, and
     * will use the given map as-is (not a copy of it).
     *
     * @param name the config source name
     * @param propertyMap the map to use
     * @param defaultOrdinal the default ordinal to use if one is not given in the map
     */
    public MapBackedConfigSource(String name, Map<String, String> propertyMap, int defaultOrdinal) {
        super(name, ConfigSourceUtil.getOrdinalFromMap(propertyMap, defaultOrdinal));
        properties = Collections.unmodifiableMap(propertyMap);
    }

    /**
     * Construct a new instance. The config source will use the given default ordinal, and
     * will use a copy of the given map if {@code copy} is {@code true}.
     *
     * @param name the config source name
     * @param propertyMap the map to use
     * @param defaultOrdinal the default ordinal to use if one is not given in the map
     * @param copy {@code true} to copy the given map, {@code false} otherwise
     */
    public MapBackedConfigSource(String name, Map<String, String> propertyMap, int defaultOrdinal, boolean copy) {
        this(name, copy ? new LinkedHashMap<>(propertyMap) : propertyMap, defaultOrdinal);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }
}
