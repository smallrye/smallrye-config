package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.ConfigSource;

public final class ConfigMappings implements Serializable {
    public static final String VALIDATE_UNKNOWN = "smallrye.config.mapping.validate-unknown";

    private static final long serialVersionUID = -7790784345796818526L;

    private final ConcurrentMap<Class<?>, Map<String, ConfigMappingObject>> mappings;

    ConfigMappings() {
        this.mappings = new ConcurrentHashMap<>();
    }

    void registerConfigMappings(final Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        // If the @ConfigMapping or @ConfigProperties interface/class already exists in this.mappings then append the new Map<String, ConfigMappingObject> object to it.
        for (Class<?> c : mappings.keySet()) {
            if (this.mappings.containsKey(c)) {
                this.mappings.get(c).putAll(mappings.get(c));
            } else {
                this.mappings.put(c, mappings.get(c));
            }
        }
    }

    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigMappingWithPrefix> mappings)
            throws ConfigValidationException {
        final ConfigMappingProvider.Builder builder = ConfigMappingProvider.builder()
                .validateUnknown(config.getOptionalValue(VALIDATE_UNKNOWN, Boolean.class).orElse(Boolean.TRUE));
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

        final Map<String, ConfigMappingObject> mappingsForType = mappings.get(getConfigMappingClass(type));
        if (mappingsForType == null) {
            throw ConfigMessages.msg.mappingNotFound(type.getName());
        }

        final ConfigMappingObject configMappingObject = mappingsForType.get(prefix);
        if (configMappingObject == null) {
            throw ConfigMessages.msg.mappingPrefixNotFound(type.getName(), prefix);
        }

        if (configMappingObject instanceof ConfigMappingClassMapper) {
            return type.cast(((ConfigMappingClassMapper) configMappingObject).map());
        }

        return type.cast(configMappingObject);
    }

    static String getPrefix(Class<?> type) {
        final ConfigMapping configMapping = type.getAnnotation(ConfigMapping.class);
        return configMapping != null ? configMapping.prefix() : "";
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
