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

package io.smallrye.config.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import io.smallrye.config.StringUtil;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producer for {@link Config} bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@ApplicationScoped
public class ConfigProducer implements Serializable{

    @Produces
    Config getConfig(InjectionPoint injectionPoint) {
        // return the Config for the TCCL
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return ConfigProvider.getConfig(tccl);
    }

    @Dependent
    @Produces @ConfigProperty
    String produceStringConfigProperty(InjectionPoint ip) {
        return getValue(ip, String.class);
    }

    @Dependent
    @Produces @ConfigProperty
    Long getLongValue(InjectionPoint ip) {
        return getValue(ip, Long.class);
    }

    @Dependent
    @Produces @ConfigProperty
    Integer getIntegerValue(InjectionPoint ip) {
        return getValue(ip, Integer.class);
    }

    @Dependent
    @Produces @ConfigProperty
    Float produceFloatConfigProperty(InjectionPoint ip) {
        return getValue(ip, Float.class);
    }

    @Dependent
    @Produces @ConfigProperty
    Double produceDoubleConfigProperty(InjectionPoint ip) {
        return getValue(ip, Double.class);
    }

    @Dependent
    @Produces @ConfigProperty
    Boolean produceBooleanConfigProperty(InjectionPoint ip) {
        return getValue(ip, Boolean.class);
    }

    @SuppressWarnings("unchecked")
    @Dependent
    @Produces @ConfigProperty
    <T> Optional<T> produceOptionalConfigValue(InjectionPoint injectionPoint) {
        Type type = injectionPoint.getAnnotated().getBaseType();
        final Class<T> valueType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            valueType = unwrapType(typeArguments[0]);
        } else {
            valueType = (Class<T>) String.class;
        }
        return Optional.ofNullable(getValue(injectionPoint, valueType));
    }

    @Dependent
    @Produces @ConfigProperty
    <T> Set<T> producesSetConfigPropery(InjectionPoint ip) {
        Type type = ip.getAnnotated().getBaseType();
        final Class<T> valueType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            valueType = unwrapType(typeArguments[0]);
        } else {
            valueType = (Class<T>) String.class;
        }
        HashSet<T> s = new HashSet<>();
        String stringValue = getStringValue(ip);
        String[] split = StringUtil.split(stringValue);
        Config config = getConfig(ip);
        for(int i = 0 ; i < split.length ; i++) {
            T item = ((SmallRyeConfig) config).convert(split[i], valueType);
            s.add(item);
        }
        return s;
    }

    @Dependent
    @Produces @ConfigProperty
    <T> List<T> producesListConfigPropery(InjectionPoint ip) {
        Type type = ip.getAnnotated().getBaseType();
        final Class<T> valueType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            valueType = unwrapType(typeArguments[0]);
        } else {
            valueType = (Class<T>) String.class;
        }
        ArrayList<T> s = new ArrayList<>();
        String stringValue = getStringValue(ip);
        String[] split = StringUtil.split(stringValue);
        Config config = getConfig(ip);
        for(int i = 0 ; i < split.length ; i++) {
            T item = ((SmallRyeConfig) config).convert(split[i], valueType);
            s.add(item);
        }
        return s;
    }

    private <T> Class<T> unwrapType(Type type) {
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getRawType();
        }
        return (Class<T>) type;
    }

    private <T> T getValue
            (InjectionPoint injectionPoint, Class<T> target) {
        Config config = getConfig(injectionPoint);
        String name = getName(injectionPoint);
        try {
            if (name == null) {
                return null;
            }
            Optional<T> optionalValue = config.getOptionalValue(name, target);
            if (optionalValue.isPresent()) {
                return optionalValue.get();
            } else {
                String defaultValue = getDefaultValue(injectionPoint);
                if (defaultValue != null && !defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                    return ((SmallRyeConfig)config).convert(defaultValue, target);
                } else {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String getStringValue(InjectionPoint injectionPoint) {
        Config config = getConfig(injectionPoint);
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }
        Optional<String> optionalValue = config.getOptionalValue(name, String.class);
        if (optionalValue.isPresent()) {
            return optionalValue.get();
        } else {
            return null;
        }
    }

    private String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty)qualifier);
                return ConfigExtension.getConfigKey(injectionPoint, configProperty);
            }
        }
        return null;
    }

    private String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                return ((ConfigProperty) qualifier).defaultValue();
            }
        }
        return null;
    }
}
