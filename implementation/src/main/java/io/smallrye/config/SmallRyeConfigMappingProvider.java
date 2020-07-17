package io.smallrye.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.mapper.ConfigMappingProvider;
import io.smallrye.config.mapper.ConfigurationObject;

public class SmallRyeConfigMappingProvider implements Serializable {
    private static final long serialVersionUID = -7807593615345914368L;

    private final ConfigMappingProvider configMappingProvider;
    private final Map<Class<?>, Map<String, ConfigurationObject>> rootsMap = new HashMap<>();

    public SmallRyeConfigMappingProvider(final ConfigMappingProvider configMappingProvider) {
        this.configMappingProvider = configMappingProvider;
    }

    public KeyMap<String> getDefaultValues() {
        return configMappingProvider.getDefaultValues();
    }

    public void mapConfiguration(final SmallRyeConfig config) throws ConfigValidationException {
        final ConfigMappingProvider.Result result = configMappingProvider.mapConfiguration(config);
        rootsMap.putAll(result.getRootsMap());
    }

    public <T> T getConfigMapping(Class<T> klass) {
        return getConfigMapping(klass, "");
    }

    public <T> T getConfigMapping(Class<T> klass, String prefix) {
        return klass.cast(rootsMap.getOrDefault(klass, Collections.emptyMap()).get(prefix));
    }
}
