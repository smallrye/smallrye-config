package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.config.ConfigMappingInterface.Property;

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
                .getProperties(ConfigMappingLoader.getConfigMapping(configClass.getKlass()))
                .get(configClass.getKlass())
                .get("").entrySet()) {
            properties.put(prefix(configClass.getPrefix(), entry.getKey()), entry.getValue());
        }
        return properties;
    }

    @Deprecated
    public static Set<String> mappedProperties(final ConfigClass configClass, final Set<String> properties) {
        ConfigMappingNames names = new ConfigMappingNames(
                ConfigMappingLoader.getConfigMapping(configClass.getKlass()).getNames());
        Set<String> mappedNames = new HashSet<>();
        for (String property : properties) {
            if (names.hasAnyName(configClass.getKlass().getName(), configClass.getPrefix(), configClass.getPrefix(),
                    Set.of(property))) {
                mappedNames.add(property);
            }
        }
        return mappedNames;
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
        if (prefix.isEmpty()) {
            return path;
        } else if (path.isEmpty()) {
            return prefix;
        } else if (path.charAt(0) == '[') {
            return prefix + path;
        } else {
            return prefix + "." + path;
        }
    }

    /**
     * A representation of a {@link ConfigMapping} or <code>@ConfigProperties</code>.
     */
    public static final class ConfigClass {
        private final Class<?> klass;
        private final String prefix;
        private final boolean validateUnknown;

        public ConfigClass(final Class<?> klass, final String prefix, final boolean validateUnknown) {
            this.klass = klass;
            this.prefix = prefix;
            this.validateUnknown = validateUnknown;
        }

        public Class<?> getKlass() {
            return klass;
        }

        public String getPrefix() {
            return prefix;
        }

        public boolean isValidateUnknown() {
            return validateUnknown;
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
            return klass.equals(that.klass) && prefix.equals(that.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(klass, prefix);
        }

        public static ConfigClass configClass(final Class<?> klass, final String prefix) {
            return new ConfigClass(klass, prefix, true);
        }

        public static ConfigClass configClass(final Class<?> klass, final String prefix, final boolean validateUnknown) {
            return new ConfigClass(klass, prefix, validateUnknown);
        }

        public static ConfigClass configClass(final Class<?> klass) {
            if (klass.isInterface()) {
                ConfigMapping configMapping = klass.getAnnotation(ConfigMapping.class);
                String prefix = configMapping != null ? configMapping.prefix() : "";
                boolean validateUnknown = configMapping == null || configMapping.validateUnknown();
                return configClass(klass, prefix, validateUnknown);
            } else {
                ConfigProperties configProperties = klass.getAnnotation(ConfigProperties.class);
                String prefix = configProperties != null ? configProperties.prefix() : "";
                if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
                    prefix = "";
                }
                return configClass(klass, prefix, false);
            }
        }
    }
}
