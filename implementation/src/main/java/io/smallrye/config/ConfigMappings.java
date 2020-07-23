package io.smallrye.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ConfigMappings implements Serializable {
    private static final long serialVersionUID = -7790784345796818526L;

    private final Map<Class<?>, Map<String, ConfigMappingObject>> mappings;

    public ConfigMappings() {
        this(new HashMap<>());
    }

    ConfigMappings(final Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings = mappings;
    }

    void registerConfigMappings(Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings.putAll(mappings);
    }

    public Map<Class<?>, Map<String, ConfigMappingObject>> getMappings() {
        return mappings;
    }

    public <T> T getConfigMapping(Class<T> type, String prefix) {
        final ConfigMappingObject configMappingObject = mappings.getOrDefault(type, Collections.emptyMap()).get(prefix);
        if (configMappingObject == null) {
            throw ConfigMessages.msg.mappingNotFound(type.getName(), prefix);
        }
        return type.cast(configMappingObject);
    }
}
