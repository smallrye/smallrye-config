package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config._private.ConfigMessages;

/**
 * Utility class for {@link ConfigMapping} annotated classes.
 */
public final class ConfigMappings {

    /**
     * Registers additional {@link ConfigMapping} annotated classes with a {@link SmallRyeConfig} instance.
     * <p>
     * The recommended method of registering {@link ConfigMapping} is with a
     * {@link SmallRyeConfigBuilder#withMapping(Class)}. In certain cases, this is not possible (ex. a CDI
     * runtime), where mapping classes can only be discovered after the <code>Config</code> instance creation.
     *
     * @param config the {@link SmallRyeConfig} instance
     * @param configClasses a <code>Set</code> of {@link ConfigMapping} annotated classes with prefixes
     * @throws ConfigValidationException if a {@link ConfigMapping} cannot be registed with the
     *         {@link SmallRyeConfig} instance
     */
    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigClass> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(config, new SmallRyeConfigBuilder(), configClasses);
        }
    }

    /**
     * Registers additional <code>ConfigProperties</code> annotated classes with a {@link SmallRyeConfig} instance.
     * <p>
     * The recommended method of registering <code>ConfigProperties</code> is with a
     * {@link SmallRyeConfigBuilder#withMapping(Class)}. In certain cases, this is not possible (ex. a CDI
     * runtime), where mapping classes can only be discovered after the <code>Config</code> instance creation.
     *
     * @param config the {@link SmallRyeConfig} instance
     * @param configClasses a <code>Set</code> of <code>ConfigProperties</code> annotated classes with prefixes
     * @throws ConfigValidationException if a <code>ConfigProperties</code> cannot be registed with the
     *         {@link SmallRyeConfig} instance
     */
    public static void registerConfigProperties(final SmallRyeConfig config, final Set<ConfigClass> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(config, new SmallRyeConfigBuilder().withValidateUnknown(false), configClasses);
        }
    }

    /**
     * Constructs a representation of all {@link Property} contained in a mapping class. The <code>Map</code> key is
     * the full path to the {@link Property}, including the mapping class prefix.
     *
     * @param configClass the {@link ConfigMapping} annotated class and <code>String</code> prefix
     * @see ConfigMappingInterface#getProperties(ConfigMappingInterface)
     * @return a <code>Map</code> with all mapping class {@link Property}.
     */
    public static Map<String, Property> getProperties(final ConfigClass configClass) {
        Map<String, Property> properties = new HashMap<>();
        // Because the properties key names do not include the path prefix we need to add it
        for (Map.Entry<String, Property> entry : ConfigMappingInterface
                .getProperties(ConfigMappingLoader.getConfigMapping(configClass.getType()))
                .get(configClass.getType())
                .get("").entrySet()) {
            properties.put(prefix(configClass.getPrefix(), entry.getKey()), entry.getValue());
        }
        return properties;
    }

    private static void mapConfiguration(
            final SmallRyeConfig config,
            final SmallRyeConfigBuilder configBuilder,
            final Set<ConfigClass> configClasses)
            throws ConfigValidationException {
        for (ConfigClass configClass : configClasses) {
            configBuilder.withMapping(configClass);
        }
        config.getDefaultValues().addDefaults(configBuilder.getDefaultValues());
        config.getMappings().putAll(config.buildMappings(configBuilder));
    }

    static String prefix(final String prefix, final String path) {
        return prefix(prefix, path, new StringBuilder(prefix));
    }

    static String prefix(final String prefix, final String path, final StringBuilder sb) {
        if (prefix.isEmpty()) {
            return path;
        } else if (path.isEmpty()) {
            return prefix;
        } else if (path.charAt(0) == '[') {
            return sb.append(path).toString();
        } else {
            return sb.append(".").append(path).toString();
        }
    }

    /**
     * A representation of a {@link ConfigMapping} or <code>@ConfigProperties</code>.
     */
    public static final class ConfigClass {
        private final Class<?> type;
        private final String prefix;
        private final Map<String, String> properties;

        public ConfigClass(final Class<?> type, final String prefix) {
            Assert.checkNotNullParam("klass", type);
            Assert.checkNotNullParam("path", prefix);

            this.type = type;
            this.prefix = prefix;
            this.properties = new HashMap<>();

            Class<?> mappingClass = getConfigMappingClass(type);
            StringBuilder sb = new StringBuilder(prefix);
            for (Map.Entry<String, String> property : ConfigMappingLoader.configMappingProperties(mappingClass).entrySet()) {
                String path = property.getKey();
                String name;
                if (prefix.isEmpty()) {
                    name = path;
                } else if (path.isEmpty()) {
                    name = prefix;
                } else if (path.charAt(0) == '[') {
                    name = sb.append(path).toString();
                } else {
                    name = sb.append(".").append(path).toString();
                }
                properties.put(name, property.getValue());
                sb.setLength(prefix.length());
            }
        }

        @Deprecated(forRemoval = true)
        public Class<?> getKlass() {
            return type;
        }

        public Class<?> getType() {
            return type;
        }

        public String getPrefix() {
            return prefix;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConfigClass that = (ConfigClass) o;
            return type.equals(that.type) && prefix.equals(that.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, prefix);
        }

        public static ConfigClass configClass(final Class<?> klass, final String prefix) {
            return new ConfigClass(klass, prefix);
        }

        public static ConfigClass configClass(final Class<?> klass) {
            if (!klass.isInterface() && klass.isAnnotationPresent(ConfigMapping.class)) {
                throw ConfigMessages.msg.mappingAnnotationNotSupportedInClass(klass);
            }

            if (klass.isInterface() && klass.isAnnotationPresent(ConfigProperties.class)) {
                throw ConfigMessages.msg.propertiesAnnotationNotSupportedInInterface(klass);
            }

            if (klass.isInterface()) {
                ConfigMapping configMapping = klass.getAnnotation(ConfigMapping.class);
                String prefix = configMapping != null ? configMapping.prefix() : "";
                return configClass(klass, prefix);
            } else {
                ConfigProperties configProperties = klass.getAnnotation(ConfigProperties.class);
                String prefix = configProperties != null ? configProperties.prefix() : "";
                if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
                    prefix = "";
                }
                return configClass(klass, prefix);
            }
        }
    }
}
