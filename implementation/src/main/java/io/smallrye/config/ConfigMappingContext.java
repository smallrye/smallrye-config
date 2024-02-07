package io.smallrye.config;

import static io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.common.utils.StringUtil.unindexed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
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
import io.smallrye.config._private.ConfigMessages;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class ConfigMappingContext {
    private final SmallRyeConfig config;
    private final Map<Class<?>, Map<String, ConfigMappingObject>> roots = new IdentityHashMap<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();

    private NamingStrategy namingStrategy;
    private final StringBuilder stringBuilder = new StringBuilder();
    private final Set<String> usedProperties = new HashSet<>();
    private final List<Problem> problems = new ArrayList<>();

    private final ConfigMappingNames names;

    public ConfigMappingContext(
            final SmallRyeConfig config,
            final Map<String, List<Class<?>>> roots,
            final Map<String, Map<String, Set<String>>> names) {

        this.config = config;
        this.names = new ConfigMappingNames(names);

        for (Map.Entry<String, List<Class<?>>> entry : roots.entrySet()) {
            String path = entry.getKey();
            for (Class<?> root : entry.getValue()) {
                registerRoot(root, path);
            }
        }
    }

    private void registerRoot(Class<?> rootType, String rootPath) {
        roots.computeIfAbsent(rootType, new Function<Class<?>, Map<String, ConfigMappingObject>>() {
            @Override
            public Map<String, ConfigMappingObject> apply(final Class<?> mapping) {
                return new HashMap<>();
            }
        }).computeIfAbsent(rootPath, new Function<String, ConfigMappingObject>() {
            @Override
            public ConfigMappingObject apply(final String path) {
                namingStrategy = null;
                stringBuilder.replace(0, stringBuilder.length(), rootPath);
                return (ConfigMappingObject) constructRoot(rootType);
            }
        });
    }

    public <T> T constructRoot(Class<T> interfaceType) {
        return constructGroup(interfaceType);
    }

    public <T> T constructGroup(Class<T> interfaceType) {
        NamingStrategy namingStrategy = this.namingStrategy;
        T mappingObject = ConfigMappingLoader.configMappingObject(interfaceType, this);
        this.namingStrategy = applyNamingStrategy(namingStrategy);
        return mappingObject;
    }

    @SuppressWarnings("unused")
    public <T> ObjectCreator<T> constructObject(String path) {
        return new ObjectCreator<>(path);
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

    public NamingStrategy applyNamingStrategy(NamingStrategy namingStrategy) {
        if (namingStrategy != null) {
            this.namingStrategy = namingStrategy;
        } else if (this.namingStrategy == null) {
            this.namingStrategy = NamingStrategy.KEBAB_CASE;
        }
        return this.namingStrategy;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    @SuppressWarnings("unused")
    public void reportProblem(RuntimeException problem) {
        problems.add(new Problem(problem.toString()));
    }

    List<Problem> getProblems() {
        return problems;
    }

    Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap() {
        return roots;
    }

    void reportUnknown(final List<String> ignoredPaths) {
        KeyMap<Boolean> ignoredProperties = new KeyMap<>();
        for (String ignoredPath : ignoredPaths) {
            KeyMap<Boolean> found;
            if (ignoredPath.endsWith(".**")) {
                found = ignoredProperties.findOrAdd(ignoredPath.substring(0, ignoredPath.length() - 3));
                found.putRootValue(Boolean.TRUE);
                ignoreRecursively(found);
            } else {
                if (!ignoredProperties.hasRootValue(ignoredPath)) {
                    found = ignoredProperties.findOrAdd(ignoredPath);
                    found.putRootValue(Boolean.TRUE);
                }
            }
        }

        Set<String> roots = new HashSet<>();
        for (Map<String, ConfigMappingObject> value : this.roots.values()) {
            roots.addAll(value.keySet());
        }

        for (String name : filterPropertiesInRoots(config.getPropertyNames(), roots)) {
            if (usedProperties.contains(name)) {
                continue;
            }

            if (!ignoredProperties.hasRootValue(name)) {
                ConfigValue configValue = config.getConfigValue(name);
                // TODO - https://github.com/quarkusio/quarkus/issues/38479
                if (configValue.getSourceName().startsWith(EnvConfigSource.NAME)) {
                    continue;
                }
                problems.add(new Problem(
                        ConfigMessages.msg.propertyDoesNotMapToAnyRoot(name, configValue.getLocation())));
            }
        }
    }

    private static void ignoreRecursively(KeyMap<Boolean> root) {
        if (root.getRootValue() == null) {
            root.putRootValue(Boolean.TRUE);
        }

        if (root.getAny() == null) {
            //noinspection CollectionAddedToSelf
            root.putAny(root);
        } else {
            var any = root.getAny();
            if (root != any) {
                ignoreRecursively(any);
            }
        }

        for (var value : root.values()) {
            ignoreRecursively(value);
        }
    }

    /**
     * Filters the full list of properties names in Config to only the property names that can match any of the
     * prefixes (namespaces) registered in mappings.
     *
     * @param properties the available property names in Config.
     * @param roots the registered mapping roots.
     *
     * @return the property names that match to at least one root.
     */
    private static Iterable<String> filterPropertiesInRoots(final Iterable<String> properties, final Set<String> roots) {
        if (roots.isEmpty()) {
            return properties;
        }

        // Will match everything, so no point in filtering
        if (roots.contains("")) {
            return properties;
        }

        List<String> matchedProperties = new ArrayList<>();
        for (String property : properties) {
            for (String root : roots) {
                if (isPropertyInRoot(property, root)) {
                    matchedProperties.add(property);
                    break;
                }
            }
        }
        return matchedProperties;
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
            return map(keyRawType, keyConvertWith, null);
        }

        public <K> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey) {
            return map(keyRawType, keyConvertWith, unnamedKey, (Class<?>) null);
        }

        public <K, V> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey,
                final Class<V> defaultClass) {

            Supplier<V> supplier = null;
            if (defaultClass != null) {
                supplier = new Supplier<V>() {
                    @Override
                    public V get() {
                        int length = stringBuilder.length();
                        stringBuilder.append(".*");
                        V defaultValue = constructGroup(defaultClass);
                        stringBuilder.setLength(length);
                        return defaultValue;
                    }
                };
            }

            return map(keyRawType, keyConvertWith, unnamedKey, supplier);
        }

        public <K, V> ObjectCreator<T> map(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final String unnamedKey,
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
                                        map.put(unnamedKey.equals("") ? null : keyConverter.convert(unnamedKey), value);
                                    }
                                }
                            });
                        }

                        Map<String, String> mapKeys = new HashMap<>();
                        Map<String, List<String>> mapProperties = new HashMap<>();
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

                        for (Map.Entry<String, String> mapKey : mapKeys.entrySet()) {
                            nestedCreators.add(new Consumer<>() {
                                @Override
                                public void accept(Function<String, Object> get) {
                                    // The properties may have been used ih the unnamed key, which cause clashes, so we skip them
                                    if (unnamedKey != null) {
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
                        StringBuilder sb = ConfigMappingContext.this.getStringBuilder();
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
                            StringBuilder sb = ConfigMappingContext.this.getStringBuilder();
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
                            StringBuilder sb = ConfigMappingContext.this.getStringBuilder();
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

        public ObjectCreator<T> value(
                final Class<T> valueRawType,
                final Class<? extends Converter<T>> valueConvertWith) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public T apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        Converter<T> valueConverter = getConverter(valueRawType, valueConvertWith);
                        return config.getValue(propertyName, valueConverter);
                    }
                });
            }
            return this;
        }

        public <V> ObjectCreator<T> optionalValue(
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public Optional<V> apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        Converter<V> valueConverter = getConverter(valueRawType, valueConvertWith);
                        return config.getOptionalValue(propertyName, valueConverter);
                    }
                });
            }
            return this;
        }

        public <V, C extends Collection<V>> ObjectCreator<T> values(
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public T apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        usedProperties.addAll(config.getIndexedProperties(propertyName));
                        Converter<V> itemConverter = itemConvertWith == null ? config.requireConverter(itemRawType)
                                : getConverterInstance(itemConvertWith);
                        IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);
                        return (T) config.getValues(propertyName, itemConverter, collectionFactory);
                    }
                });
            }
            return this;
        }

        public <V, C extends Collection<V>> ObjectCreator<T> optionalValues(
                final Class<V> itemRawType,
                final Class<? extends Converter<V>> itemConvertWith,
                final Class<C> collectionRawType) {
            for (Consumer<Function<String, Object>> creator : creators) {
                creator.accept(new Function<String, Object>() {
                    @Override
                    public T apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        usedProperties.addAll(config.getIndexedProperties(propertyName));
                        Converter<V> itemConverter = getConverter(itemRawType, itemConvertWith);
                        IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);
                        return (T) config.getOptionalValues(propertyName, itemConverter, collectionFactory);
                    }
                });
            }
            return this;
        }

        public <K, V> ObjectCreator<T> values(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                Function<String, Object> values = new Function<>() {
                    @Override
                    public Object apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        usedProperties.addAll(config.getMapKeys(propertyName).values());
                        Converter<K> keyConverter = getConverter(keyRawType, keyConvertWith);
                        Converter<V> valueConverter = getConverter(valueRawType, valueConvertWith);

                        if (defaultValue == null) {
                            // TODO - We should use getValues here, but this makes the Map to be required. This is a breaking change
                            return config.getOptionalValues(propertyName, keyConverter, valueConverter, HashMap::new)
                                    .orElse(new HashMap<>());
                        } else {
                            IntFunction<Map<K, V>> mapFactory = new IntFunction<>() {
                                @Override
                                public Map<K, V> apply(final int value) {
                                    return new MapWithDefault<>(valueConverter.convert(defaultValue));
                                }
                            };
                            return config.getOptionalValues(propertyName, keyConverter, valueConverter, mapFactory)
                                    .orElse(mapFactory.apply(0));
                        }
                    }
                };
                creator.accept(values);
            }
            return this;
        }

        public <K, V, C extends Collection<V>> ObjectCreator<T> values(
                final Class<K> keyRawType,
                final Class<? extends Converter<K>> keyConvertWith,
                final Class<V> valueRawType,
                final Class<? extends Converter<V>> valueConvertWith,
                final Class<C> collectionRawType,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                Function<String, Object> values = new Function<>() {
                    @Override
                    public Object apply(final String propertyName) {
                        usedProperties.add(propertyName);
                        usedProperties.addAll(config.getMapKeys(propertyName).values());
                        Converter<K> keyConverter = getConverter(keyRawType, keyConvertWith);
                        Converter<V> valueConverter = getConverter(valueRawType, valueConvertWith);

                        IntFunction<C> collectionFactory = (IntFunction<C>) createCollectionFactory(collectionRawType);

                        if (defaultValue == null) {
                            // TODO - We should use getValues here, but this makes the Map to be required. This is a breaking change
                            return config.getOptionalValues(propertyName, keyConverter, valueConverter, HashMap::new,
                                    collectionFactory).orElse(new HashMap<>());
                        } else {
                            IntFunction<Map<K, C>> mapFactory = new IntFunction<>() {
                                @Override
                                public Map<K, C> apply(final int value) {
                                    return new MapWithDefault<>(
                                            Converters.newCollectionConverter(valueConverter, collectionFactory)
                                                    .convert(defaultValue));
                                }
                            };

                            return config.getOptionalValues(propertyName, keyConverter, valueConverter, mapFactory,
                                    collectionFactory).orElse(mapFactory.apply(0));
                        }
                    }
                };
                creator.accept(values);
            }
            return this;
        }

        public T get() {
            return root;
        }

        private <V> Converter<V> getConverter(final Class<V> rawType, final Class<? extends Converter<V>> convertWith) {
            return convertWith == null ? config.requireConverter(rawType) : getConverterInstance(convertWith);
        }

        private <G> boolean createRequired(final Class<G> groupType, final String path) {
            Set<PropertyName> names = ConfigMappingContext.this.names.get(groupType.getName(), path);
            if (names == null) {
                return false;
            }

            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith(path) && names.contains(new PropertyName(propertyName))) {
                    return true;
                }
            }
            return false;
        }

        private IntFunction<Collection<?>> createCollectionFactory(final Class<?> type) {
            if (type == List.class) {
                return ArrayList::new;
            }

            if (type == Set.class) {
                return HashSet::new;
            }

            throw new IllegalArgumentException();
        }
    }

    static class MapWithDefault<K, V> extends HashMap<K, V> {
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
