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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Based on GERONIMO-6595 support implicit converters.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ImplicitConverters {

    static Converter getConverter(Class<?> clazz) {
        for (Converter converter : new Converter[] {
                getConverterFromConstructor(clazz, String.class),
                getConverterFromConstructor(clazz, CharSequence.class),
                getConverterFromStaticMethod(clazz, "valueOf", String.class),
                getConverterFromStaticMethod(clazz, "valueOf", CharSequence.class),
                getConverterFromStaticMethod(clazz, "parse", String.class),
                getConverterFromStaticMethod(clazz, "parse", CharSequence.class)
        }) {
            if (converter != null) {
                return converter;
            }
        }
        return null;
    }

    private static Converter getConverterFromConstructor(Class<?> clazz, Class<?> paramType) {
        try {
            final Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(paramType);
            if (!declaredConstructor.isAccessible()) {
                declaredConstructor.setAccessible(true);
            }
            return value -> {
                try {
                    return declaredConstructor.newInstance(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            };
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private static Converter getConverterFromStaticMethod(Class<?> clazz, String methodName, Class<?> paramType) {
        try {
            final Method method = clazz.getMethod(methodName, paramType);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            if (Modifier.isStatic(method.getModifiers())) {
                return value -> {
                    try {
                        return method.invoke(null, value);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                };
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
}
