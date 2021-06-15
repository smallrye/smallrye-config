package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static java.lang.Boolean.TRUE;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
Ê
import org.eclipse.microprofile.config.inject.ConfigProperties;

public final class ConfigMappings implements Serializable {
    private static final long serialVersionUID = -7790784345796818526L;

    private final ConfigValidator configValidator;
    private final ConcurrentMap<Class<?>, Map<String, ConfigMappingObject>> mappings;

    ConfigMappings(final ConfigValidator configValidator) {
        this.configValidator = configValidator;
        this.mappings = new ConcurrentHashMap<>();
    }

    void registerConfigMappings(final Map<Class<?>, Map<String, ConfigMappingObject>> mappings) {
        this.mappings.putAll(mappings);
    }

    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            Boolean validateUnknown = config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, Boolean.class)
                    .orElse(TRUE);
            mapConfiguration(ConfigMappingProvider.builder().validateUnknown(validateUnknown), config, configClasses);
        }
    }

    public static void registerConfigProperties(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(ConfigMappingProvider.builder().validateUnknown(false), config, configClasses);
        }
    }

    static void mapConfiguration(
            final ConfigMappingProvider.Builder builder,
            final SmallRyeConfig config,
            final Set<ConfigClassWithPrefix> configClasses) throws ConfigValidationException {
        for (ConfigClassWithPrefix configClass : configClasses) {
            builder.addRoot(configClass.getPrefix(), configClass.getKlass());
        }
        builder.build().mapConfiguration(config);
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

        Object value = configMappingObject;
        if (configMappingObject instanceof ConfigMappingClassMapper) {
            value = ((ConfigMappingClassMapper) configMappingObject).map();
        }

        configValidator.validateMapping(type, prefix, value);

        return type.cast(value);
    }

    static String getPrefix(Class<?> type) {
        final ConfigMapping configMapping = type.getAnnotation(ConfigMapping.class);
        return configMapping != null ? configMapping.prefix() : "";
    }

    public static final class ConfigClassWithPrefix {
        private final Class<?> klass;
        private final String prefix;

        public ConfigClassWithPrefix(final Class<?> klass, final String prefix) {
            this.klass = klass;
            this.prefix = prefix;
        }

        public Class<?> getKlass() {
            return klass;
        }

        public String getPrefix() {
            return prefix;
        }

        public static ConfigClassWithPrefix configClassWithPrefix(final Class<?> klass, final String prefix) {
            return new ConfigClassWithPrefix(klass, prefix);
        }
    }
}
