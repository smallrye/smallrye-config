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

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import io.smallrye.config.SmallRyeConfigBuilder.InterceptorWithPriority;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfig implements Config, Serializable {

    private static final long serialVersionUID = 8138651532357898263L;

    static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigSource>() {
        @Override
        public int compare(ConfigSource o1, ConfigSource o2) {
            int res = Integer.compare(o2.getOrdinal(), o1.getOrdinal());
            // if 2 config sources have the same ordinal,
            // provide consistent order by sorting them
            // according to their name.
            return res != 0 ? res : o2.getName().compareTo(o1.getName());
        }
    };

    private final AtomicReference<ConfigSources> configSources;
    private final Map<Type, Converter<?>> converters;
    private final Map<Type, Converter<Optional<?>>> optionalConverters = new ConcurrentHashMap<>();

    SmallRyeConfig(SmallRyeConfigBuilder builder) {
        this.configSources = new AtomicReference<>(new ConfigSources(buildConfigSources(builder), buildInterceptors(builder)));
        this.converters = buildConverters(builder);
    }

    @Deprecated
    protected SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        this.configSources = new AtomicReference<>(
                new ConfigSources(configSources, buildInterceptors(new SmallRyeConfigBuilder())));
        this.converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        this.converters.putAll(converters);
    }

    private List<ConfigSource> buildConfigSources(final SmallRyeConfigBuilder builder) {
        final List<ConfigSource> sourcesToBuild = new ArrayList<>(builder.getSources());
        if (builder.isAddDiscoveredSources()) {
            sourcesToBuild.addAll(builder.discoverSources());
        }
        if (builder.isAddDefaultSources()) {
            sourcesToBuild.addAll(builder.getDefaultSources());
        }

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

    public <T> T getValue(String name, Converter<T> converter) {
        String value = getRawValue(name);
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

    ConfigValue getConfigValue(String name) {
        return configSources.get().getInterceptorChain().proceed(name);
    }

    /**
     * Get the <em>raw value</em> of a configuration property.
     *
     * @param name the property name (must not be {@code null})
     * @return the raw value, or {@code null} if no property value was discovered for the given property name
     */
    public String getRawValue(String name) {
        final ConfigValue configValue = getConfigValue(name);
        return configValue != null ? configValue.getValue() : null;
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
        configSources.updateAndGet(configSources -> {
            List<ConfigSource> currentSources = configSources.getSources();

            int oldSize = currentSources.size();
            List<ConfigSource> newSources = Arrays.asList(currentSources.toArray(new ConfigSource[oldSize + 1]));
            newSources.set(oldSize, configSource);
            return new ConfigSources(newSources, configSources);
        });
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

    private static class ConfigSources implements Serializable {
        private static final long serialVersionUID = 3483018375584151712L;

        private final List<ConfigSource> sources;
        private final List<ConfigSourceInterceptor> interceptors;
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
            sources.sort(CONFIG_SOURCE_COMPARATOR);
            interceptors.sort(Comparator.comparingInt(InterceptorWithPriority::getPriority).reversed());

            List<ConfigSourceInterceptor> initializedInterceptors = new ArrayList<>();
            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);
            for (int i = sources.size() - 1; i >= 0; i--) {
                current = new SmallRyeConfigSourceInterceptorContext(configSourceInterceptor(sources.get(i)), current);
            }

            for (int i = interceptors.size() - 1; i >= 0; i--) {
                ConfigSourceInterceptor interceptor = interceptors.get(i).getInterceptor(current);
                current = new SmallRyeConfigSourceInterceptorContext(interceptor, current);
                initializedInterceptors.add(interceptor);
            }

            this.sources = Collections.unmodifiableList(sources);
            this.interceptors = Collections.unmodifiableList(initializedInterceptors);
            this.interceptorChain = current;
        }

        /**
         * Builds a representation of Config Sources, Interceptors and the Interceptor chain to be used in Config. Note
         * that this constructor is used in an already existent Config instance to reconstruct the chain if additional
         * Config Sources are added to the Config.
         *
         * @param sources the Config Sources to be part of Config.
         * @param configSources the previous ConfigSources
         */
        ConfigSources(final List<ConfigSource> sources, final ConfigSources configSources) {
            sources.sort(CONFIG_SOURCE_COMPARATOR);

            SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(EMPTY, null);

            for (int i = sources.size() - 1; i >= 0; i--) {
                current = new SmallRyeConfigSourceInterceptorContext(configSourceInterceptor(sources.get(i)), current);
            }

            for (int i = configSources.getInterceptors().size() - 1; i >= 0; i--) {
                current = new SmallRyeConfigSourceInterceptorContext(configSources.getInterceptors().get(i), current);
            }

            this.sources = Collections.unmodifiableList(sources);
            this.interceptors = configSources.getInterceptors();
            this.interceptorChain = current;
        }

        List<ConfigSource> getSources() {
            return sources;
        }

        List<ConfigSourceInterceptor> getInterceptors() {
            return interceptors;
        }

        ConfigSourceInterceptorContext getInterceptorChain() {
            return interceptorChain;
        }
    }
}
