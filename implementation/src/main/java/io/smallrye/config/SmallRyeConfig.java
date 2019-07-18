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

import static java.lang.reflect.Array.newInstance;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
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

    static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigSource>() {
        @Override
        public int compare(ConfigSource o1, ConfigSource o2) {
            int res = Integer.signum(o2.getOrdinal() - o1.getOrdinal());
            // if 2 config sources have the same ordinal,
            // provide consistent order by sorting them
            // according to their name.
            return res != 0 ? res : o2.getName().compareTo(o1.getName());
        }
    };

    private final AtomicReference<List<ConfigSource>> configSourcesRef;
    private final Map<Type, Converter<?>> converters;

    protected SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        configSourcesRef = new AtomicReference<>(Collections.unmodifiableList(configSources));
        this.converters = new HashMap<>(Converters.ALL_CONVERTERS);
        this.converters.putAll(converters);
    }

    // no @Override
    public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                if (value.isEmpty()) {
                    // empty collection
                    break;
                }
                String[] itemStrings = StringUtil.split(value);
                final C collection = collectionFactory.apply(itemStrings.length);
                for (String itemString : itemStrings) {
                    collection.add(convert(itemString, itemClass));
                }
                return collection;
            }
        }
        // value not found
        throw propertyNotFound(name);
    }

    public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                if (value.isEmpty()) {
                    // empty collection
                    break;
                }
                String[] itemStrings = StringUtil.split(value);
                final C collection = collectionFactory.apply(itemStrings.length);
                for (String itemString : itemStrings) {
                    collection.add(converter.convert(itemString));
                }
                return collection;
            }
        }
        // value not found
        throw propertyNotFound(name);
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                if (value.isEmpty()) {
                    // treat empty value as non-present
                    break;
                }
                return convert(value, aClass);
            }
        }

        // check for  Optional numerical types to return their empty()
        // if the property is not found
        if (aClass.isAssignableFrom(OptionalInt.class)) {
            return aClass.cast(OptionalInt.empty());
        } else if (aClass.isAssignableFrom(OptionalLong.class)) {
            return aClass.cast(OptionalLong.empty());
        } else if (aClass.isAssignableFrom(OptionalDouble.class)) {
            return aClass.cast(OptionalDouble.empty());
        }
        throw propertyNotFound(name);
    }

    public <T> T getValue(String name, Converter<T> converter) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                if (value.isEmpty()) {
                    // treat empty value as non-present
                    break;
                }
                return converter.convert(value);
            }
        }
        throw propertyNotFound(name);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                // treat empty value as non-present
                return value.isEmpty() ? Optional.empty() : Optional.of(convert(value, aClass));
            }
        }
        // value not found
        return Optional.empty();
    }

    public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
        for (ConfigSource configSource : getConfigSources()) {
            String value = configSource.getValue(name);
            if (value != null) {
                // treat empty value as non-present
                return value.isEmpty() ? Optional.empty() : Optional.of(converter.convert(value));
            }
        }
        // value not found
        return Optional.empty();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Set<String> names = new HashSet<>();
        for (ConfigSource configSource : getConfigSources()) {
            names.addAll(configSource.getProperties().keySet());
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
        if (value != null) {
            boolean isArray = asType.isArray();
            if (isArray) {
                String[] split = StringUtil.split(value);
                Class<?> componentType = asType.getComponentType();
                T array = asType.cast(newInstance(componentType, split.length));
                for (int i = 0; i < split.length; i++) {
                    Array.set(array, i, convert(split[i], componentType));
                }
                return array;
            } else {
                Converter<T> converter = getConverter(asType);
                return converter.convert(value);
            }
        }
        return null;
    }

    public <T> Converter<T> getConverter(Class<T> asType) {
        @SuppressWarnings("unchecked")
        Converter<T> converter = (Converter<T>) converters.get(asType);
        if (converter == null) {
            // look for implicit converters
            synchronized (converters) {
                converter = ImplicitConverters.getConverter(asType);
                converters.putIfAbsent(asType, converter);
            }
        }
        if (converter == null) {
            throw new IllegalArgumentException("No Converter registered for class " + asType);
        }
        return converter;
    }

    private static NoSuchElementException propertyNotFound(final String name) {
        return new NoSuchElementException("Property " + name + " not found");
    }
}
