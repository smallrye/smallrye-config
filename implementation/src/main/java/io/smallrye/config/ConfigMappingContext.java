package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.configMappingNames;
import static io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.ProfileConfigSourceInterceptor.activeName;
import static io.smallrye.config.common.utils.StringUtil.unindexed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
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

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.utils.StringUtil;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class ConfigMappingContext {
    private final SmallRyeConfig config;
    private final ConfigMappingNames names;
    private final Map<Class<?>, Map<String, ConfigMappingObject>> roots = new IdentityHashMap<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();

    private NamingStrategy namingStrategy = NamingStrategy.KEBAB_CASE;
    private boolean beanStyleGetters = false;
    private String rootPath = null;
    private final StringBuilder nameBuilder = new StringBuilder();
    private final Set<String> usedProperties = new HashSet<>();
    private final List<Problem> problems = new ArrayList<>();

    public ConfigMappingContext(final SmallRyeConfig config, final Map<Class<?>, Set<String>> roots) {
        this(config, new Supplier<Map<String, Map<String, Set<String>>>>() {
            @Override
            public Map<String, Map<String, Set<String>>> get() {
                // All mapping names must be loaded first because of split mappings
                Map<String, Map<String, Set<String>>> names = new HashMap<>();
                for (Map.Entry<Class<?>, Set<String>> mapping : roots.entrySet()) {
                    for (Map.Entry<String, Map<String, Set<String>>> entry : configMappingNames(mapping.getKey()).entrySet()) {
                        names.putIfAbsent(entry.getKey(), new HashMap<>());
                        names.get(entry.getKey()).putAll(entry.getValue());
                    }
                }
                return names;
            }
        }.get(), roots);
    }

    ConfigMappingContext(
            final SmallRyeConfig config,
            final Map<String, Map<String, Set<String>>> names,
            final Map<Class<?>, Set<String>> roots) {

        this.config = config;
        this.names = new ConfigMappingNames(names);
        matchPropertiesWithEnv(roots);
        for (Map.Entry<Class<?>, Set<String>> mapping : roots.entrySet()) {
            Map<String, ConfigMappingObject> mappingObjects = new HashMap<>();
            for (String rootPath : mapping.getValue()) {
                applyRootPath(rootPath);
                mappingObjects.put(rootPath, (ConfigMappingObject) constructRoot(mapping.getKey()));
            }
            this.roots.put(mapping.getKey(), mappingObjects);
        }
    }

    <T> T constructRoot(Class<T> interfaceType) {
        return constructGroup(interfaceType);
    }

    public <T> T constructGroup(Class<T> interfaceType) {
        NamingStrategy namingStrategy = this.namingStrategy;
        boolean beanStyleGetters = this.beanStyleGetters;
        T mappingObject = ConfigMappingLoader.configMappingObject(interfaceType, this);
        applyNamingStrategy(namingStrategy);
        applyBeanStyleGetters(beanStyleGetters);
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

    public void applyRootPath(final String rootPath) {
        this.rootPath = rootPath;
        this.nameBuilder.replace(0, nameBuilder.length(), rootPath);
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

    Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap() {
        return roots;
    }

    private void matchPropertiesWithEnv(final Map<Class<?>, Set<String>> roots) {
        // TODO - We shouldn't be mutating the EnvSource.
        // We should do the calculation when creating the EnvSource, but right now mappings and sources are not well integrated.

        Set<String> rootPaths = new HashSet<>();
        for (Set<String> paths : roots.values()) {
            rootPaths.addAll(paths);
        }
        boolean all = rootPaths.contains("");
        StringBuilder sb = new StringBuilder();

        for (ConfigSource configSource : config.getConfigSources(EnvConfigSource.class)) {
            if (roots.isEmpty()) {
                break;
            }

            EnvConfigSource envConfigSource = (EnvConfigSource) configSource;
            Set<String> mutableEnvProperties = envConfigSource.getPropertyNames();
            List<String> envProperties = new ArrayList<>(mutableEnvProperties);
            for (String envProperty : envProperties) {
                String activeEnvProperty;
                if (envProperty.charAt(0) == '%') {
                    activeEnvProperty = activeName(envProperty, config.getProfiles());
                } else {
                    activeEnvProperty = envProperty;
                }

                String matchedRoot = null;
                if (!all) {
                    for (String rootPath : rootPaths) {
                        if (StringUtil.isInPath(rootPath, activeEnvProperty)) {
                            matchedRoot = rootPath;
                            break;
                        }
                    }
                    if (matchedRoot == null) {
                        continue;
                    }
                } else {
                    matchedRoot = "";
                }

                for (Map<PropertyName, List<PropertyName>> mappingsNames : names.getNames().values()) {
                    List<PropertyName> propertyNames = mappingsNames.get(new PropertyName(""));
                    if (propertyNames == null) {
                        continue;
                    }

                    for (PropertyName mappedName : propertyNames) {
                        String name = matchedRoot.isEmpty() ? mappedName.getName() : matchedRoot + "." + mappedName.getName();
                        // Try to match Env with Root mapped property and generate the expected format
                        List<Integer> indexOfDashes = indexOfDashes(name, activeEnvProperty);
                        if (indexOfDashes != null) {
                            sb.append(activeEnvProperty);
                            for (Integer dash : indexOfDashes) {
                                sb.setCharAt(dash, '-');
                            }
                            String expectedEnvProperty = sb.toString();
                            if (!activeEnvProperty.equals(expectedEnvProperty)) {
                                envConfigSource.getPropertyNames().add(sb.toString());
                                envConfigSource.getPropertyNames().remove(envProperty);
                                // TODO - https://github.com/quarkusio/quarkus/issues/38479
                                //ignoredPaths.add(activeEnvProperty);
                            }
                            sb.setLength(0);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds and returns all indexes from a dotted Environment property name, related to its matched mapped
     * property name that must be replaced with a dash. This allows to set single environment variables as
     * <code>FOO_BAR_BAZ</code> and match them to mapped properties like <code>foo.*.baz</code>,
     * <code>foo-bar.baz</code> or any other combinations find in mappings, without the need of additional metadata.
     *
     * @param mappedProperty the mapping property name.
     * @param envProperty a generated dotted property from the {@link EnvConfigSource}.
     * @return a List of indexes from the env property name to replace with a dash.
     */
    private static List<Integer> indexOfDashes(final String mappedProperty, final String envProperty) {
        if (mappedProperty.length() > envProperty.length()) {
            return null;
        }

        List<Integer> dashesPosition = null;
        int matchPosition = envProperty.length() - 1;
        for (int i = mappedProperty.length() - 1; i >= 0; i--) {
            if (matchPosition == -1) {
                return null;
            }

            char c = mappedProperty.charAt(i);
            if (c == '.' || c == '-') {
                char p = envProperty.charAt(matchPosition);
                if (p != '.' && p != '-') { // a property coming from env can either be . or -
                    return null;
                }
                if (c == '-') {
                    if (dashesPosition == null) {
                        dashesPosition = new ArrayList<>();
                    }
                    dashesPosition.add(matchPosition);
                }
                matchPosition--;
            } else if (c == '*') { // it's a map - skip to next separator
                char p = envProperty.charAt(matchPosition);
                if (p == '"') {
                    matchPosition = envProperty.lastIndexOf('"', matchPosition - 1);
                    if (matchPosition != -1) {
                        matchPosition = envProperty.lastIndexOf('.', matchPosition);
                    }
                }
                matchPosition = envProperty.lastIndexOf('.', matchPosition);
            } else if (c == ']') { // it's a collection - skip to next separator
                i = i - 2;
                matchPosition = envProperty.lastIndexOf('[', matchPosition);
                if (matchPosition != -1) {
                    matchPosition--;
                }
            } else if (c != envProperty.charAt(matchPosition)) {
                return null;
            } else {
                matchPosition--;
            }
        }
        return dashesPosition;
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
        for (Map<String, ConfigMappingObject> value : this.roots.values()) {
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
                final Iterable<String> keys,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                Function<String, Object> values = new Function<>() {
                    @Override
                    public Object apply(final String propertyName) {
                        Converter<K> keyConverter = getConverter(keyRawType, keyConvertWith);
                        Converter<V> valueConverter = getConverter(valueRawType, valueConvertWith);

                        Map<String, String> mapKeys = new HashMap<>();
                        if (keys != null) {
                            for (String key : keys) {
                                mapKeys.put(key, propertyName + "." + quoted(key));
                            }
                        }
                        if (mapKeys.isEmpty()) {
                            mapKeys = config.getMapKeys(propertyName);
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

                        usedProperties.add(propertyName);
                        usedProperties.addAll(mapKeys.values());
                        return config.getMapValues(mapKeys, keyConverter, valueConverter, mapFactory);
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
                final Iterable<String> keys,
                final String defaultValue) {
            for (Consumer<Function<String, Object>> creator : creators) {
                Function<String, Object> values = new Function<>() {
                    @Override
                    public Object apply(final String propertyName) {
                        Converter<K> keyConverter = getConverter(keyRawType, keyConvertWith);
                        Converter<V> valueConverter = getConverter(valueRawType, valueConvertWith);

                        Map<String, String> mapKeys = new HashMap<>();
                        if (keys != null) {
                            for (String key : keys) {
                                mapKeys.put(key, propertyName + "." + quoted(key));
                            }
                        }
                        if (mapKeys.isEmpty()) {
                            mapKeys = config.getMapIndexedKeys(propertyName);
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

                        usedProperties.add(propertyName);
                        usedProperties.addAll(mapKeys.values());
                        // map keys can be indexed or unindexed, so we need to find which ones exist to mark them as used
                        usedProperties.addAll(config.getMapKeys(propertyName).values());
                        return config.getMapIndexedValues(mapKeys, keyConverter, valueConverter, mapFactory, collectionFactory);
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
            return ConfigMappingContext.this.names.hasAnyName(groupType.getName(), rootPath, path, config.getPropertyNames());
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

        private String quoted(final String key) {
            NameIterator keyIterator = new NameIterator(key);
            keyIterator.next();
            return keyIterator.hasNext() ? "\"" + key + "\"" : key;
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
