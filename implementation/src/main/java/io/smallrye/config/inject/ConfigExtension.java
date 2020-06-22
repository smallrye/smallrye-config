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

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

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
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;

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
        Config config = ConfigProvider.getConfig(getContextClassLoader());
        Set<String> configNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();

            // We don't validate the Optional / Provider / Supplier / ConfigValue for defaultValue.
            if (type instanceof Class && ConfigValue.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalInt.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalLong.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalDouble.class.isAssignableFrom((Class<?>) type)
                    || type instanceof ParameterizedType
                            && (Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                                    || Provider.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                                    || Supplier.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))) {
                return;
            }

            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            String name = getConfigKey(injectionPoint, configProperty);

            // Check if the name is part of the properties first. Since properties can be a subset, then search for the actual property for a value.
            if (!configNames.contains(name) && ConfigProducerUtil.getRawValue(name, (SmallRyeConfig) config) == null) {
                if (configProperty.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                    adv.addDeploymentProblem(InjectionMessages.msg.noConfigValue(name));
                }
            }

            try {
                // Check if there is a Converter registed for the injected type
                Converter<?> resolvedConverter = ConfigProducerUtil.resolveConverter(injectionPoint, (SmallRyeConfig) config);

                // Check if the value can be converted. The TCK checks this, but this requires to get the value eagerly.
                // This should not be required!
                SecretKeys.doUnlocked(() -> ((SmallRyeConfig) config).getOptionalValue(name, resolvedConverter));
            } catch (IllegalArgumentException e) {
                adv.addDeploymentProblem(e);
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
                || requiredType == OptionalDouble.class
                || requiredType == Supplier.class
                || requiredType == ConfigValue.class;
    }
}
