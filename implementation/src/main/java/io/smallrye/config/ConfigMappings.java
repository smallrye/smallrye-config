package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.smallrye.config.ConfigMappingInterface.Property;

/**
 * Utility class for {@link ConfigMapping} annotated classes.
 */
public final class ConfigMappings {

    /**
     * Registers additional {@link ConfigMapping} annotated classes with a {@link SmallRyeConfig} instance.
     * <p>
     * The recommended method of registering {@link ConfigMapping} is with a
     * {@link SmallRyeConfigBuilder#withMapping(Class, String)}. In certain cases, this is not possible (ex. a CDI
     * runtime), where mapping classes can only be discovered after the <code>Config</code> instance creation.
     *
     * @param config the {@link SmallRyeConfig} instance
     * @param configClasses a <code>Set</code> of {@link ConfigMapping} annotated classes with prefixes
     * @throws ConfigValidationException if a {@link ConfigMapping} cannot be registed with the
     *         {@link SmallRyeConfig} instance
     */
    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(config, new SmallRyeConfigBuilder(), configClasses);
        }
    }

    /**
     * Registers additional <code>ConfigProperties</code> annotated classes with a {@link SmallRyeConfig} instance.
     * <p>
     * The recommended method of registering <code>ConfigProperties</code> is with a
     * {@link SmallRyeConfigBuilder#withMapping(Class, String)}. In certain cases, this is not possible (ex. a CDI
     * runtime), where mapping classes can only be discovered after the <code>Config</code> instance creation.
     *
     * @param config the {@link SmallRyeConfig} instance
     * @param configClasses a <code>Set</code> of <code>ConfigProperties</code> annotated classes with prefixes
     * @throws ConfigValidationException if a <code>ConfigProperties</code> cannot be registed with the
     *         {@link SmallRyeConfig} instance
     */
    public static void registerConfigProperties(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
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
    public static Map<String, Property> getProperties(final ConfigClassWithPrefix configClass) {
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
    public static Set<String> mappedProperties(final ConfigClassWithPrefix configClass, final Set<String> properties) {
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
            final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        for (ConfigClassWithPrefix configClass : configClasses) {
            configBuilder.withMapping(configClass.getKlass(), configClass.getPrefix());
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

    static String getPrefix(Class<?> type) {
        final ConfigMapping configMapping = type.getAnnotation(ConfigMapping.class);
        return configMapping != null ? configMapping.prefix() : "";
    }

    /**
     * A representation of a {@link ConfigMapping} or <code>@ConfigProperties</code> with a <code>Class</code> and the
     * prefix.
     */
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

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConfigClassWithPrefix that = (ConfigClassWithPrefix) o;
            return klass.equals(that.klass) && prefix.equals(that.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(klass, prefix);
        }

        public static ConfigClassWithPrefix configClassWithPrefix(final Class<?> klass, final String prefix) {
            return new ConfigClassWithPrefix(klass, prefix);
        }

        public static ConfigClassWithPrefix configClassWithPrefix(final Class<?> klass) {
            return configClassWithPrefix(klass, ConfigMappings.getPrefix(klass));
        }
    }
}
