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

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfig implements Config, Serializable {

    private static final long serialVersionUID = 8138651532357898263L;

    static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigSource>() {
        @Override
        public int compare(ConfigSource o1, ConfigSource o2) {
            int res = Long.signum((long) o2.getOrdinal() - (long) o1.getOrdinal());
            // if 2 config sources have the same ordinal,
            // provide consistent order by sorting them
            // according to their name.
            return res != 0 ? res : o2.getName().compareTo(o1.getName());
        }
    };

    private final AtomicReference<List<ConfigSource>> configSourcesRef;
    private final Map<Type, Converter<?>> converters;
    private final Map<Type, Converter<Optional<?>>> optionalConverters = new ConcurrentHashMap<>();

    protected SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        configSourcesRef = new AtomicReference<>(Collections.unmodifiableList(configSources));
        this.converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        this.converters.putAll(converters);
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
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                final T converted = converter.convert(value);
                if (converted == null) {
                    throw propertyNotFound(name);
                }
                return converted;
            }
        }
        try {
            final T converted = converter.convert("");
            if (converted == null) {
                throw propertyNotFound(name);
            }
            return converted;
        } catch (IllegalArgumentException ignored) {
            throw propertyNotFound(name);
        }
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
        Set<String> names = new HashSet<>();
        for (ConfigSource configSource : getConfigSources()) {
            names.addAll(configSource.getPropertyNames());
        }
        return names;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSourcesRef.get();
    }

    /**
     * Add a configuration source to the configuration object. The list of configuration sources is re-sorted
     * to insert the new source into the correct position. Configuration source wrappers configured with
     * {@link SmallRyeConfigBuilder#withWrapper(UnaryOperator)} will not be applied.
     *
     * @param configSource the new config source (must not be {@code null})
     */
    public void addConfigSource(ConfigSource configSource) {
        List<ConfigSource> oldVal, newVal;
        int oldSize;
        do {
            oldVal = configSourcesRef.get();
            oldSize = oldVal.size();
            newVal = Arrays.asList(oldVal.toArray(new ConfigSource[oldSize + 1]));
            newVal.set(oldSize, configSource);
            newVal.sort(CONFIG_SOURCE_COMPARATOR);
        } while (!configSourcesRef.compareAndSet(oldVal, Collections.unmodifiableList(newVal)));
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
        if (asType.isArray()) {
            return Converters.newArrayConverter(getConverter(asType.getComponentType()), asType);
        }
        return (Converter<T>) converters.computeIfAbsent(asType, clazz -> {
            final Converter<?> conv = ImplicitConverters.getConverter((Class<?>) clazz);
            if (conv == null) {
                throw new IllegalArgumentException("No Converter registered for class " + asType);
            }
            return conv;
        });
    }

    private static NoSuchElementException propertyNotFound(final String name) {
        return new NoSuchElementException("Property " + name + " not found");
    }
}
