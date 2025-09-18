package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.configMappingProperties;
import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.Converters.newSecretConverter;
import static io.smallrye.config.common.utils.StringUtil.unindexed;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfigBuilder.MappingBuilder;
import io.smallrye.config._private.ConfigMessages;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class ConfigMappingContext {
    private final SmallRyeConfig config;
    private final Map<Class<?>, Map<String, Object>> mappings = new IdentityHashMap<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();

    private NamingStrategy namingStrategy = NamingStrategy.KEBAB_CASE;
    private boolean beanStyleGetters = false;
    private final StringBuilder nameBuilder = new StringBuilder();
    private final Set<String> usedProperties = new HashSet<>();
    private final List<Problem> problems = new ArrayList<>();

    public ConfigMappingContext(
            final SmallRyeConfig config,
            final MappingBuilder mappingBuilder) {

        this.config = config;

        for (Map.Entry<ConfigClass, Object> entry : mappingBuilder.getMappingsInstances().entrySet()) {
            Class<?> type = getConfigMappingClass(entry.getKey().getType());
            String prefix = entry.getKey().getPrefix();
            Object instance = entry.getValue();
            this.mappings.computeIfAbsent(type, k -> new HashMap<>(4)).put(prefix, instance);
        }

        for (ConfigClass configClass : mappingBuilder.getMappings()) {
            Class<?> type = getConfigMappingClass(configClass.getType());
            String prefix = configClass.getPrefix();
            applyPrefix(configClass.getPrefix());
            Object instance = constructMapping(type, prefix);
            this.mappings.computeIfAbsent(type, k -> new HashMap<>(4)).put(prefix, instance);
        }
    }

    @SuppressWarnings("unchecked")
    <T> T constructMapping(Class<T> interfaceType, String prefix) {
        int problemsCount = problems.size();
        Object mappingObject = constructGroup(interfaceType);
        if (problemsCount != problems.size()) {
            return (T) mappingObject;
        }
        try {
            if (mappingObject instanceof ConfigMappingClassMapper) {
                mappingObject = ((ConfigMappingClassMapper) mappingObject).map();
                config.getConfigValidator().validateMapping(mappingObject.getClass(), prefix, mappingObject);
            } else {
                config.getConfigValidator().validateMapping(interfaceType, prefix, mappingObject);
            }
        } catch (ConfigValidationException e) {
            problems.addAll(Arrays.asList(e.getProblems()));
        }
        return (T) mappingObject;
    }

    public <T> T constructGroup(Class<T> interfaceType) {
        NamingStrategy namingStrategy = this.namingStrategy;
        boolean beanStyleGetters = this.beanStyleGetters;
        T mappingObject = ConfigMappingLoader.configMappingObject(interfaceType, this);
        applyNamingStrategy(namingStrategy);
        applyBeanStyleGetters(beanStyleGetters);
        return mappingObject;
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getConverterInstance(Class<? extends Converter<? extends T>> converterType) {
        return (Converter<T>) converterInstances.computeIfAbsent(converterType, t -> {
            try {
                return (Converter<T>) t.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException | Error e2) {
                    throw e2;
                } catch (Throwable t2) {
                    throw new UndeclaredThrowableException(t2);
                }
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        });
    }

    public void applyPrefix(final String prefix) {
        this.nameBuilder.replace(0, nameBuilder.length(), prefix);
    }

    public void applyNamingStrategy(final NamingStrategy namingStrategy) {
        if (namingStrategy != null) {
            this.namingStrategy = namingStrategy;
        }
    }

    public void applyBeanStyleGetters(final Boolean beanStyleGetters) {
        if (beanStyleGetters != null) {
            this.beanStyleGetters = beanStyleGetters;
        }
    }

    private static final Function<String, String> BEAN_STYLE_GETTERS = new Function<String, String>() {
        @Override
        public String apply(final String name) {
            if (name.startsWith("get") && name.length() > 3) {
                return Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                return Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            return name;
        }
    };

    public Function<String, String> propertyName() {
        return beanStyleGetters ? BEAN_STYLE_GETTERS.andThen(namingStrategy) : namingStrategy;
    }

    public StringBuilder getNameBuilder() {
        return nameBuilder;
    }

    @SuppressWarnings("unused")
    public void reportProblem(RuntimeException problem) {
        problems.add(new Problem(problem.toString()));
    }

    List<Problem> getProblems() {
        return problems;
    }

    Map<Class<?>, Map<String, Object>> getMappings() {
        return mappings;
    }

    void reportUnknown(final Set<String> ignoredPaths) {
        Set<PropertyName> ignoredNames = new HashSet<>();
        Set<String> ignoredPrefixes = new HashSet<>();
        for (String ignoredPath : ignoredPaths) {
            if (ignoredPath.endsWith(".**")) {
                ignoredPrefixes.add(ignoredPath.substring(0, ignoredPath.length() - 3));
            } else {
                ignoredNames.add(new PropertyName(ignoredPath));
            }
        }

        Set<String> prefixes = new HashSet<>();
        for (Map<String, Object> value : this.mappings.values()) {
            prefixes.addAll(value.keySet());
        }
        if (prefixes.contains("")) {
            prefixes.clear();
        }

        propertyNames: for (String propertyName : config.getPropertyNames()) {
            if (usedProperties.contains(propertyName)) {
                continue;
            }

            if (ignoredNames.contains(new PropertyName(propertyName))) {
                continue;
            }

            for (String ignoredPrefix : ignoredPrefixes) {
                if (propertyName.startsWith(ignoredPrefix)) {
                    continue propertyNames;
                }
            }

            for (String prefix : prefixes) {
                if (isPropertyInRoot(propertyName, prefix)) {
                    ConfigValue configValue = config.getConfigValue(propertyName);
                    // TODO - https://github.com/quarkusio/quarkus/issues/38479
                    if (configValue.getSourceName() != null && configValue.getSourceName().startsWith(EnvConfigSource.NAME)) {
                        continue;
                    }
                    problems.add(new Problem(
                            ConfigMessages.msg.propertyDoesNotMapToAnyRoot(propertyName, configValue.getLocation())));
                }
            }
        }
    }

    private static boolean isPropertyInRoot(final String property, final String root) {
        if (property.equals(root)) {
            return true;
        }

        // if property is less than the root no way to match
        if (property.length() <= root.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char c = property.charAt(root.length());
        if ((c == '.') || c == '[') {
            return property.startsWith(root);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public class ObjectCreator<T> {
        private T root;
        private List<Consumer<Function<String, Object>>> creators;

        public ObjectCreator(final String path) {
            this.creators = List.of(new Consumer<Function<String, Object>>() {
                @Override
                public void accept(Function<String, Object> get) {
                    root = (T) get.apply(path);
                }
            });
        }

        public <K> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith) {
            return map(keyRawType, keyConvertWith, null, Collections.emptyList());
        }

        public <K> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey,
                final Iterable<String> keys) {
            return map(keyRawType, keyConvertWith, unnamedKey, keys, (Class<?>) null);
        }

        public <K, V> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey,
                final Iterable<String> keys,
                final Class<V> defaultClass) {

            Supplier<V> supplier = null;
            if (defaultClass != null) {
                supplier = new Supplier<V>() {
                    @Override
                    public V get() {
                        int length = nameBuilder.length();
                        nameBuilder.append(".*");
                        V defaultValue = constructGroup(defaultClass);
                        nameBuilder.setLength(length);
                        return defaultValue;
                    }
                };
            }

            return map(keyRawType, keyConvertWith, unnamedKey, keys, supplier);
        }

        public <K, V> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey,
                final Iterable<String> keys,
                final Supplier<V> defaultValue) {
            Converter<K> keyConverter = keyConvertWith == null ? config.requireConverter(keyRawType)
                    : getConverterInstance(keyConvertWith);
            List<Consumer<Function<String, Object>>> nestedCreators = new ArrayList<>();
            for (Consumer<Function<String, Object>> creator : this.creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String path) {
                        Map<K, V> map = defaultValue != null ? new MapWithDefault<>(defaultValue.get()) : new HashMap<>();

                        if (unnamedKey != null) {
                            nestedCreators.add(new Consumer<>() {
                                @Override
                                public void accept(Function<String, Object> get) {
                                    V value = (V) get.apply(path);
                                    if (value != null) {
                                        map.put(unnamedKey.isEmpty() ? null : keyConverter.convert(unnamedKey), value);
                                    }
                                }
                            });
                        }

                        // single map key with the path plus the map key
                        // the key is used in the resulting Map and the value in the nested creators to append nested elements paths
                        Map<String, String> mapKeys = new HashMap<>();
                        // single map key with all property names that share the same key
                        Map<String, List<String>> mapProperties = new HashMap<>();

                        if (keys != null) {
                            for (String key : keys) {
                                if (key.isEmpty()) {
                                    mapKeys.put(key, path);
                                } else {
                                    mapKeys.put(key, path + "." + quoted(key));
                                }
                            }
                        }
                        if (mapKeys.isEmpty()) {
                            for (String propertyName : config.getPropertyNames()) {
                                if (propertyName.length() > path.length() + 1 // only consider properties bigger than the map path
                                        && (path.isEmpty() || propertyName.charAt(path.length()) == '.') // next char must be a dot (for the key)
                                        && propertyName.startsWith(path)) { // the property must start with the map path

                                    // Start at the map root path
                                    NameIterator mapProperty = !path.isEmpty()
                                            ? new NameIterator(unindexed(propertyName), path.length())
                                            : new NameIterator(unindexed(propertyName));
                                    // Move to the next key
                                    mapProperty.next();

                                    String mapKey = unindexed(mapProperty.getPreviousSegment());
                                    mapKeys.computeIfAbsent(mapKey, new Function<String, String>() {
                                        @Override
                                        public String apply(final String s) {
                                            return unindexed(propertyName.substring(0, mapProperty.getPosition()));
                                        }
                                    });

                                    mapProperties.computeIfAbsent(mapKey, new Function<String, List<String>>() {
                                        @Override
                                        public List<String> apply(final String s) {
                                            return new ArrayList<>();
                                        }
                                    });
                                    mapProperties.get(mapKey).add(propertyName);
                                }
                            }
                        }

                        for (Map.Entry<String, String> mapKey : mapKeys.entrySet()) {
                            nestedCreators.add(new Consumer<>() {
                                @Override
                                public void accept(Function<String, Object> get) {
                                    // When we use the unnamed key empty and nested elements, we don't know if
                                    // properties reference a nested element name or a named key. Since unnamed key
                                    // creator runs first, we know which property names were used and skip those.
                                    if (unnamedKey != null && !unnamedKey.isEmpty() && !mapProperties.isEmpty()) {
                                        boolean allUsed = true;
                                        for (String mapProperty : mapProperties.get(mapKey.getKey())) {
                                            if (!usedProperties.contains(mapProperty)) {
                                                allUsed = false;
                                                break;
                                            }
                                        }
                                        if (allUsed) {
                                            return;
                                        }
                                    }

                                    // This is the full path plus the map key
                                    V value = (V) get.apply(mapKey.getValue());
                                    if (value != null) {
                                        map.put(keyConverter.convert(mapKey.getKey()), value);
                                    }
                                }
                            });

                        }

                        return map;
                    }
                });
            }
            this.creators = nestedCreators;
            return this;
        }

        public <V, C extends Collection<V>> ObjectCreator<T> collection(
                final Class<C> collectionRawType) {
            List<Consumer<Function<String, Object>>> nestedCreators = new ArrayList<>();
            IntFunction<Collection<?>> collectionFactory = createCollectionFactory(collectionRawType);
            for (Consumer<Function<String, Object>> creator : this.creators) {
                Collection<V> collection = (Collection<V>) collectionFactory.apply(0);
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String path) {
                        // This is ordered, so it shouldn't require a set by index
                        for (Integer index : config.getIndexedPropertiesIndexes(path)) {
                            nestedCreators.add(new Consumer<Function<String, Object>>() {
                                @Override
                                public void accept(final Function<String, Object> get) {
                                    collection.add((V) get.apply(path + "[" + index + "]"));
                                }
                            });
                        }
                        return collection;
                    }
                });
            }
            this.creators = nestedCreators;
            return this;
        }

        public <V, C extends Collection<V>> ObjectCreator<T> optionalCollection(
                final Class<C> collectionRawType) {
            List<Consumer<Function<String, Object>>> nestedCreators = new ArrayList<>();
            IntFunction<Collection<?>> collectionFactory = createCollectionFactory(collectionRawType);
            for (Consumer<Function<String, Object>> creator : this.creators) {
                Collection<V> collection = (Collection<V>) collectionFactory.apply(0);
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String path) {
                        // This is ordered, so it shouldn't require a set by index
                        List<Integer> indexes = config.getIndexedPropertiesIndexes(path);
                        for (Integer index : indexes) {
                            nestedCreators.add(new Consumer<Function<String, Object>>() {
                                @Override
                                public void accept(final Function<String, Object> get) {
                                    collection.add((V) get.apply(path + "[" + index + "]"));
                                }
                            });
                        }
                        return indexes.isEmpty() ? Optional.empty() : Optional.of(collection);
                    }
                });
            }
            this.creators = nestedCreators;
            return this;
        }

        public <G> ObjectCreator<T> group(final Class<G> groupType) {
            for (Consumer<Function<String, Object>> creator : this.creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public G apply(final String path) {
                        StringBuilder sb = ConfigMappingContext.this.getNameBuilder();
                        int length = sb.length();
                        sb.append(path, length, path.length());
                        G group = constructGroup(groupType);
                        sb.setLength(length);
                        return group;
                    }
                });
            }
            return this;
        }

        public <G> ObjectCreator<T> lazyGroup(final Class<G> groupType) {
            for (Consumer<Function<String, Object>> creator : this.creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public G apply(final String path) {
                        if (createRequired(groupType, path)) {
                            StringBuilder sb = ConfigMappingContext.this.getNameBuilder();
                            int length = sb.length();
                            sb.append(path, length, path.length());
                            G group = constructGroup(groupType);
                            sb.setLength(length);
                            return group;
                        } else {
                            return null;
                        }
                    }
                });
            }
            return this;
        }

        public <G> ObjectCreator<T> optionalGroup(final Class<G> groupType) {
            for (Consumer<Function<String, Object>> creator : this.creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Optional<G> apply(final String path) {
                        if (createRequired(groupType, path)) {
                            StringBuilder sb = ConfigMappingContext.this.getNameBuilder();
                            int length = sb.length();
                            sb.append(path, length, path.length());
                            G group = constructGroup(groupType);
                            sb.setLength(length);
                            return Optional.of(group);
                        } else {
                            return Optional.empty();
                        }
                    }
                });
            }
            return this;
        }

        public static <V> V value(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            return convertValue(context, propertyName, getConverter(context, valueRawType, valueConvertWith));
        }

        @SuppressWarnings("unused")
        public static <V> Secret<V> secretValue(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            Converter<Secret<V>> valueConverter = newSecretConverter(getConverter(context, valueRawType, valueConvertWith));
            return convertValue(context, propertyName, valueConverter);
        }

        private static <V> V convertValue(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<V> valueConverter) {
            context.usedProperties.add(propertyName);
            return context.config.getValue(propertyName, valueConverter);
        }

        public ObjectCreator<T> value(
                final Class<T> valueRawType,
                final Class<? extends Converter<T>> valueConvertWith) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String propertyName) {
                        return value(ConfigMappingContext.this, propertyName, valueRawType, valueConvertWith);
                    }
                });
            }
            return this;
        }

        public static <V> Optional<V> optionalValue(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            return convertOptionalValue(context, propertyName, getConverter(context, valueRawType, valueConvertWith));
        }

        @SuppressWarnings("unused")
        public static <V> Optional<Secret<V>> optionalSecretValue(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            Converter<Optional<Secret<V>>> valueConverter = Converters
                    .newOptionalConverter(newSecretConverter(getConverter(context, valueRawType, valueConvertWith)));
            return convertValue(context, propertyName, valueConverter);
        }

        private static <V> Optional<V> convertOptionalValue(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<V> valueConverter) {
            context.usedProperties.add(propertyName);
            return context.config.getOptionalValue(propertyName, valueConverter);
        }

        public <V> ObjectCreator<T> optionalValue(
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Optional<V> apply(final String propertyName) {
                        return optionalValue(ConfigMappingContext.this, propertyName, valueRawType, valueConvertWith);
                    }
                });
            }
            return this;
        }

        public static <V, C extends Collection<V>> C values(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            Converter<V> itemConverter = getConverter(context, itemRawType, itemConvertWith);
            return convertValues(context, propertyName, itemConverter, collectionRawType);
        }

        @SuppressWarnings("unused")
        public static <V, C extends Collection<Secret<V>>> C secretValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            Converter<Secret<V>> itemConverter = newSecretConverter(getConverter(context, itemRawType, itemConvertWith));
            return convertValues(context, propertyName, itemConverter, collectionRawType);
        }

        public static <V, C extends Collection<V>> C convertValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<V> itemConverter,
                final Class<C> collectionRawType) {
            context.usedProperties.add(propertyName);
            context.usedProperties.addAll(context.config.getIndexedProperties(propertyName));
            IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);
            return context.config.getValues(propertyName, itemConverter, collectionFactory);
        }

        public <V, C extends Collection<V>> ObjectCreator<T> values(
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String propertyName) {
                        return values(ConfigMappingContext.this, propertyName, itemRawType, itemConvertWith, collectionRawType);
                    }
                });
            }
            return this;
        }

        public static <V, C extends Collection<V>> Optional<C> optionalValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            Converter<V> itemConverter = getConverter(context, itemRawType, itemConvertWith);
            return convertOptionalValues(context, propertyName, itemConverter, collectionRawType);
        }

        @SuppressWarnings("unused")
        public static <V, C extends Collection<Secret<V>>> Optional<C> optionalSecretValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            Converter<Secret<V>> itemConverter = newSecretConverter(getConverter(context, itemRawType, itemConvertWith));
            return convertOptionalValues(context, propertyName, itemConverter, collectionRawType);
        }

        public static <V, C extends Collection<V>> Optional<C> convertOptionalValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<V> itemConverter,
                final Class<C> collectionRawType) {
            context.usedProperties.add(propertyName);
            context.usedProperties.addAll(context.config.getIndexedProperties(propertyName));
            IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);
            return context.config.getOptionalValues(propertyName, itemConverter, collectionFactory);
        }

        public <V, C extends Collection<V>> ObjectCreator<T> optionalValues(
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String propertyName) {
                        return optionalValues(ConfigMappingContext.this, propertyName, itemRawType, itemConvertWith,
                                collectionRawType);
                    }
                });
            }
            return this;
        }

        public static <K, V> Map<K, V> values(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Iterable<String> keys,
                final String defaultValue) {
            Converter<K> keyConverter = getConverter(context, keyRawType, keyConvertWith);
            Converter<V> valueConverter = getConverter(context, valueRawType, valueConvertWith);
            return convertValues(context, propertyName, keyConverter, valueConverter, keys, defaultValue);
        }

        @SuppressWarnings("unused")
        public static <K, V> Map<K, Secret<V>> secretValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Iterable<String> keys,
                final String defaultValue) {
            Converter<K> keyConverter = getConverter(context, keyRawType, keyConvertWith);
            Converter<Secret<V>> valueConverter = newSecretConverter(getConverter(context, valueRawType, valueConvertWith));
            return convertValues(context, propertyName, keyConverter, valueConverter, keys, defaultValue);
        }

        public static <K, V> Map<K, V> convertValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<K> keyConverter,
                final Converter<V> valueConverter,
                final Iterable<String> keys,
                final String defaultValue) {

            Map<String, String> mapKeys = new HashMap<>();
            if (keys != null) {
                for (String key : keys) {
                    mapKeys.put(key, propertyName + "." + quoted(key));
                }
            }
            if (mapKeys.isEmpty()) {
                mapKeys = context.config.getMapKeys(propertyName);
            }

            IntFunction<Map<K, V>> mapFactory;
            if (defaultValue != null) {
                mapFactory = new IntFunction<>() {
                    @Override
                    public Map<K, V> apply(final int value) {
                        return new MapWithDefault<>(valueConverter.convert(defaultValue));
                    }
                };
            } else {
                mapFactory = new IntFunction<Map<K, V>>() {
                    @Override
                    public Map<K, V> apply(final int size) {
                        return new HashMap<>(size);
                    }
                };
            }

            context.usedProperties.add(propertyName);
            context.usedProperties.addAll(mapKeys.values());
            return context.config.getMapValues(mapKeys, keyConverter, valueConverter, mapFactory);
        }

        public <K, V> ObjectCreator<T> values(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Iterable<String> keys,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String propertyName) {
                        return values(ConfigMappingContext.this, propertyName, keyRawType, keyConvertWith, valueRawType,
                                valueConvertWith, keys, defaultValue);
                    }
                });
            }
            return this;
        }

        public static <K, V, C extends Collection<V>> Map<K, C> values(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Class<C> collectionRawType,
                final Iterable<String> keys,
                final String defaultValue) {
            Converter<K> keyConverter = getConverter(context, keyRawType, keyConvertWith);
            Converter<V> valueConverter = getConverter(context, valueRawType, valueConvertWith);
            return convertValues(context, propertyName, keyConverter, valueConverter, collectionRawType, keys, defaultValue);
        }

        public static <K, V, C extends Collection<Secret<V>>> Map<K, C> secretValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Class<C> collectionRawType,
                final Iterable<String> keys,
                final String defaultValue) {
            Converter<K> keyConverter = getConverter(context, keyRawType, keyConvertWith);
            Converter<Secret<V>> valueConverter = newSecretConverter(getConverter(context, valueRawType, valueConvertWith));
            return convertValues(context, propertyName, keyConverter, valueConverter, collectionRawType, keys, defaultValue);
        }

        public static <K, V, C extends Collection<V>> Map<K, C> convertValues(
                final ConfigMappingContext context,
                final String propertyName,
                final Converter<K> keyConverter,
                final Converter<V> valueConverter,
                final Class<C> collectionRawType,
                final Iterable<String> keys,
                final String defaultValue) {

            Map<String, String> mapKeys = new HashMap<>();
            if (keys != null) {
                for (String key : keys) {
                    mapKeys.put(key, propertyName + "." + quoted(key));
                }
            }
            if (mapKeys.isEmpty()) {
                mapKeys = context.config.getMapIndexedKeys(propertyName);
            }

            IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);
            IntFunction<Map<K, C>> mapFactory;
            if (defaultValue != null) {
                mapFactory = new IntFunction<>() {
                    @Override
                    public Map<K, C> apply(final int value) {
                        return new MapWithDefault<>(
                                Converters.newCollectionConverter(valueConverter, collectionFactory)
                                        .convert(defaultValue));
                    }
                };
            } else {
                mapFactory = new IntFunction<Map<K, C>>() {
                    @Override
                    public Map<K, C> apply(final int size) {
                        return new HashMap<>(size);
                    }
                };
            }

            context.usedProperties.add(propertyName);
            context.usedProperties.addAll(mapKeys.values());
            // map keys can be indexed or unindexed, so we need to find which ones exist to mark them as used
            context.usedProperties.addAll(context.config.getMapKeys(propertyName).values());
            return context.config.getMapIndexedValues(mapKeys, keyConverter, valueConverter, mapFactory, collectionFactory);
        }

        public <K, V, C extends Collection<V>> ObjectCreator<T> values(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Class<C> collectionRawType,
                final Iterable<String> keys,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Object apply(final String propertyName) {
                        return values(ConfigMappingContext.this, propertyName, keyRawType, keyConvertWith, valueRawType,
                                valueConvertWith, collectionRawType, keys, defaultValue);
                    }
                });
            }
            return this;
        }

        public T get() {
            return root;
        }

        /**
         * Matches that at least one runtime configuration name is in relative path of a mapping class. This is
         * required to trigger the construction of lazy mapping objects like <code>Optional</code> or <code>Map</code>.
         *
         * @param groupType the class of the mapping
         * @param path the relative path to the mapping
         * @return <code>true</code> if a runtime config name exits in the mapping names or <code>false</code> otherwise
         */
        private <G> boolean createRequired(final Class<G> groupType, final String path) {
            List<String> candidates = new ArrayList<>();
            for (String name : config.getPropertyNames()) {
                if (name.startsWith(path)) {
                    String candidate = name.length() > path.length() && name.charAt(path.length()) == '.'
                            ? name.substring(path.length() + 1)
                            : name.substring(path.length());
                    if (namingStrategy.equals(NamingStrategy.KEBAB_CASE)) {
                        candidates.add(candidate);
                    } else {
                        candidates.add(NamingStrategy.KEBAB_CASE.apply(candidate));
                    }
                }
            }

            if (!candidates.isEmpty()) {
                Map<String, String> properties = configMappingProperties(groupType);
                for (String mappedProperty : properties.keySet()) {
                    for (String candidate : candidates) {
                        if (PropertyName.equals(candidate, mappedProperty)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private static <V> Converter<V> getConverter(final ConfigMappingContext context, final Class<V> rawType,
                final Class<? extends Converter<V>> convertWith) {
            return convertWith == null ? context.config.requireConverter(rawType) : context.getConverterInstance(convertWith);
        }

        private static IntFunction<Collection<?>> createCollectionFactory(final Class<?> type) {
            if (type == List.class) {
                return ArrayList::new;
            }

            if (type == Set.class) {
                return HashSet::new;
            }

            throw new IllegalArgumentException();
        }

        private static String quoted(final String key) {
            NameIterator keyIterator = new NameIterator(key);
            keyIterator.next();
            return keyIterator.hasNext() ? "\"" + key + "\"" : key;
        }
    }

    static class MapWithDefault<K, V> extends HashMap<K, V> {
        @Serial
        private static final long serialVersionUID = 1390928078837140814L;
        private final V defaultValue;

        MapWithDefault(final V defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public V get(final Object key) {
            return getOrDefault(key, defaultValue);
        }
    }
}
