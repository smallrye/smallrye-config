package io.smallrye.config;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public final class ConfigMappings implements Serializable {
    private static final long serialVersionUID = -7790784345796818526L;

    private final ConcurrentMap<Class<?>, Map<String, ConfigMappingObject>> mappings;

    ConfigMappings() {
        this.mappings = new ConcurrentHashMap<>();
    }

    void registerConfigMappings(final Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings.putAll(mappings);
    }

    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigMappingWithPrefix> mappings)
            throws ConfigValidationException {
        final ConfigMappingProvider.Builder builder = ConfigMappingProvider.builder();
        for (ConfigMappingWithPrefix mapping : mappings) {
            builder.addRoot(mapping.getPrefix(), mapping.getKlass());
        }

        registerConfigMappings(config, builder.build());
    }

    public static void registerConfigMappings(final SmallRyeConfig config, final ConfigMappingProvider mappingProvider)
            throws ConfigValidationException {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource instanceof DefaultValuesConfigSource) {
                final DefaultValuesConfigSource defaultValuesConfigSource = (DefaultValuesConfigSource) configSource;
                defaultValuesConfigSource.registerDefaults(mappingProvider.getDefaultValues());
            }
        }

        mappingProvider.mapConfiguration(config);
    }

    <T> T getConfigMapping(Class<T> type) {
        return getConfigMapping(type, getPrefix(type));
    }

    <T> T getConfigMapping(Class<T> type, String prefix) {
        if (prefix == null) {
            return getConfigMapping(type);
        }

        final Map<String, ConfigMappingObject> mappingsForType = mappings.get(type);
        if (mappingsForType == null) {
            throw ConfigMessages.msg.mappingNotFound(type.getName());
        }

        final ConfigMappingObject configMappingObject = mappingsForType.get(prefix);
        if (configMappingObject == null) {
            throw ConfigMessages.msg.mappingPrefixNotFound(type.getName(), prefix);
        }

        return type.cast(configMappingObject);
    }

    static String getPrefix(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(ConfigMapping.class)).map(ConfigMapping::prefix).orElse("");
    }

    public static final class ConfigMappingWithPrefix {
        private final Class<?> klass;
        private final String prefix;

        public ConfigMappingWithPrefix(final Class<?> klass, final String prefix) {
            this.klass = klass;
            this.prefix = prefix;
        }

        public Class<?> getKlass() {
            return klass;
        }

        public String getPrefix() {
            return prefix;
        }

        public static ConfigMappingWithPrefix configMappingWithPrefix(final Class<?> klass, final String prefix) {
            return new ConfigMappingWithPrefix(klass, prefix);
        }
    }
}
