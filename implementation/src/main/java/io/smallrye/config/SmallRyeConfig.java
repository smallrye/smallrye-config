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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfig implements Config, Serializable {

    private final List<ConfigSource> configSources;
    private Map<Type, Converter> converters;

    SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter> converters) {
        this.configSources = configSources;
        this.converters = new HashMap<>(Converters.ALL_CONVERTERS);
        this.converters.putAll(converters);
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getValue(name);
            if (value != null) {
                return convert(value, aClass);
            }
        }
        throw new NoSuchElementException("Property " + name + " not found");
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getValue(name);
            // treat empty value as null
            if (value != null && value.length() > 0) {
                return Optional.of(convert(value, aClass));
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Set<String> names = new HashSet<>();
        for (ConfigSource configSource : configSources) {
            names.addAll(configSource.getProperties().keySet());
        }
        return names;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }

    public <T> T convert(String value, Class<T> asType) {
        if (value != null) {
            boolean isArray = asType.isArray();
            if (isArray) {
                String[] split = StringUtil.split(value);
                Class<?> componentType = asType.getComponentType();
                T array =  (T)newInstance(componentType, split.length);
                Converter<T> converter = getConverter(asType);
                for (int i = 0 ; i < split.length ; i++) {
                    T s = converter.convert(split[i]);
                    Array.set(array, i, s);
                }
                return array;
            } else {
                Converter<T> converter = getConverter(asType);
                return converter.convert(value);
            }
        }
        return null;
    }

    private <T> Converter getConverter(Class<T> asType) {
        if (asType.isArray()) {
            return getConverter(asType.getComponentType());
        } else {
            Converter converter = converters.get(asType);
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
    }
}
