/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import static io.smallrye.config.ConfigSourceInterceptor.EMPTY;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static io.smallrye.config.common.utils.StringUtil.toLowerCaseAndDotted;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.config.SmallRyeConfigBuilder.InterceptorWithPriority;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfig implements Config, Serializable {
    public static final String SMALLRYE_CONFIG_PROFILE = "smallrye.config.profile";
    public static final String SMALLRYE_CONFIG_PROFILE_PARENT = "smallrye.config.profile.parent";
    public static final String SMALLRYE_CONFIG_LOCATIONS = "smallrye.config.locations";
    public static final String SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN = "smallrye.config.mapping.validate-unknown";

    private static final long serialVersionUID = 8138651532357898263L;

    private final ConfigSources configSources;
    private final Map<Type, Converter<?>> converters;
    private final Map<Type, Converter<Optional<?>>> optionalConverters = new ConcurrentHashMap<>();

    private final ConfigMappings mappings;

    SmallRyeConfig(SmallRyeConfigBuilder builder, ConfigMappings mappings) {
        this.configSources = new ConfigSources(builder);
        this.converters = buildConverters(builder);
        this.mappings = mappings;
    }

    private Map<Type, Converter<?>> buildConverters(final SmallRyeConfigBuilder builder) {
        final Map<Type, SmallRyeConfigBuilder.ConverterWithPriority> convertersToBuild = new HashMap<>(builder.getConverters());

        if (builder.isAddDiscoveredConverters()) {
            for (Converter<?> converter : builder.discoverConverters()) {
                Type type = Converters.getConverterType(converter.getClass());
                if (type == null) {
                    throw ConfigMessages.msg.unableToAddConverter(converter);
                }
                SmallRyeConfigBuilder.addConverter(type, converter, convertersToBuild);
            }
        }

        final ConcurrentHashMap<Type, Converter<?>> converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        for (Map.Entry<Type, SmallRyeConfigBuilder.ConverterWithPriority> entry : convertersToBuild.entrySet()) {
            converters.put(entry.getKey(), entry.getValue().getConverter());
        }
        converters.put(ConfigValue.class, ConfigValueConverter.CONFIG_VALUE_CONVERTER);

        return converters;
    }

    @Override
    public <T> List<T> getValues(final String propertyName, final Class<T> propertyType) {
        return getValues(propertyName, propertyType, ArrayList::new);
    }

    public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) {
        return getValues(name, requireConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) {
        try {
            return getValue(name, Converters.newCollectionConverter(converter, collectionFactory));
        } catch (NoSuchElementException e) {
            return getIndexedValues(name, converter, collectionFactory);
        }
    }

    public <T, C extends Collection<T>> C getIndexedValues(String name, Converter<T> converter,
            IntFunction<C> collectionFactory) {
        List<String> indexedProperties = getIndexedProperties(name);
        if (indexedProperties.isEmpty()) {
            throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
        }

        final C collection = collectionFactory.apply(indexedProperties.size());
        for (String indexedProperty : indexedProperties) {
            collection.add(getValue(indexedProperty, converter));
        }

        return collection;
    }

    public List<String> getIndexedProperties(final String property) {
        List<Integer> indexes = getIndexedPropertiesIndexes(property);
        List<String> indexedProperties = new ArrayList<>();
        for (Integer index : indexes) {
            indexedProperties.add(property + "[" + index + "]");
        }

        return indexedProperties;
    }

    public List<Integer> getIndexedPropertiesIndexes(final String property) {
        Set<Integer> indexes = new HashSet<>();
        for (String propertyName : this.getPropertyNames()) {
            if (propertyName.startsWith(property) && propertyName.length() > property.length()) {
                int index = property.length();
                if (propertyName.charAt(index) == '[') {
                    for (;;) {
                        if (propertyName.charAt(index) == ']') {
                            try {
                                indexes.add(Integer.parseInt(propertyName.substring(property.length() + 1, index)));
                            } catch (NumberFormatException e) {
                                //NOOP
                            }
                            break;
                        } else if (index < propertyName.length() - 1) {
                            index++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        List<Integer> sortIndexes = new ArrayList<>(indexes);
        Collections.sort(sortIndexes);
        return sortIndexes;
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        return getValue(name, requireConverter(aClass));
    }

    /**
     * Return the content of the direct sub properties as the requested type of Map.
     *
     * @param name The configuration property name
     * @param kClass the type into which the keys should be converted
     * @param vClass the type into which the values should be converted
     * @param <K> the key type
     * @param <V> the value type
     * @return the resolved property value as an instance of the requested Map (not {@code null})
     * @throws IllegalArgumentException if a key or a value cannot be converted to the specified types
     * @throws NoSuchElementException if no direct sub properties could be found.
     */
    @Experimental("Extension to retrieve mandatory sub properties as a Map")
    public <K, V> Map<K, V> getValues(String name, Class<K> kClass, Class<V> vClass) {
        final Map<K, V> result = getValuesAsMap(name, requireConverter(kClass), requireConverter(vClass));
        if (result == null) {
            throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
        }
        return result;
    }

    /**
     * Return the content of the direct sub properties as the requested type of Map.
     *
     * @param name The configuration property name
     * @param keyConverter The converter to use for the keys.
     * @param valueConverter The converter to use for the values.
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @return the resolved property value as an instance of the requested Map or {@code null} if it could not be found.
     * @throws IllegalArgumentException if a key or a value cannot be converted to the specified types
     */
    public <K, V> Map<K, V> getValuesAsMap(String name, Converter<K> keyConverter, Converter<V> valueConverter) {
        final String prefix = name.endsWith(".") ? name : name + ".";
        final Map<K, V> result = new HashMap<>();
        for (String propertyName : getPropertyNames()) {
            if (propertyName.startsWith(prefix)) {
                final String key = propertyName.substring(prefix.length());
                if (key.indexOf('.') >= 0) {
                    // Ignore sub namespaces
                    continue;
                }
                result.put(convertValue(propertyName + "#key", key, keyConverter),
                        convertValue(propertyName + "#value", getRawValue(propertyName), valueConverter));
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 
     * This method handles calls from both {@link Config#getValue} and {@link Config#getOptionalValue}.<br>
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, Converter<T> converter) {
        final ConfigValue configValue = getConfigValue(name);
        if (ConfigValueConverter.CONFIG_VALUE_CONVERTER.equals(converter)) {
            return (T) configValue;
        }

        if (converter instanceof Converters.OptionalConverter<?>) {
            if (ConfigValueConverter.CONFIG_VALUE_CONVERTER.equals(
                    ((Converters.OptionalConverter<?>) converter).getDelegate())) {
                return (T) Optional.of(configValue);
            }
        }

        final String value = configValue.getValue(); // Can return the empty String (which is not considered as null)

        return convertValue(name, value, converter);
    }

    /**
     * 
     * This method handles converting values for both CDI injections and programatical calls.<br>
     * <br>
     * 
     * Calls for converting non-optional values ({@link Config#getValue} and "Injecting Native Values")
     * should throw an {@link Exception} for each of the following:<br>
     * 
     * 1. {@link IllegalArgumentException} - if the property cannot be converted by the {@link Converter} to the specified type
     * <br>
     * 2. {@link NoSuchElementException} - if the property is not defined <br>
     * 3. {@link NoSuchElementException} - if the property is defined as an empty string <br>
     * 4. {@link NoSuchElementException} - if the {@link Converter} returns {@code null} <br>
     * <br>
     * 
     * Calls for converting optional values ({@link Config#getOptionalValue} and "Injecting Optional Values")
     * should only throw an {@link Exception} for #1 ({@link IllegalArgumentException} when the property cannot be converted to
     * the specified type).
     */
    public <T> T convertValue(String name, String value, Converter<T> converter) {

        final T converted;

        if (value != null) {
            try {
                converted = converter.convert(value);
            } catch (IllegalArgumentException e) {
                throw ConfigMessages.msg.converterException(e, name, value, e.getLocalizedMessage()); // 1
            }
        } else {
            try {
                // See if the Converter is designed to handle a missing (null) value i.e. Optional Converters
                converted = converter.convert("");
            } catch (IllegalArgumentException ignored) {
                throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name)); // 2
            }
        }

        if (converted == null) {
            if (value == null) {
                throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name)); // 2
            } else if (value.length() == 0) {
                throw ConfigMessages.msg.propertyEmptyString(name, converter.getClass().getTypeName()); // 3
            } else {
                throw ConfigMessages.msg.converterReturnedNull(name, value, converter.getClass().getTypeName()); // 4
            }
        }

        return converted;
    }

    /**
     * Determine whether the <em>raw value</em> of a configuration property is exactly equal to the expected given
     * value.
     *
     * @param name the property name (must not be {@code null})
     * @param expected the expected value (may be {@code null})
     * @return {@code true} if the values are equal, {@code false} otherwise
     */
    public boolean rawValueEquals(String name, String expected) {
        return Objects.equals(expected, getRawValue(name));
    }

    @Experimental("Extension to the original ConfigSource to allow retrieval of additional metadata on config lookup")
    public ConfigValue getConfigValue(String name) {
        final ConfigValue configValue = configSources.getInterceptorChain().proceed(name);
        return configValue != null ? configValue : ConfigValue.builder().withName(name).build();
    }

    /**
     * Get the <em>raw value</em> of a configuration property.
     *
     * @param name the property name (must not be {@code null})
     * @return the raw value, or {@code null} if no property value was discovered for the given property name
     */
    public String getRawValue(String name) {
        final ConfigValue configValue = getConfigValue(name);
        return configValue != null && configValue.getValue() != null ? configValue.getValue() : null;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        return getValue(name, getOptionalConverter(aClass));
    }

    /**
     * Return the content of the direct sub properties as the requested type of Map.
     *
     * @param name The configuration property name
     * @param kClass the type into which the keys should be converted
     * @param vClass the type into which the values should be converted
     * @param <K> the key type
     * @param <V> the value type
     * @return the resolved property value as an instance of the requested Map (not {@code null})
     * @throws IllegalArgumentException if a key or a value cannot be converted to the specified types
     */
    @Experimental("Extension to retrieve non mandatory sub properties as a Map")
    public <K, V> Optional<Map<K, V>> getOptionalValues(String name, Class<K> kClass, Class<V> vClass) {
        return Optional.ofNullable(getValuesAsMap(name, requireConverter(kClass), requireConverter(vClass)));
    }

    public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
        return getValue(name, Converters.newOptionalConverter(converter));
    }

    public <T> Optional<List<T>> getOptionalValues(final String propertyName, final Class<T> propertyType) {
        return getOptionalValues(propertyName, propertyType, ArrayList::new);
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Class<T> itemClass,
            IntFunction<C> collectionFactory) {
        return getOptionalValues(name, requireConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter,
            IntFunction<C> collectionFactory) {
        final Optional<C> optionalValue = getOptionalValue(name,
                Converters.newCollectionConverter(converter, collectionFactory));
        if (optionalValue.isPresent()) {
            return optionalValue;
        } else {
            return getIndexedOptionalValues(name, converter, collectionFactory);
        }
    }

    public <T, C extends Collection<T>> Optional<C> getIndexedOptionalValues(String name, Converter<T> converter,
            IntFunction<C> collectionFactory) {
        List<String> indexedProperties = getIndexedProperties(name);
        if (indexedProperties.isEmpty()) {
            return Optional.empty();
        }

        final C collection = collectionFactory.apply(indexedProperties.size());
        for (String indexedProperty : indexedProperties) {
            final Optional<T> optionalValue = getOptionalValue(indexedProperty, converter);
            optionalValue.ifPresent(collection::add);
        }

        if (!collection.isEmpty()) {
            return Optional.of(collection);
        }

        return Optional.empty();
    }

    public ConfigMappings getConfigMappings() {
        return mappings;
    }

    @Experimental("ConfigMapping API to group configuration properties")
    public <T> T getConfigMapping(Class<T> type) {
        return mappings.getConfigMapping(type);
    }

    @Experimental("ConfigMapping API to group configuration properties")
    public <T> T getConfigMapping(Class<T> type, String prefix) {
        return mappings.getConfigMapping(type, prefix);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return configSources.getPropertyNames().get();
    }

    /**
     * Checks if a property is present in the {@link Config} instance.
     *
     * Because {@link ConfigSource#getPropertyNames()} may not include all available properties, it is not possible to
     * reliable determine if the property is present in the properties list. The property needs to be retrieved to make
     * sure it exists. The lookup is done without expression expansion, because the expansion value may not be
     * available and it not relevant for the final check.
     *
     * @param name the property name.
     * @return true if the property is present or false otherwise.
     */
    @Experimental("Check if a property is present")
    public boolean isPropertyPresent(String name) {
        return Expressions.withoutExpansion(() -> getConfigValue(name).getValue() != null);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources.getSources();
    }

    public Iterable<ConfigSource> getConfigSources(final Class<?> type) {
        final List<ConfigSource> configSourcesByType = new ArrayList<>();
        for (ConfigSource configSource : getConfigSources()) {
            if (type.isAssignableFrom(configSource.getClass())) {
                configSourcesByType.add(configSource);
            }
        }
        return configSourcesByType;
    }

    @Experimental("To retrieve a ConfigSource by name")
    public Optional<ConfigSource> getConfigSource(final String name) {
        for (ConfigSource configSource : getConfigSources()) {
            final String configSourceName = configSource.getName();
            if (configSourceName != null && configSourceName.equals(name)) {
                return Optional.of(configSource);
            }
        }
        return Optional.empty();
    }

    public <T> T convert(String value, Class<T> asType) {
        return value != null ? requireConverter(asType).convert(value) : null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Converter<Optional<T>> getOptionalConverter(Class<T> asType) {
        return optionalConverters.computeIfAbsent(asType,
                clazz -> Converters.newOptionalConverter(requireConverter((Class) clazz)));
    }

    @Deprecated // binary-compatibility bridge method for Quarkus
    public <T> Converter<T> getConverter$$bridge(Class<T> asType) {
        return requireConverter(asType);
    }

    // @Override // in MP Config 2.0+
    public <T> Optional<Converter<T>> getConverter(Class<T> asType) {
        return Optional.ofNullable(getConverterOrNull(asType));
    }

    public <T> Converter<T> requireConverter(final Class<T> asType) {
        final Converter<T> conv = getConverterOrNull(asType);
        if (conv == null) {
            throw ConfigMessages.msg.noRegisteredConverter(asType);
        }
        return conv;
    }

    @SuppressWarnings("unchecked")
    <T> Converter<T> getConverterOrNull(Class<T> asType) {
        final Converter<?> exactConverter = converters.get(asType);
        if (exactConverter != null) {
            return (Converter<T>) exactConverter;
        }
        if (asType.isPrimitive()) {
            return (Converter<T>) getConverterOrNull(Converters.wrapPrimitiveType(asType));
        }
        if (asType.isArray()) {
            final Converter<?> conv = getConverterOrNull(asType.getComponentType());
            return conv == null ? null : Converters.newArrayConverter(conv, asType);
        }
        return (Converter<T>) converters.computeIfAbsent(asType, clazz -> ImplicitConverters.getConverter((Class<?>) clazz));
    }

    @Override
    public <T> T unwrap(final Class<T> type) {
        if (Config.class.isAssignableFrom(type)) {
            return type.cast(this);
        }

        throw ConfigMessages.msg.getTypeNotSupportedForUnwrapping(type);
    }

    @Experimental("To retrieve active profiles")
    public List<String> getProfiles() {
        return configSources.getProfiles();
    }

    void addPropertyNames(Set<String> properties) {
        configSources.getPropertyNames().add(properties);
    }

    private static class ConfigSources implements Serializable {
        private static final long serialVersionUID = 3483018375584151712L;

        private final List<String> profiles;
        private final List<ConfigSource> sources;
        private final ConfigSourceInterceptorContext interceptorChain;
        private final PropertyNames propertyNames;

        /**
         * Builds a representation of Config Sources, Interceptors and the Interceptor chain to be used in Config. Note
         * that this constructor must be used when the Config object is being initialized, because interceptors also
         * require initialization.
         */
        ConfigSources(final SmallRyeConfigBuilder builder) {
            // Add all sources except for ConfigurableConfigSource types. These are initialized later
            List<ConfigSource> sources = buildSources(builder);
            // Add all interceptors
            List<ConfigSourceInterceptor> interceptors = new ArrayList<>();
            List<InterceptorWithPriority> interceptorWithPriorities = buildInterceptors(builder);

            // Create the initial chain with initial sources and all interceptors
            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            current = new SmallRyeConfigSourceInterceptorContext(new SmallRyeConfigSources(mapSources(sources)), current);
            for (InterceptorWithPriority interceptorWithPriority : interceptorWithPriorities) {
                ConfigSourceInterceptor interceptor = interceptorWithPriority.getInterceptor(current);
                interceptors.add(interceptor);
                current = new SmallRyeConfigSourceInterceptorContext(interceptor, current);
            }

            // Init all late sources
            List<String> profiles = getProfiles(interceptors);
            List<ConfigSourceWithPriority> sourcesWithPriorities = mapLateSources(current, sources, profiles,
                    builder.isAddDiscoveredSources());

            // Rebuild the chain with the late sources and new instances of the interceptors
            // The new instance will ensure that we get rid of references to factories and other stuff and keep only
            // the resolved final source or interceptor to use.
            current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            current = new SmallRyeConfigSourceInterceptorContext(new SmallRyeConfigSources(sourcesWithPriorities),
                    current);
            for (ConfigSourceInterceptor interceptor : interceptors) {
                current = new SmallRyeConfigSourceInterceptorContext(interceptor, current);
            }

            // PropertyNames and generate additional properties
            List<ConfigSource> configSources = getSources(sourcesWithPriorities);
            PropertyNamesConfigSourceInterceptor propertyNamesInterceptor = new PropertyNamesConfigSourceInterceptor();
            current = new SmallRyeConfigSourceInterceptorContext(propertyNamesInterceptor, current);
            PropertyNames propertyNames = new PropertyNames(propertyNamesInterceptor);
            propertyNames.add(generateDottedProperties(configSources, current));

            this.profiles = profiles;
            this.sources = configSources;
            this.interceptorChain = current;
            this.propertyNames = propertyNames;
        }

        private static List<ConfigSource> buildSources(final SmallRyeConfigBuilder builder) {
            final List<ConfigSource> sourcesToBuild = new ArrayList<>(builder.getSources());
            if (builder.isAddDiscoveredSources()) {
                sourcesToBuild.addAll(builder.discoverSources());
            }
            if (builder.isAddDefaultSources()) {
                sourcesToBuild.addAll(builder.getDefaultSources());
            }
            sourcesToBuild.add(new DefaultValuesConfigSource(builder.getDefaultValues()));

            return sourcesToBuild;
        }

        private static List<InterceptorWithPriority> buildInterceptors(final SmallRyeConfigBuilder builder) {
            final List<InterceptorWithPriority> interceptors = new ArrayList<>(builder.getInterceptors());
            if (builder.isAddDiscoveredInterceptors()) {
                interceptors.addAll(builder.discoverInterceptors());
            }
            if (builder.isAddDefaultInterceptors()) {
                interceptors.addAll(builder.getDefaultInterceptors());
            }

            interceptors.sort(null);
            return interceptors;
        }

        private static List<ConfigSourceWithPriority> mapSources(final List<ConfigSource> sources) {
            final List<ConfigSourceWithPriority> sourcesWithPriority = new ArrayList<>();
            for (ConfigSource source : sources) {
                if (!(source instanceof ConfigurableConfigSource)) {
                    sourcesWithPriority.add(new ConfigSourceWithPriority(source));
                }
            }
            sourcesWithPriority.sort(null);
            Collections.reverse(sourcesWithPriority);
            return sourcesWithPriority;
        }

        private static List<String> getProfiles(final List<ConfigSourceInterceptor> interceptors) {
            for (final ConfigSourceInterceptor interceptor : interceptors) {
                if (interceptor instanceof ProfileConfigSourceInterceptor) {
                    return Arrays.asList(((ProfileConfigSourceInterceptor) interceptor).getProfiles());
                }
            }
            return Collections.emptyList();
        }

        private static List<ConfigSourceWithPriority> mapLateSources(
                final SmallRyeConfigSourceInterceptorContext initChain,
                final List<ConfigSource> sources,
                final List<String> profiles,
                final boolean addDiscoveredSources) {

            ConfigSourceWithPriority.resetLoadPriority();

            List<ConfigurableConfigSource> lateSources = new ArrayList<>();
            for (ConfigSource source : sources) {
                if (source instanceof ConfigurableConfigSource) {
                    lateSources.add((ConfigurableConfigSource) source);
                }
            }
            lateSources.sort(Comparator.comparingInt(ConfigurableConfigSource::getOrdinal));

            int countSourcesFromLocations = 0;
            List<ConfigSourceWithPriority> sourcesWithPriority = new ArrayList<>();
            for (ConfigurableConfigSource configurableSource : lateSources) {
                final List<ConfigSource> configSources = configurableSource.getConfigSources(new ConfigSourceContext() {
                    @Override
                    public ConfigValue getValue(final String name) {
                        ConfigValue value = initChain.proceed(name);
                        return value != null ? value : ConfigValue.builder().withName(name).build();
                    }

                    @Override
                    public List<String> getProfiles() {
                        return profiles;
                    }

                    @Override
                    public Iterator<String> iterateNames() {
                        return initChain.iterateNames();
                    }
                });

                if (configurableSource.getFactory() instanceof AbstractLocationConfigSourceFactory) {
                    countSourcesFromLocations = countSourcesFromLocations + configSources.size();
                }

                for (ConfigSource configSource : configSources) {
                    sourcesWithPriority.add(new ConfigSourceWithPriority(configSource));
                }
            }

            if (countSourcesFromLocations == 0 && addDiscoveredSources) {
                ConfigValue locations = initChain.proceed(SMALLRYE_CONFIG_LOCATIONS);
                if (locations != null && locations.getValue() != null) {
                    ConfigLogging.log.configLocationsNotFound(SMALLRYE_CONFIG_LOCATIONS, locations.getValue());
                }
            }

            sourcesWithPriority.addAll(mapSources(sources));
            sourcesWithPriority.sort(null);
            Collections.reverse(sourcesWithPriority);

            return sourcesWithPriority;
        }

        private static List<ConfigSource> getSources(final List<ConfigSourceWithPriority> sourceWithPriorities) {
            final List<ConfigSource> configSources = new ArrayList<>();
            for (ConfigSourceWithPriority configSourceWithPriority : sourceWithPriorities) {
                configSources.add(configSourceWithPriority.getSource());
            }
            return Collections.unmodifiableList(configSources);
        }

        /**
         * Generate dotted properties from Env properties.
         *
         * These are required when a consumer relies on the list of properties to find additional
         * configurations. The list of properties is not normalized due to environment variables, which follow specific
         * naming rules. The MicroProfile Config specification defines a set of conversion rules to look up and find
         * values from environment variables even when using their dotted version, but this does not apply to the
         * properties list.
         *
         * Because an environment variable name may only be represented by a subset of characters, it is not possible
         * to represent exactly a dotted version name from an environment variable name. Additional dotted properties
         * mapped from environment variables are only added if a relationship cannot be found between all properties
         * using the conversions look up rules of the MicroProfile Config specification. Example:
         *
         * If <code>foo.bar</code> is present and <code>FOO_BAR</code> is also present, no property is required.
         * If <code>foo-bar</code> is present and <code>FOO_BAR</code> is also present, no property is required.
         * If <code>FOO_BAR</code> is present a property <code>foo.bar</code> is required.
         */
        private static Set<String> generateDottedProperties(final List<ConfigSource> sources,
                final SmallRyeConfigSourceInterceptorContext current) {
            // Collect all known properties
            Set<String> properties = new HashSet<>();
            Iterator<String> iterateNames = current.iterateNames();
            while (iterateNames.hasNext()) {
                properties.add(iterateNames.next());
            }

            // Collect only properties from the EnvSources
            Set<String> envProperties = new HashSet<>();
            for (ConfigSource source : sources) {
                if (source instanceof EnvConfigSource) {
                    envProperties.addAll(source.getPropertyNames());
                }
            }
            properties.removeAll(envProperties);

            // Collect properties that have the same semantic meaning
            Set<String> overrides = new HashSet<>();
            for (String property : properties) {
                for (String envProperty : envProperties) {
                    if (envProperty.equalsIgnoreCase(replaceNonAlphanumericByUnderscores(property))) {
                        overrides.add(envProperty);
                        break;
                    }
                }
            }

            // Remove them - Remaining properties can only be found in the EnvSource - generate a dotted version
            envProperties.removeAll(overrides);
            Set<String> dottedProperties = new HashSet<>();
            for (String envProperty : envProperties) {
                dottedProperties.add(toLowerCaseAndDotted(envProperty));
            }
            return dottedProperties;
        }

        List<String> getProfiles() {
            return profiles;
        }

        List<ConfigSource> getSources() {
            return sources;
        }

        ConfigSourceInterceptorContext getInterceptorChain() {
            return interceptorChain;
        }

        PropertyNames getPropertyNames() {
            return propertyNames;
        }

        class PropertyNames implements Serializable {
            private static final long serialVersionUID = 4193517748286869745L;

            private final PropertyNamesConfigSourceInterceptor interceptor;

            private PropertyNames(final PropertyNamesConfigSourceInterceptor propertyNamesInterceptor) {
                this.interceptor = propertyNamesInterceptor;
            }

            Iterable<String> get() {
                final HashSet<String> names = new HashSet<>();
                final Iterator<String> namesIterator = interceptorChain.iterateNames();
                while (namesIterator.hasNext()) {
                    names.add(namesIterator.next());
                }
                return names;
            }

            void add(final Set<String> properties) {
                interceptor.addProperties(properties);
            }
        }
    }

    static class ConfigSourceWithPriority implements Comparable<ConfigSourceWithPriority>, Serializable {
        private static final long serialVersionUID = 3709554647398262957L;

        private final ConfigSource source;
        private final int priority;
        private final int loadPriority = loadPrioritySequence++;

        ConfigSourceWithPriority(final ConfigSource source) {
            this.source = source;
            this.priority = source.getOrdinal();
        }

        ConfigSource getSource() {
            return source;
        }

        @Override
        public int compareTo(final ConfigSourceWithPriority other) {
            int res = Integer.compare(this.priority, other.priority);
            return res != 0 ? res : Integer.compare(other.loadPriority, this.loadPriority);
        }

        private static int loadPrioritySequence = 0;

        static void resetLoadPriority() {
            loadPrioritySequence = 0;
        }
    }

    private Object writeReplace() throws ObjectStreamException {
        return RegisteredConfig.instance;
    }

    /**
     * Serialization placeholder which deserializes to the current registered config
     */
    private static class RegisteredConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final RegisteredConfig instance = new RegisteredConfig();

        private Object readResolve() throws ObjectStreamException {
            return ConfigProvider.getConfig();
        }
    }
}
