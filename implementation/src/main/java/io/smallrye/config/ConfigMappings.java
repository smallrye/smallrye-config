package io.smallrye.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.ConfigSource;

public final class ConfigMappings implements Serializable {
    private static final long serialVersionUID = -7790784345796818526L;

    private final Map<Class<?>, Map<String, ConfigMappingObject>> mappings;

    ConfigMappings() {
        this(new HashMap<>());
    }

    ConfigMappings(final Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings = mappings;
    }

    void registerConfigMappings(Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings.putAll(mappings);
    }

    public void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigMappingWithPrefix> mappings)
            throws ConfigValidationException {
        final ConfigMappingProvider.Builder builder = ConfigMappingProvider.builder().validateUnknown(false);
        for (ConfigMappingWithPrefix mapping : mappings) {
            builder.addRoot(mapping.getPrefix(), mapping.getKlass());
        }
        final ConfigMappingProvider mappingProvider = builder.build();

        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource instanceof DefaultValuesConfigSource) {
                final DefaultValuesConfigSource defaultValuesConfigSource = (DefaultValuesConfigSource) configSource;
                defaultValuesConfigSource.registerDefaults(mappingProvider.getDefaultValues());
            }
        }

        mappingProvider.mapConfiguration(config, this);
    }

    <T> T getConfigMapping(Class<T> type) {
        final String prefix = Optional.ofNullable(type.getAnnotation(ConfigMapping.class))
                .map(ConfigMapping::prefix)
                .orElseGet(() -> Optional.ofNullable(type.getAnnotation(ConfigProperties.class)).map(ConfigProperties::prefix)
                        .orElse(""));

        return getConfigMapping(type, prefix);
    }

    <T> T getConfigMapping(Class<T> type, String prefix) {
        if (prefix == null) {
            return getConfigMapping(type);
        }

        final ConfigMappingObject configMappingObject = mappings
                .getOrDefault(ConfigMappingClass.toInterface(type), Collections.emptyMap()).get(prefix);
        if (configMappingObject == null) {
            throw ConfigMessages.msg.mappingNotFound(type.getName(), prefix);
        }

        if (configMappingObject instanceof ConfigMappingClassMapper) {
            return type.cast(((ConfigMappingClassMapper) configMappingObject).map());
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
