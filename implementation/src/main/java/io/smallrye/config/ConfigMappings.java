package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static java.lang.Boolean.TRUE;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.Property;

public final class ConfigMappings implements Serializable {
    private static final long serialVersionUID = -7790784345796818526L;

    public static void registerConfigMappings(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            Boolean validateUnknown = config.getOptionalValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, Boolean.class)
                    .orElse(TRUE);
            mapConfiguration(config, ConfigMappingProvider.builder().validateUnknown(validateUnknown), configClasses);
        }
    }

    public static void registerConfigProperties(final SmallRyeConfig config, final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        if (!configClasses.isEmpty()) {
            mapConfiguration(config, ConfigMappingProvider.builder().validateUnknown(false), configClasses);
        }
    }

    public static Map<Class<?>, Map<String, Map<String, Property>>> getProperties(final ConfigClassWithPrefix configClass) {
        Map<Class<?>, Map<String, Map<String, Property>>> properties = new HashMap<>();
        Function<String, String> path = new Function<>() {
            @Override
            public String apply(final String path) {
                return configClass.getPrefix().isEmpty() && !path.isEmpty() ? path.substring(1)
                        : configClass.getPrefix() + path;
            }
        };
        ConfigMappingInterface configMapping = ConfigMappingLoader.getConfigMapping(configClass.getKlass());
        for (ConfigMappingInterface superType : configMapping.getSuperTypes()) {
            getProperties(new GroupProperty(null, null, superType), configMapping.getNamingStrategy(), path, properties);
        }
        getProperties(new GroupProperty(null, null, configMapping), configMapping.getNamingStrategy(), path, properties);
        return properties;
    }

    public static Map<String, Map<String, Set<String>>> getNames(final ConfigClassWithPrefix configClass) {
        Map<String, Map<String, Set<String>>> names = new HashMap<>();
        Map<Class<?>, Map<String, Map<String, Property>>> properties = getProperties(configClass);
        for (Map.Entry<Class<?>, Map<String, Map<String, Property>>> entry : properties.entrySet()) {
            Map<String, Set<String>> groups = new HashMap<>();
            for (Map.Entry<String, Map<String, Property>> group : entry.getValue().entrySet()) {
                groups.put(group.getKey(), group.getValue().keySet());
            }
            names.put(entry.getKey().getName(), groups);
        }
        return names;
    }

    public static Set<String> getKeys(final ConfigClassWithPrefix configClass) {
        return getProperties(configClass).get(configClass.getKlass()).get(configClass.getPrefix()).keySet();
    }

    public static Map<String, String> getDefaults(final ConfigClassWithPrefix configClass) {
        Function<String, String> path = new Function<>() {
            @Override
            public String apply(final String path) {
                return configClass.getPrefix().isEmpty() && !path.isEmpty() ? path.substring(1)
                        : configClass.getPrefix() + path;
            }
        };
        ConfigMappingInterface configMapping = ConfigMappingLoader.getConfigMapping(configClass.getKlass());
        Map<String, String> defaults = new HashMap<>();

        for (ConfigMappingInterface superType : configMapping.getSuperTypes()) {
            Map<Class<?>, Map<String, Map<String, Property>>> properties = new HashMap<>();
            getProperties(new GroupProperty(null, null, superType), configMapping.getNamingStrategy(), path, properties);
            for (Map.Entry<Class<?>, Map<String, Map<String, Property>>> mappingEntry : properties.entrySet()) {
                for (Map.Entry<String, Map<String, Property>> prefixEntry : mappingEntry.getValue().entrySet()) {
                    for (Map.Entry<String, Property> propertyEntry : prefixEntry.getValue().entrySet()) {
                        if (propertyEntry.getValue().hasDefaultValue()) {
                            defaults.put(propertyEntry.getKey(), propertyEntry.getValue().getDefaultValue());
                        }
                    }
                }
            }
        }

        Map<Class<?>, Map<String, Map<String, Property>>> properties = getProperties(configClass);
        for (Map.Entry<String, Property> entry : properties.get(configClass.getKlass()).get(configClass.getPrefix())
                .entrySet()) {
            if (entry.getValue().hasDefaultValue()) {
                defaults.put(entry.getKey(), entry.getValue().getDefaultValue());
            }
        }
        return defaults;
    }

    public static Set<String> mappedProperties(final ConfigClassWithPrefix configClass, final Set<String> properties) {
        Set<PropertyName> names = new ConfigMappingNames(getNames(configClass))
                .get(configClass.getKlass().getName(), configClass.getPrefix());
        Set<String> mappedProperties = new HashSet<>();
        for (String property : properties) {
            if (names.contains(new PropertyName(property))) {
                mappedProperties.add(property);
            }
        }
        return mappedProperties;
    }

    private static void mapConfiguration(
            final SmallRyeConfig config,
            final ConfigMappingProvider.Builder builder,
            final Set<ConfigClassWithPrefix> configClasses)
            throws ConfigValidationException {
        DefaultValuesConfigSource defaultValues = (DefaultValuesConfigSource) config.getDefaultValues();
        for (ConfigClassWithPrefix configClass : configClasses) {
            builder.addRoot(configClass.getPrefix(), configClass.getKlass());
            defaultValues.addDefaults(
                    getDefaults(configClassWithPrefix(getConfigMappingClass(configClass.getKlass()), configClass.getPrefix())));
        }
        config.getMappings().putAll(builder.build().mapConfiguration(config));
    }

    private static void getProperties(
            final GroupProperty groupProperty,
            final NamingStrategy namingStrategy,
            final Function<String, String> path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties) {

        ConfigMappingInterface groupType = groupProperty.getGroupType();
        Map<String, Property> groupProperties = properties
                .computeIfAbsent(groupType.getInterfaceType(), group -> new HashMap<>())
                .computeIfAbsent(path.apply(""), s -> new HashMap<>());

        getProperties(groupProperty, namingStrategy, path, properties, groupProperties);
    }

    private static void getProperties(
            final GroupProperty groupProperty,
            final NamingStrategy namingStrategy,
            final Function<String, String> path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties,
            final Map<String, Property> groupProperties) {

        for (Property property : groupProperty.getGroupType().getProperties()) {
            getProperty(property, namingStrategy, path, properties, groupProperties);
        }
    }

    private static void getProperty(
            final Property property,
            final NamingStrategy namingStrategy,
            final Function<String, String> path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties,
            final Map<String, Property> groupProperties) {

        if (property.isLeaf()) {
            groupProperties.put(
                    path.apply(property.isParentPropertyName() ? "" : "." + property.getPropertyName(namingStrategy)),
                    property);
        } else if (property.isPrimitive()) {
            groupProperties.put(
                    path.apply(property.isParentPropertyName() ? "" : "." + property.getPropertyName(namingStrategy)),
                    property);
        } else if (property.isGroup()) {
            GroupProperty groupProperty = property.asGroup();
            NamingStrategy groupNamingStrategy = groupProperty.hasNamingStrategy() ? groupProperty.getNamingStrategy()
                    : namingStrategy;
            Function<String, String> groupPath = new Function<>() {
                @Override
                public String apply(final String name) {
                    return property.isParentPropertyName() ? path.apply("") + name
                            : path.apply("." + property.getPropertyName(namingStrategy)) + name;
                }
            };
            getProperties(groupProperty, groupNamingStrategy, groupPath, properties);
            getProperties(groupProperty, groupNamingStrategy, groupPath, properties, groupProperties);
        } else if (property.isMap()) {
            ConfigMappingInterface.MapProperty mapProperty = property.asMap();
            if (mapProperty.getValueProperty().isLeaf()) {
                groupProperties.put(property.isParentPropertyName() ? path.apply(".*")
                        : path.apply("." + property.getPropertyName(namingStrategy) + ".*"), mapProperty);
                if (mapProperty.hasKeyUnnamed()) {
                    groupProperties.put(property.isParentPropertyName() ? path.apply("")
                            : path.apply("." + property.getPropertyName(namingStrategy)), mapProperty);
                }
            } else if (mapProperty.getValueProperty().isGroup()) {
                GroupProperty groupProperty = mapProperty.getValueProperty().asGroup();
                NamingStrategy groupNamingStrategy = groupProperty.hasNamingStrategy() ? groupProperty.getNamingStrategy()
                        : namingStrategy;
                Function<String, String> groupPath = new Function<>() {
                    @Override
                    public String apply(final String name) {
                        return property.isParentPropertyName() ? path.apply(".*") + name
                                : path.apply("." + mapProperty.getPropertyName(namingStrategy) + ".*") + name;
                    }
                };
                getProperties(groupProperty, groupNamingStrategy, groupPath, properties);
                getProperties(groupProperty, groupNamingStrategy, groupPath, properties, groupProperties);
                if (mapProperty.hasKeyUnnamed()) {
                    Function<String, String> unnamedGroupPath = new Function<>() {
                        @Override
                        public String apply(final String name) {
                            return property.isParentPropertyName() ? path.apply(name)
                                    : path.apply("." + mapProperty.getPropertyName(namingStrategy)) + name;
                        }
                    };
                    getProperties(groupProperty, groupNamingStrategy, unnamedGroupPath, properties);
                    getProperties(groupProperty, groupNamingStrategy, unnamedGroupPath, properties, groupProperties);
                }
            } else if (mapProperty.getValueProperty().isCollection()) {
                CollectionProperty collectionProperty = mapProperty.getValueProperty().asCollection();
                Property element = collectionProperty.getElement();
                if (element.isLeaf()) {
                    LeafProperty leafProperty = new LeafProperty(element.getMethod(), element.getPropertyName(),
                            element.asLeaf().getValueType(), element.asLeaf().getConvertWith(), null);
                    getProperty(leafProperty, namingStrategy, new Function<String, String>() {
                        @Override
                        public String apply(final String name) {
                            return path.apply(name + ".*");
                        }
                    }, properties, groupProperties);
                }
                getProperty(element, namingStrategy, new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        return path.apply(name + ".*[*]");
                    }
                }, properties, groupProperties);
                if (mapProperty.hasKeyUnnamed()) {
                    getProperty(element, namingStrategy, new Function<String, String>() {
                        @Override
                        public String apply(final String name) {
                            return path.apply(name + "[*]");
                        }
                    }, properties, groupProperties);
                }
            } else if (mapProperty.getValueProperty().isMap()) {
                getProperty(mapProperty.getValueProperty(), namingStrategy,
                        new Function<String, String>() {
                            @Override
                            public String apply(final String name) {
                                return path.apply(name + ".*");
                            }
                        }, properties, groupProperties);
                if (mapProperty.hasKeyUnnamed()) {
                    getProperty(mapProperty.getValueProperty(), namingStrategy,
                            new Function<String, String>() {
                                @Override
                                public String apply(final String name) {
                                    return path.apply(name);
                                }
                            }, properties, groupProperties);
                }
            }
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            if (collectionProperty.getElement().isLeaf()) {
                getProperty(collectionProperty.getElement(), namingStrategy, path, properties, groupProperties);
            }
            getProperty(collectionProperty.getElement(), namingStrategy, new Function<String, String>() {
                @Override
                public String apply(final String name) {
                    return path.apply(name.endsWith(".*") ? name.substring(0, name.length() - 2) + "[*].*" : name + "[*]");
                }
            }, properties, groupProperties);
        } else if (property.isOptional()) {
            getProperty(property.asOptional().getNestedProperty(), namingStrategy, path, properties, groupProperties);
        }
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
