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
import static io.smallrye.config.SmallRyeConfigSourceInterceptor.configSourceInterceptor;

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
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

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
    private static final long serialVersionUID = 8138651532357898263L;

    private final AtomicReference<ConfigSources> configSources;
    private final Map<Type, Converter<?>> converters;
    private final Map<Type, Converter<Optional<?>>> optionalConverters = new ConcurrentHashMap<>();

    private final ConfigMappings mappings;

    SmallRyeConfig(SmallRyeConfigBuilder builder, ConfigMappings mappings) {
        this.configSources = new AtomicReference<>(new ConfigSources(buildConfigSources(builder), buildInterceptors(builder)));
        this.converters = buildConverters(builder);
        this.mappings = mappings;
    }

    @Deprecated
    protected SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        this.configSources = new AtomicReference<>(
                new ConfigSources(configSources, buildInterceptors(new SmallRyeConfigBuilder())));
        this.converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        this.converters.putAll(converters);
        this.mappings = new ConfigMappings();
    }

    private List<ConfigSource> buildConfigSources(final SmallRyeConfigBuilder builder) {
        final List<ConfigSource> sourcesToBuild = new ArrayList<>(builder.getSources());
        if (builder.isAddDiscoveredSources()) {
            sourcesToBuild.addAll(builder.discoverSources());
        }
        if (builder.isAddDefaultSources()) {
            sourcesToBuild.addAll(builder.getDefaultSources());
        }
        sourcesToBuild.add(new DefaultValuesConfigSource(builder.getDefaultValues()));

        // wrap all
        final Function<ConfigSource, ConfigSource> sourceWrappersToBuild = builder.getSourceWrappers();
        final ListIterator<ConfigSource> it = sourcesToBuild.listIterator();
        while (it.hasNext()) {
            it.set(sourceWrappersToBuild.apply(it.next()));
        }

        return sourcesToBuild;
    }

    private List<InterceptorWithPriority> buildInterceptors(final SmallRyeConfigBuilder builder) {
        final List<InterceptorWithPriority> interceptors = new ArrayList<>(builder.getInterceptors());
        if (builder.isAddDiscoveredInterceptors()) {
            interceptors.addAll(builder.discoverInterceptors());
        }
        if (builder.isAddDefaultInterceptors()) {
            interceptors.addAll(builder.getDefaultInterceptors());
        }

        return interceptors;
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

    // no @Override
    public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) {
        return getValues(name, getConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) {
        return getValue(name, Converters.newCollectionConverter(converter, collectionFactory));
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        return getValue(name, getConverter(aClass));
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, Converter<T> converter) {
        ConfigValue configValue = getConfigValue(name);
        if (ConfigValueConverter.CONFIG_VALUE_CONVERTER.equals(converter)) {
            return (T) configValue;
        }

        if (converter instanceof Converters.OptionalConverter<?>) {
            if (ConfigValueConverter.CONFIG_VALUE_CONVERTER.equals(
                    ((Converters.OptionalConverter<?>) converter).getDelegate())) {
                return (T) Optional.of(configValue);
            }
        }

        String value = configValue.getValue();
        final T converted;
        if (value != null) {
            converted = converter.convert(value);
        } else {
            try {
                converted = converter.convert("");
            } catch (IllegalArgumentException ignored) {
                throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
            }
        }
        if (converted == null) {
            throw new NoSuchElementException(ConfigMessages.msg.propertyNotFound(name));
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
        final ConfigValue configValue = configSources.get().getInterceptorChain().proceed(name);
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

    public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
        return getValue(name, Converters.newOptionalConverter(converter));
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Class<T> itemClass,
            IntFunction<C> collectionFactory) {
        return getOptionalValues(name, getConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter,
            IntFunction<C> collectionFactory) {
        return getOptionalValue(name, Converters.newCollectionConverter(converter, collectionFactory));
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
        final HashSet<String> names = new HashSet<>();
        final Iterator<String> namesIterator = configSources.get().getInterceptorChain().iterateNames();
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        return names;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources.get().getSources();
    }

    /**
     * Add a configuration source to the configuration object. The list of configuration sources is re-sorted
     * to insert the new source into the correct position. Configuration source wrappers configured with
     * {@link SmallRyeConfigBuilder#withWrapper(UnaryOperator)} will not be applied.
     *
     * @param configSource the new config source (must not be {@code null})
     */
    @Deprecated
    public void addConfigSource(ConfigSource configSource) {
        configSources.updateAndGet(configSources -> new ConfigSources(configSources, configSource));
    }

    public <T> T convert(String value, Class<T> asType) {
        return value != null ? getConverter(asType).convert(value) : null;
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<Optional<T>> getOptionalConverter(Class<T> asType) {
        return optionalConverters.computeIfAbsent(asType,
                clazz -> Converters.newOptionalConverter(getConverter((Class) clazz)));
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getConverter(Class<T> asType) {
        final Converter<?> exactConverter = converters.get(asType);
        if (exactConverter != null) {
            return (Converter<T>) exactConverter;
        }
        if (asType.isPrimitive()) {
            return (Converter<T>) getConverter(Converters.wrapPrimitiveType(asType));
        }
        if (asType.isArray()) {
            return Converters.newArrayConverter(getConverter(asType.getComponentType()), asType);
        }
        return (Converter<T>) converters.computeIfAbsent(asType, clazz -> {
            final Converter<?> conv = ImplicitConverters.getConverter((Class<?>) clazz);
            if (conv == null) {
                throw ConfigMessages.msg.noRegisteredConverter(asType);
            }
            return conv;
        });
    }

    @Experimental("To retrive active profiles")
    public List<String> getProfiles() {
        return configSources.get().getProfiles();
    }

    private static class ConfigSources implements Serializable {
        private static final long serialVersionUID = 3483018375584151712L;

        private final List<ConfigSource> sources;
        private final List<ConfigSourceInterceptorWithPriority> interceptors;
        private final ConfigSourceInterceptorContext interceptorChain;

        /**
         * Builds a representation of Config Sources, Interceptors and the Interceptor chain to be used in Config. Note
         * that this constructor must be used when the Config object is being initialized, because interceptors also
         * require initialization.
         *
         * Instances of the Interceptors are then kept in ConfigSources if the interceptor chain requires a reorder (in
         * the case a new ConfigSource is addded to Config).
         *
         * @param sources the Config Sources to be part of Config.
         * @param interceptors the Interceptors to be part of Config.
         */
        ConfigSources(final List<ConfigSource> sources, final List<InterceptorWithPriority> interceptors) {
            final List<ConfigSourceInterceptorWithPriority> sortInterceptors = new ArrayList<>();
            // Add all sources except for ConfigurableConfigSource types. These are initialized later
            // Sources are converted to the interceptor API
            sortInterceptors.addAll(mapSources(sources));
            // Add all interceptors
            sortInterceptors.addAll(mapInterceptors(interceptors));
            sortInterceptors.sort(null);

            // Create the initial chain and init each element with the current context
            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : sortInterceptors) {
                current = new SmallRyeConfigSourceInterceptorContext(configSourceInterceptor.getInterceptor(current), current);
            }

            // Init all late sources. Late sources are converted to the interceptor API and sorted again
            sortInterceptors.addAll(mapLateSources(current, sources, getProfiles(sortInterceptors)));
            sortInterceptors.sort(null);

            // Rebuild the chain with the late sources and collect new instances of the interceptors
            // The new instance will ensure that we get rid of references to factories and other stuff and keep only
            // the resolved final source or interceptor to use.
            final List<ConfigSourceInterceptorWithPriority> initInterceptors = new ArrayList<>();
            current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : sortInterceptors) {
                ConfigSourceInterceptorWithPriority initInterceptor = configSourceInterceptor.initialized(current);
                current = new SmallRyeConfigSourceInterceptorContext(initInterceptor.getInterceptor(), current);
                initInterceptors.add(initInterceptor);
            }

            this.interceptorChain = current;
            this.sources = Collections.unmodifiableList(getSources(initInterceptors));
            this.interceptors = Collections.unmodifiableList(initInterceptors);
        }

        /**
         * Builds a representation of Config Sources, Interceptors and the Interceptor chain to be used in Config. Note
         * that this constructor is used in an already existent Config instance to reconstruct the chain if additional
         * Config Sources are added to the Config.
         *
         * @param sources the Config Sources to be part of Config.
         * @param configSource the new ConfigSource to add into the interceptor the chain.
         */
        ConfigSources(final ConfigSources sources, final ConfigSource configSource) {
            final int oldSize = sources.getInterceptors().size();
            final List<ConfigSourceInterceptorWithPriority> newInterceptors = Arrays
                    .asList(sources.getInterceptors().toArray(new ConfigSourceInterceptorWithPriority[oldSize + 1]));
            newInterceptors.set(oldSize, new ConfigSourceInterceptorWithPriority(configSource));
            newInterceptors.sort(null);

            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : newInterceptors) {
                current = new SmallRyeConfigSourceInterceptorContext(configSourceInterceptor.getInterceptor(current), current);
            }

            this.sources = Collections.unmodifiableList(getSources(newInterceptors));
            this.interceptors = Collections.unmodifiableList(newInterceptors);
            this.interceptorChain = current;
        }

        private static List<ConfigSourceInterceptorWithPriority> mapSources(final List<ConfigSource> sources) {
            ConfigSourceInterceptorWithPriority.raiseLoadPriority();
            final List<ConfigSourceInterceptorWithPriority> sourcesWithPriority = new ArrayList<>();
            for (ConfigSource source : sources) {
                if (!(source instanceof ConfigurableConfigSource)) {
                    sourcesWithPriority.add(new ConfigSourceInterceptorWithPriority(source));
                }
            }
            return sourcesWithPriority;
        }

        private static List<ConfigSourceInterceptorWithPriority> mapInterceptors(
                final List<InterceptorWithPriority> interceptors) {
            final List<ConfigSourceInterceptorWithPriority> sourcesWithPriority = new ArrayList<>();
            for (InterceptorWithPriority interceptor : interceptors) {
                sourcesWithPriority.add(new ConfigSourceInterceptorWithPriority(interceptor));
            }
            return sourcesWithPriority;
        }

        private static List<String> getProfiles(final List<ConfigSourceInterceptorWithPriority> interceptors) {
            for (final ConfigSourceInterceptorWithPriority interceptor : interceptors) {
                if (interceptor.getInterceptor() instanceof ProfileConfigSourceInterceptor) {
                    return Arrays.asList(((ProfileConfigSourceInterceptor) interceptor.getInterceptor()).getProfiles());
                }
            }
            return Collections.emptyList();
        }

        private static List<ConfigSourceInterceptorWithPriority> mapLateSources(
                final SmallRyeConfigSourceInterceptorContext initChain,
                final List<ConfigSource> sources,
                final List<String> profiles) {

            final List<ConfigurableConfigSource> lateSources = new ArrayList<>();
            for (ConfigSource source : sources) {
                if (source instanceof ConfigurableConfigSource) {
                    lateSources.add((ConfigurableConfigSource) source);
                }
            }
            lateSources.sort(Comparator.comparingInt(ConfigurableConfigSource::getOrdinal));

            ConfigSourceInterceptorWithPriority.raiseLoadPriority();
            final List<ConfigSourceInterceptorWithPriority> sourcesWithPriority = new ArrayList<>();
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

                for (ConfigSource configSource : configSources) {
                    sourcesWithPriority.add(new ConfigSourceInterceptorWithPriority(configSource));
                }
            }

            return sourcesWithPriority;
        }

        private static List<ConfigSource> getSources(final List<ConfigSourceInterceptorWithPriority> interceptors) {
            final List<ConfigSource> sources = new ArrayList<>();
            for (ConfigSourceInterceptorWithPriority interceptor : interceptors) {
                if (interceptor.getInterceptor() instanceof SmallRyeConfigSourceInterceptor) {
                    sources.add(((SmallRyeConfigSourceInterceptor) interceptor.getInterceptor()).getSource());
                }
            }
            Collections.reverse(sources);
            return sources;
        }

        List<ConfigSource> getSources() {
            return sources;
        }

        List<ConfigSourceInterceptorWithPriority> getInterceptors() {
            return interceptors;
        }

        ConfigSourceInterceptorContext getInterceptorChain() {
            return interceptorChain;
        }

        List<String> getProfiles() {
            for (final ConfigSourceInterceptorWithPriority interceptor : getInterceptors()) {
                if (interceptor.getInterceptor() instanceof ProfileConfigSourceInterceptor) {
                    return Arrays.asList(((ProfileConfigSourceInterceptor) interceptor.getInterceptor()).getProfiles());
                }
            }
            return Collections.emptyList();
        }
    }

    static class ConfigSourceInterceptorWithPriority implements Comparable<ConfigSourceInterceptorWithPriority>, Serializable {
        private static final long serialVersionUID = 1637460029437579033L;

        private final Function<ConfigSourceInterceptorContext, ConfigSourceInterceptor> init;
        private final int priority;
        private final int loadPriority = loadPrioritySequence--;
        private final String name;

        private ConfigSourceInterceptor interceptor;

        ConfigSourceInterceptorWithPriority(final InterceptorWithPriority interceptor) {
            this.init = interceptor::getInterceptor;
            this.priority = interceptor.getPriority();
            this.name = "undefined";
        }

        ConfigSourceInterceptorWithPriority(final ConfigSource configSource) {
            this.init = context -> configSourceInterceptor(configSource);
            this.priority = configSource.getOrdinal();
            this.name = configSource.getName();
        }

        private ConfigSourceInterceptorWithPriority(final ConfigSourceInterceptor interceptor, final int priority,
                final String name) {
            this.init = null;
            this.priority = priority;
            this.name = name;
            this.interceptor = interceptor;
        }

        ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
            if (this.interceptor == null) {
                this.interceptor = init.apply(context);
            }

            return this.interceptor;
        }

        ConfigSourceInterceptor getInterceptor() {
            if (this.interceptor == null) {
                throw new IllegalStateException();
            }

            return this.interceptor;
        }

        ConfigSourceInterceptorWithPriority initialized(final ConfigSourceInterceptorContext context) {
            return new ConfigSourceInterceptorWithPriority(this.getInterceptor(context), this.priority, this.name);
        }

        private static int loadPrioritySequence = 0;
        private static int loadPrioritySequenceNumber = 1;

        static void raiseLoadPriority() {
            loadPrioritySequenceNumber++;
            loadPrioritySequence = 1000 * loadPrioritySequenceNumber;
        }

        @Override
        public int compareTo(final ConfigSourceInterceptorWithPriority other) {
            int res = Integer.compare(this.priority, other.priority);
            return res != 0 ? res : Integer.compare(this.loadPriority, other.loadPriority);
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
        private static RegisteredConfig instance = new RegisteredConfig();

        private Object readResolve() throws ObjectStreamException {
            return ConfigProvider.getConfig();
        }
    }

}
