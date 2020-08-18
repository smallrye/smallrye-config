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

import static io.smallrye.config.ConfigMappings.ConfigMappingWithPrefix.configMappingWithPrefix;
import static io.smallrye.config.inject.ConfigProducer.isClassHandledByConfigProducer;
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
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;

/**
 * CDI Extension to produces Config bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ConfigExtension implements Extension {
    private final Set<InjectionPoint> configPropertyInjectionPoints = new HashSet<>();

    private final Set<AnnotatedType<?>> configMappings = new HashSet<>();
    private final Set<InjectionPoint> configMappingInjectionPoints = new HashSet<>();

    public ConfigExtension() {
    }

    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
        bbd.addAnnotatedType(configBean, ConfigProducer.class.getName());
    }

    protected void processConfigMappings(
            @Observes @WithAnnotations({ ConfigProperties.class,
                    ConfigMapping.class }) ProcessAnnotatedType<?> processAnnotatedType) {

        // Even if we filter in the CDI event, beans containing injection points of ConfigMapping are also fired.
        if (processAnnotatedType.getAnnotatedType().isAnnotationPresent(ConfigProperties.class)
                || processAnnotatedType.getAnnotatedType().isAnnotationPresent(ConfigMapping.class)) {
            // We are going to veto, because it may be a managed bean and we will use a configurator bean
            processAnnotatedType.veto();
            configMappings.add(processAnnotatedType.getAnnotatedType());
        }
    }

    protected void collectConfigPropertyInjectionPoints(@Observes ProcessInjectionPoint<?, ?> pip) {
        if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
            configPropertyInjectionPoints.add(pip.getInjectionPoint());
        }

        if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperties.class)
                || pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigMapping.class)) {
            configMappingInjectionPoints.add(pip.getInjectionPoint());
        }
    }

    protected void registerCustomBeans(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        Set<Class<?>> customTypes = new HashSet<>();
        for (InjectionPoint ip : configPropertyInjectionPoints) {
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
            } else if (requiredType instanceof Class && !isClassHandledByConfigProducer(requiredType)) {
                // type is not produced by ConfigProducer
                customTypes.add((Class<?>) requiredType);
            }
        }

        customTypes.stream().map(customType -> new ConfigInjectionBean<>(bm, customType)).forEach(abd::addBean);
        configMappings.stream().map(annotatedType -> new ConfigMappingInjectionBean<>(bm, annotatedType)).forEach(abd::addBean);
    }

    protected void validate(@Observes AfterDeploymentValidation adv) {
        Config config = ConfigProvider.getConfig(getContextClassLoader());
        Set<String> configNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        for (InjectionPoint injectionPoint : configPropertyInjectionPoints) {
            Type type = injectionPoint.getType();

            // We don't validate the Optional / Provider / Supplier / ConfigValue for defaultValue.
            if (type instanceof Class && org.eclipse.microprofile.config.ConfigValue.class.isAssignableFrom((Class<?>) type)
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
            String name = ConfigProducerUtil.getConfigKey(injectionPoint, configProperty);

            // Check if the name is part of the properties first. Since properties can be a subset, then search for the actual property for a value.
            if (!configNames.contains(name) && ConfigProducerUtil.getRawValue(name, (SmallRyeConfig) config) == null) {
                if (configProperty.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                    adv.addDeploymentProblem(InjectionMessages.msg.noConfigValue(name));
                }
            }

            try {
                // Check if the value can be injected. This may cause duplicated config reads (to validate and to inject).
                ConfigProducerUtil.getValue(injectionPoint, config);
            } catch (IllegalArgumentException e) {
                adv.addDeploymentProblem(e);
            }
        }

        Set<ConfigMappings.ConfigMappingWithPrefix> configMappingsWithPrefix = new HashSet<>();
        for (AnnotatedType<?> configMapping : configMappings) {
            configMappingsWithPrefix.add(configMappingWithPrefix(configMapping.getJavaClass(),
                    ConfigMappingInjectionBean.getConfigMappingPrefix(configMapping)));
        }

        for (InjectionPoint injectionPoint : configMappingInjectionPoints) {
            configMappingsWithPrefix.add(configMappingWithPrefix((Class<?>) injectionPoint.getType(),
                    ConfigMappingInjectionBean.getConfigMappingPrefix(injectionPoint.getAnnotated())));
        }

        try {
            ((SmallRyeConfig) config).getConfigMappings().registerConfigMappings((SmallRyeConfig) config,
                    configMappingsWithPrefix);
        } catch (ConfigValidationException e) {
            adv.addDeploymentProblem(e);
        }
    }

    protected Set<InjectionPoint> getConfigPropertyInjectionPoints() {
        return configPropertyInjectionPoints;
    }
}
