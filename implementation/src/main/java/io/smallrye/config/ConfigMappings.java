package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingInterface.Property;

/**
 * Utility class for {@link ConfigMapping} annotated classes.
 */
public final class ConfigMappings {

    /**
     * Registers additional {@link ConfigClass} classes with a {@link SmallRyeConfig} instance.
     * <p>
     * The recommended method of registering {@link ConfigClass} is with a
     * {@link SmallRyeConfigBuilder#withMapping(Class)}. In certain cases, this is not possible (ex. a CDI
     * runtime), where config classes can only be discovered after the <code>Config</code> instance creation.
     *
     * @param config the {@link SmallRyeConfig} instance
     * @param configClasses a <code>Set</code> of {@link ConfigClass} classes with prefixes
     * @param validateUnknown if <code>true</code> it will validate that all configurations in {@link SmallRyeConfig}
     *        under the specified config classes prefixes have a matching property
     * @throws ConfigValidationException if a {@link ConfigClass} cannot be registered with the {@link SmallRyeConfig} instance
     */
    public static void registerConfigClasses(
            final SmallRyeConfig config,
            final Set<ConfigClass> configClasses,
            final boolean validateUnknown)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(config, new SmallRyeConfigBuilder().withValidateUnknown(validateUnknown), configClasses);
        }
    }

    /**
     * Constructs a representation of all {@link Property} contained in a mapping class. The <code>Map</code> key is
     * the full path to the {@link Property}, including the mapping class prefix.
     *
     * @param configClass the {@link ConfigMapping} annotated class and <code>String</code> prefix
     * @see ConfigMappingInterface#getProperties(ConfigMappingInterface)
     * @return a <code>Map</code> with all mapping class {@link Property}
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

    /**
     * Constructs a {@link PropertyNamesMatcher} with all the property names mapped by the specified list of mapping
     * classes.
     *
     * @param configClasses a list of {@link ConfigMapping} annotated classes
     * @return a {@link PropertyNamesMatcher} to match names mapped by the mapping classes
     */
    public static PropertyNamesMatcher<?> propertyNamesMatcher(final List<ConfigClass> configClasses) {
        PropertyNamesMatcher<?> matcher = new PropertyNamesMatcher<>();
        for (ConfigClass configClass : configClasses) {
            matcher.add(configClass.getProperties().keySet());
        }
        return matcher;
    }

    private static void mapConfiguration(
            final SmallRyeConfig config,
            final SmallRyeConfigBuilder configBuilder,
            final Set<ConfigClass> configClasses)
            throws ConfigValidationException {
        for (ConfigClass configClass : configClasses) {
            configBuilder.withMapping(configClass);
        }
        config.getDefaultValues().addDefaults(configBuilder.getDefaults());
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
     * A representation of a configuration class.
     */
    public static final class ConfigClass {
        private final Class<?> type;
        private final String prefix;
        private final Map<String, String> properties;
        private final Set<String> secrets;

        public ConfigClass(final Class<?> type, final String prefix) {
            Assert.checkNotNullParam("klass", type);
            Assert.checkNotNullParam("path", prefix);

            this.type = type;
            this.prefix = prefix;
            this.properties = new HashMap<>();
            this.secrets = new HashSet<>();

            Class<?> mappingClass = getConfigMappingClass(type);
            Set<String> secrets = ConfigMappingLoader.configMappingSecrets(mappingClass);
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
                this.properties.put(name, property.getValue());
                if (secrets.contains(property.getKey())) {
                    this.secrets.add(name);
                }
                sb.setLength(prefix.length());
            }
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

        public Set<String> getSecrets() {
            return secrets;
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
            ConfigMappingHandler configClassHandler = Handlers.find(klass);
            return configClass(klass, configClassHandler.getPrefix(klass));
        }
    }
}
