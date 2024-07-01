package io.smallrye.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

public abstract class MapBackedConfigValueConfigSource extends AbstractConfigSource implements ConfigValueConfigSource {
    private static final long serialVersionUID = -4619155951589529987L;

    private final Map<String, ConfigValue> properties;

    /**
     * Construct a new instance. The config source will use a default ordinal of {@code 100} and
     * will use a copy of the given map.
     *
     * @param name the config source name
     * @param propertyMap the map to use
     */
    public MapBackedConfigValueConfigSource(String name, Map<String, ConfigValue> propertyMap) {
        this(name, propertyMap, ConfigSource.DEFAULT_ORDINAL);
    }

    /**
     * Construct a new instance. The config source will use the given default ordinal, and
     * will use a copy of the given map.
     *
     * @param name the config source name
     * @param propertyMap the map to use
     * @param defaultOrdinal the default ordinal to use if one is not given in the map
     */
    public MapBackedConfigValueConfigSource(String name, Map<String, ConfigValue> propertyMap, int defaultOrdinal) {
        super(name, ConfigSourceUtil.getOrdinalFromMap(new ConfigValueMapView(propertyMap), defaultOrdinal));
        this.properties = new HashMap<>();
        for (Map.Entry<String, ConfigValue> entry : propertyMap.entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue().from().withConfigSourceOrdinal(getOrdinal()).build());
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public ConfigValue getConfigValue(final String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public Map<String, ConfigValue> getConfigValueProperties() {
        return properties;
    }
}
