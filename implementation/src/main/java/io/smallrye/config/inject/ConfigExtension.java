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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI Extension to produces Config bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ConfigExtension implements Extension {

    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    public ConfigExtension() {
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
        bbd.addAnnotatedType(configBean, ConfigProducer.class.getName());
    }

    void collectConfigPropertyInjectionPoints(@Observes ProcessInjectionPoint<?, ?> pip) {
        if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    void registerCustomBeans(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        Set<Class<?>> customTypes = new HashSet<>();
        for (InjectionPoint ip : injectionPoints) {
            Type requiredType = ip.getType();
            if (requiredType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) requiredType;
                // TODO We should probably handle all parameterized types correctly
                if (type.getRawType().equals(Provider.class) || type.getRawType().equals(Instance.class)) {
                    // These injection points are satisfied by the built-in Instance bean 
                    Type typeArgument = type.getActualTypeArguments()[0];
                    if (typeArgument instanceof Class && !isClassHandledByConfigProducer(typeArgument)) {
                        customTypes.add((Class<?>) typeArgument);
                    }
                }
            } else if (requiredType instanceof Class
                    && !isClassHandledByConfigProducer(requiredType)) {
                // type is not produced by ConfigProducer
                customTypes.add((Class<?>) requiredType);
            }
        }

        for (Class<?> customType : customTypes) {
            abd.addBean(new ConfigInjectionBean(bm, customType));
        }
    }

    void validate(@Observes AfterDeploymentValidation adv) {

        Config config = ConfigProvider.getConfig();
        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();
            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            if (type instanceof Class) {
                String key = getConfigKey(injectionPoint, configProperty);
                try {
                    if (!config.getOptionalValue(key, (Class<?>) type).isPresent()) {
                        String defaultValue = configProperty.defaultValue();
                        if (defaultValue == null ||
                                defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                            adv.addDeploymentProblem(InjectionMessages.msg.noConfigValue(key));
                        }
                    }
                } catch (IllegalArgumentException cause) {
                    adv.addDeploymentProblem(InjectionMessages.msg.retrieveConfigFailure(cause, key));
                }
            } else if (type instanceof ParameterizedType) {
                Class<?> rawType = (Class<?>) ((ParameterizedType) type).getRawType();
                // for collections, we only check if the property config exists without trying to convert it
                if (Collection.class.isAssignableFrom(rawType)) {
                    String key = getConfigKey(injectionPoint, configProperty);
                    try {
                        if (!config.getOptionalValue(key, String.class).isPresent()) {
                            String defaultValue = configProperty.defaultValue();
                            if (defaultValue == null ||
                                    defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                                adv.addDeploymentProblem(InjectionMessages.msg.noConfigValue(key));
                            }
                        }
                    } catch (IllegalArgumentException cause) {
                        adv.addDeploymentProblem(InjectionMessages.msg.retrieveConfigFailure(cause, key));
                    }
                }
            }
        }
    }

    static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.trim().isEmpty()) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember member = (AnnotatedMember) ip.getAnnotated();
            AnnotatedType declaringType = member.getDeclaringType();
            if (declaringType != null) {
                String[] parts = declaringType.getJavaClass().getCanonicalName().split("\\.");
                StringBuilder sb = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    sb.append(".").append(parts[i]);
                }
                sb.append(".").append(member.getJavaMember().getName());
                return sb.toString();
            }
        }
        throw InjectionMessages.msg.noConfigPropertyDefaultName(ip);
    }

    private static boolean isClassHandledByConfigProducer(Type requiredType) {
        return requiredType == String.class
                || requiredType == Boolean.class
                || requiredType == Boolean.TYPE
                || requiredType == Integer.class
                || requiredType == Integer.TYPE
                || requiredType == Long.class
                || requiredType == Long.TYPE
                || requiredType == Float.class
                || requiredType == Float.TYPE
                || requiredType == Double.class
                || requiredType == Double.TYPE
                || requiredType == Short.class
                || requiredType == Short.TYPE
                || requiredType == Byte.class
                || requiredType == Byte.TYPE
                || requiredType == Character.class
                || requiredType == Character.TYPE
                || requiredType == OptionalInt.class
                || requiredType == OptionalLong.class
                || requiredType == OptionalDouble.class;
    }
}
