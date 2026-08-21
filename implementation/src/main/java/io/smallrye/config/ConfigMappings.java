package io.smallrye.config;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappingHandler.Handlers;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingLoader.ConfigClassImplementation;

/**
 * Utility class for Config classes.
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
     * @param configClass the Config class and <code>String</code> prefix
     * @return a <code>Map</code> with all mapping class {@link Property}
     */
    public static Map<String, Property> getProperties(final ConfigClass configClass) {
        Map<String, Property> properties = new HashMap<>();
        // Because the properties key names do not include the path prefix we need to add it
        for (Entry<String, Property> entry : ConfigMappingInterface
                .getProperties(configClass.implementation().getInterfaceType()).entrySet()) {
            properties.put(prefix(configClass.getPrefix(), entry.getKey()), entry.getValue());
        }
        return properties;
    }

    /**
     * Constructs a {@link PropertyNamesMatcher} with all the property names mapped by the specified list of mapping
     * classes.
     *
     * @param configClasses a list of Config classes
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
     * A representation of a Config class.
     * <p>
     * A {@code ConfigClass} pairs the config type (an interface annotated with
     * {@link ConfigMapping @ConfigMapping} or a concrete class) with the configuration prefix it maps to and the
     * handler responsible for its processing.
     * <p>
     * Property names and secret paths are lazily computed and cached on first access.
     */
    public static final class ConfigClass {
        private final Class<?> type;
        private final String prefix;
        private final ConfigMappingHandler handler;
        private final Function<ClassLoader, ConfigClass> forClassLoader;

        private volatile Properties properties;

        private ConfigClass(
                final Class<?> type,
                final String prefix,
                final ConfigMappingHandler handler,
                final Function<ClassLoader, ConfigClass> forClassLoader) {
            Assert.checkNotNullParam("type", type);
            Assert.checkNotNullParam("prefix", prefix);
            Assert.checkNotNullParam("handler", handler);

            this.type = type;
            this.prefix = prefix;
            this.handler = handler;
            this.forClassLoader = forClassLoader;
        }

        /**
         * Returns the configuration type.
         *
         * @return the configuration type
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Returns the configuration prefix that this type is mapped to.
         *
         * @return the configuration prefix
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Returns the {@link ConfigMappingHandler} responsible for processing this configuration type.
         *
         * @return the handler
         */
        public ConfigMappingHandler getHandler() {
            return handler;
        }

        /**
         * Returns property names mapped by this configuration type, keyed by name (prefixed with {@link #getPrefix()})
         * and with the mapped configuration.
         *
         * @return a Map of property names to defaults
         */
        public Map<String, String> getProperties() {
            return holder().properties();
        }

        /**
         * Returns the names that are marked as secrets. Names are prefixed with {@link #getPrefix()}.
         *
         * @return a Set of secret property names
         */
        public Set<String> getSecrets() {
            return holder().secrets();
        }

        ConfigClassImplementation implementation() {
            Handlers.register(type, handler);
            return ConfigClassImplementation.get(type);
        }

        ConfigClass forClassLoader(final ClassLoader classLoader) {
            return forClassLoader == null ? this : forClassLoader.apply(classLoader);
        }

        private Properties holder() {
            Properties p = properties;
            if (p == null) {
                synchronized (this) {
                    p = properties;
                    if (p == null) {
                        properties = p = new Properties();
                    }
                }
            }
            return p;
        }

        private final class Properties {
            private final Map<String, String> properties;
            private final Set<String> secrets;

            Properties() {
                Map<String, String> prefixedProperties = new HashMap<>();
                Set<String> prefixedSecrets = new HashSet<>();

                Map<String, String> properties = implementation().getProperties();
                Set<String> secrets = implementation().getSecrets();
                StringBuilder sb = new StringBuilder(prefix);
                for (Map.Entry<String, String> property : properties.entrySet()) {
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
                    prefixedProperties.put(name, property.getValue());
                    if (secrets.contains(property.getKey())) {
                        prefixedSecrets.add(name);
                    }
                    sb.setLength(prefix.length());
                }

                this.properties = unmodifiableMap(prefixedProperties);
                this.secrets = unmodifiableSet(prefixedSecrets);
            }

            Map<String, String> properties() {
                return properties;
            }

            Set<String> secrets() {
                return secrets;
            }
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
            // we are specifically hashing the class name here as Class doesn't implement hashCode and doesn't provide a stable hashCode implementation
            // we want this hashCode to be stable so that order in HashMap/HashSet is preserved - if added in same order
            // the equals implementation is still using Class so that it's ClassLoader aware
            return Objects.hash(type.getName(), prefix);
        }

        /**
         * Creates a {@code ConfigClass} for the given type, discovering the prefix and handler automatically.
         *
         * @param type the configuration type
         * @return a new {@code ConfigClass}
         */
        public static ConfigClass configClass(final Class<?> type) {
            ConfigMappingHandler handler = Handlers.find(type);
            return new ConfigClass(type, handler.getPrefix(type), handler, new Function<>() {
                @Override
                public ConfigClass apply(ClassLoader classLoader) {
                    ConfigMappingHandler handler = Handlers.find(type, classLoader);
                    return new ConfigClass(type, handler.getPrefix(type), handler, null);
                }
            });
        }

        /**
         * Creates a {@code ConfigClass} for the given type with an explicit prefix, discovering the handler
         * automatically.
         *
         * @param type the configuration type
         * @param prefix the configuration prefix
         * @return a new {@code ConfigClass}
         */
        public static ConfigClass configClass(final Class<?> type, final String prefix) {
            return new ConfigClass(type, prefix, Handlers.find(type), new Function<>() {
                @Override
                public ConfigClass apply(ClassLoader classLoader) {
                    ConfigMappingHandler handler = Handlers.find(type, classLoader);
                    return new ConfigClass(type, prefix, handler, null);
                }
            });
        }

        /**
         * Creates a {@code ConfigClass} for the given type with an explicit handler. The prefix is obtained
         * from the handler.
         *
         * @param type the configuration type
         * @param handler the handler responsible for processing this type
         * @return a new {@code ConfigClass}
         */
        public static ConfigClass configClass(final Class<?> type, final ConfigMappingHandler handler) {
            return new ConfigClass(type, handler.getPrefix(type), handler, null);
        }

        /**
         * Creates a {@code ConfigClass} for the given type with an explicit prefix and handler.
         *
         * @param type the configuration type
         * @param prefix the configuration prefix
         * @param handler the handler responsible for processing this type
         * @return a new {@code ConfigClass}
         */
        public static ConfigClass configClass(final Class<?> type, final String prefix, final ConfigMappingHandler handler) {
            return new ConfigClass(type, prefix, handler, null);
        }
    }
}
