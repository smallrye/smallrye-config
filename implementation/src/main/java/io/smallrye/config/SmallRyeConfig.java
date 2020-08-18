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
import static java.util.stream.Collectors.toList;

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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.config.Config;
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
            for (Converter converter : builder.discoverConverters()) {
                Type type = Converters.getConverterType(converter.getClass());
                if (type == null) {
                    throw ConfigMessages.msg.unableToAddConverter(converter);
                }
                SmallRyeConfigBuilder.addConverter(type, converter, convertersToBuild);
            }
        }

        final ConcurrentHashMap<Type, Converter<?>> converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        convertersToBuild.forEach(
                (type, converterWithPriority) -> converters.put(type, converterWithPriority.getConverter()));
        converters.put(ConfigValue.class, ConfigValueConverter.CONFIG_VALUE_CONVERTER);

        return converters;
    }

    // no @Override
    public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) {
        return getValues(name, requireConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) {
        return getValue(name, Converters.newCollectionConverter(converter, collectionFactory));
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        return getValue(name, requireConverter(aClass));
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
                throw ConfigMessages.msg.propertyNotFound(name);
            }
        }
        if (converted == null) {
            throw ConfigMessages.msg.propertyNotFound(name);
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
        return getOptionalValues(name, requireConverter(itemClass), collectionFactory);
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
    public <T> T getConfigProperties(final Class<T> configProperties, final String prefix) {
        return getConfigMapping(configProperties, prefix);
    }

    @Override
    public <T> T getConfigProperties(final Class<T> configProperties) {
        return getConfigMapping(configProperties);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        final HashSet<String> names = new HashSet<>();
        configSources.get().getInterceptorChain().iterateNames().forEachRemaining(names::add);
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

    <T> Converter<T> requireConverter(final Class<T> asType) {
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
            sortInterceptors.addAll(sources.stream()
                    .filter(configSource -> !(configSource instanceof ConfigurableConfigSource))
                    .map(ConfigSourceInterceptorWithPriority::new)
                    .collect(toList()));
            sortInterceptors.addAll(interceptors.stream().map(ConfigSourceInterceptorWithPriority::new).collect(toList()));
            sortInterceptors.sort(Comparator.comparingInt(ConfigSourceInterceptorWithPriority::getPriority));

            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : sortInterceptors) {
                final ConfigSourceInterceptorWithPriority initInterceptor = configSourceInterceptor.getInterceptor(current);
                current = new SmallRyeConfigSourceInterceptorContext(initInterceptor.getInterceptor(), current);
            }

            final SmallRyeConfigSourceInterceptorContext initChain = current;
            final List<ConfigurableConfigSource> lateInitSources = sources.stream()
                    .filter(ConfigurableConfigSource.class::isInstance)
                    .map(ConfigurableConfigSource.class::cast)
                    .sorted(Comparator.comparingInt(ConfigurableConfigSource::getOrdinal))
                    .collect(toList());

            final List<ConfigSourceInterceptorWithPriority> lateInterceptors = lateInitSources.stream()
                    .flatMap(configurableConfigSource -> configurableConfigSource.getConfigSources(
                            new ConfigSourceContext() {
                                @Override
                                public ConfigValue getValue(final String name) {
                                    return initChain.proceed(name);
                                }

                                @Override
                                public Iterator<String> iterateNames() {
                                    return initChain.iterateNames();
                                }
                            }).stream())
                    .map(ConfigSourceInterceptorWithPriority::new)
                    .collect(toList());

            sortInterceptors.addAll(lateInterceptors);
            sortInterceptors.sort(Comparator.comparingInt(ConfigSourceInterceptorWithPriority::getPriority));

            final List<ConfigSourceInterceptorWithPriority> initInterceptors = new ArrayList<>();
            current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : sortInterceptors) {
                final ConfigSourceInterceptorWithPriority initInterceptor = configSourceInterceptor.getInterceptor(current);
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
            newInterceptors.sort(Comparator.comparingInt(ConfigSourceInterceptorWithPriority::getPriority));

            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (ConfigSourceInterceptorWithPriority configSourceInterceptor : newInterceptors) {
                final ConfigSourceInterceptor interceptor = configSourceInterceptor.getInterceptor();
                current = new SmallRyeConfigSourceInterceptorContext(interceptor, current);
            }

            this.sources = Collections.unmodifiableList(getSources(newInterceptors));
            this.interceptors = Collections.unmodifiableList(newInterceptors);
            this.interceptorChain = current;
        }

        private List<ConfigSource> getSources(final List<ConfigSourceInterceptorWithPriority> interceptors) {
            final List<ConfigSource> sources = interceptors.stream()
                    .map(ConfigSourceInterceptorWithPriority::getInterceptor)
                    .filter(SmallRyeConfigSourceInterceptor.class::isInstance)
                    .map(SmallRyeConfigSourceInterceptor.class::cast)
                    .map(SmallRyeConfigSourceInterceptor::getSource)
                    .collect(toList());
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
    }

    static class ConfigSourceInterceptorWithPriority implements Comparable<ConfigSourceInterceptorWithPriority>, Serializable {
        private static final long serialVersionUID = 1637460029437579033L;

        final Function<ConfigSourceInterceptorContext, ConfigSourceInterceptor> interceptor;
        final int priority;
        final String name;

        public ConfigSourceInterceptorWithPriority(final InterceptorWithPriority interceptor) {
            this.interceptor = interceptor::getInterceptor;
            this.priority = interceptor.getPriority();
            this.name = "undefined";
        }

        public ConfigSourceInterceptorWithPriority(final ConfigSource configSource) {
            this.interceptor = context -> configSourceInterceptor(configSource);
            this.priority = configSource.getOrdinal();
            this.name = configSource.getName();
        }

        public ConfigSourceInterceptorWithPriority(final ConfigSourceInterceptor interceptor, final int priority,
                final String name) {
            this.interceptor = (Serializable & Function) context -> interceptor;
            this.priority = priority;
            this.name = name;
        }

        public ConfigSourceInterceptor getInterceptor() {
            return interceptor.apply(null);
        }

        public ConfigSourceInterceptorWithPriority getInterceptor(final ConfigSourceInterceptorContext context) {
            return new ConfigSourceInterceptorWithPriority(interceptor.apply(context), priority, name);
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(final ConfigSourceInterceptorWithPriority other) {
            int res = Integer.compare(this.priority, other.priority);
            return res != 0 ? res : this.name.compareTo(other.name);
        }
    }
}
