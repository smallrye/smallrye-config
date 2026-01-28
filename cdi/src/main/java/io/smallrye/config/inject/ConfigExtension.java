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

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.inject.ConfigProducer.isClassHandledByConfigProducer;
import static io.smallrye.config.inject.InjectionMessages.formatInjectionPoint;
import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;

/**
 * CDI Extension to produces Config bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ConfigExtension implements Extension {
    private final Set<InjectionPoint> configPropertyInjectionPoints = new HashSet<>();
    /** ConfigProperties for SmallRye Config */
    private final Set<ConfigClass> configProperties = new HashSet<>();
    /** ConfigProperties for CDI */
    private final Set<ConfigClass> configPropertiesBeans = new HashSet<>();
    /** ConfigMappings for SmallRye Config */
    private final Set<ConfigClass> configMappings = new HashSet<>();
    /** ConfigMappings for CDI */
    private final Set<ConfigClass> configMappingBeans = new HashSet<>();

    public ConfigExtension() {
    }

    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
        bbd.addAnnotatedType(configBean, ConfigProducer.class.getName());

        // Remove NonBinding annotation. OWB is not able to look up CDI beans programmatically with NonBinding in the
        // case the look-up changed the non-binding parameters (in this case the prefix)
        AnnotatedTypeConfigurator<ConfigProperties> configPropertiesConfigurator = bbd
                .configureQualifier(ConfigProperties.class);
        configPropertiesConfigurator.methods().forEach(methodConfigurator -> methodConfigurator
                .remove(annotation -> annotation.annotationType().equals(Nonbinding.class)));
    }

    protected void processConfigProperties(
            @Observes @WithAnnotations(ConfigProperties.class) ProcessAnnotatedType<?> processAnnotatedType) {
        // Even if we filter in the CDI event, beans containing injection points of ConfigProperties are also fired.
        AnnotatedType<?> annotatedType = processAnnotatedType.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(ConfigProperties.class)) {
            // We are going to veto, because it may be a managed bean, and we will use a configurator bean
            processAnnotatedType.veto();

            // Unconfigured is represented as an empty String in SmallRye Config
            String prefix = annotatedType.getAnnotation(ConfigProperties.class).prefix();
            if (ConfigProperties.UNCONFIGURED_PREFIX.equals(prefix)) {
                prefix = "";
            }

            // Each config class is both in SmallRyeConfig and managed by a configurator bean.
            // CDI requires more beans for injection points due to binding prefix.
            ConfigClass properties = ConfigClass.configClass(annotatedType.getJavaClass(), prefix);
            configProperties.add(properties);
            configPropertiesBeans.add(properties);
        }
    }

    protected void processConfigMappings(
            @Observes @WithAnnotations(ConfigMapping.class) ProcessAnnotatedType<?> processAnnotatedType) {
        // Even if we filter in the CDI event, beans containing injection points of ConfigMapping are also fired.
        AnnotatedType<?> annotatedType = processAnnotatedType.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(ConfigMapping.class)) {
            // We are going to veto, because it may be a managed bean, and we will use a configurator bean
            processAnnotatedType.veto();

            // Each config class is both in SmallRyeConfig and managed by a configurator bean.
            // CDI requires a single configurator bean per class due to non-binding prefix.
            ConfigClass mapping = ConfigClass.configClass(annotatedType.getJavaClass());
            configMappings.add(mapping);
            configMappingBeans.add(mapping);
        }
    }

    protected void processConfigInjectionPoints(@Observes ProcessInjectionPoint<?, ?> pip) {
        InjectionPoint injectionPoint = pip.getInjectionPoint();
        if (injectionPoint.getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
            configPropertyInjectionPoints.add(injectionPoint);
        }

        if (injectionPoint.getAnnotated().isAnnotationPresent(ConfigProperties.class)) {
            ConfigClass properties = ConfigClass.configClass((Class<?>) injectionPoint.getType(),
                    injectionPoint.getAnnotated().getAnnotation(ConfigProperties.class).prefix());

            // If the prefix is empty at the injection point, fallbacks to the class prefix (already registered)
            if (!properties.getPrefix().equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
                configProperties.add(properties);
            }
            // Cover all combinations of the configurator bean for ConfigProperties because prefix is binding
            configPropertiesBeans.add(properties);
        }

        // Add to SmallRyeConfig config classes with a different prefix by injection point
        if (injectionPoint.getAnnotated().isAnnotationPresent(ConfigMapping.class)) {
            ConfigClass mapping = ConfigClass.configClass((Class<?>) injectionPoint.getType(),
                    injectionPoint.getAnnotated().getAnnotation(ConfigMapping.class).prefix());
            // If the prefix is empty at the injection point, fallbacks to the class prefix (already registered)
            if (!mapping.getPrefix().isEmpty()) {
                configMappings.add(mapping);
            }
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

        customTypes.forEach(customType -> abd.addBean(new ConfigInjectionBean<>(bm, customType)));
        configPropertiesBeans.forEach(properties -> abd.addBean(new ConfigPropertiesInjectionBean<>(properties)));
        configMappingBeans.forEach(mapping -> abd.addBean(new ConfigMappingInjectionBean<>(mapping, bm)));
    }

    protected void validate(@Observes AfterDeploymentValidation adv) {
        SmallRyeConfig config = Config.getOrCreate(getContextClassLoader()).unwrap(SmallRyeConfig.class);
        Set<String> configNames = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        for (InjectionPoint injectionPoint : getConfigPropertyInjectionPoints()) {
            Type type = injectionPoint.getType();

            // validate injection config name
            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            String name;
            try {
                name = ConfigProducerUtil.getConfigKey(injectionPoint, configProperty);
            } catch (IllegalStateException e) {
                adv.addDeploymentProblem(InjectionMessages.msg.retrieveConfigFailure(null, formatInjectionPoint(injectionPoint),
                        e.getLocalizedMessage(), e));
                continue;
            }

            // We don't validate the Optional / Provider / Supplier / ConfigValue for defaultValue.
            if (type instanceof Class && org.eclipse.microprofile.config.ConfigValue.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalInt.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalLong.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalDouble.class.isAssignableFrom((Class<?>) type)
                    || type instanceof ParameterizedType
                            && (Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                                    || Provider.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                                    || Supplier.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))) {
                continue;
            }

            // Check if the name is part of the properties first.
            // Since properties can be a subset, then search for the actual property for a value.
            // Check if the property is indexed (might be a Collection with indexed properties).
            // Check if the properti is part of a Map
            if (!configNames.contains(name)
                    && ConfigProducerUtil.getConfigValue(name, config).getValue() == null
                    && !isIndexed(type, name, config)
                    && !isMap(type, name, config)) {

                if (configProperty.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                    adv.addDeploymentProblem(
                            InjectionMessages.msg.noConfigValue(name, formatInjectionPoint(injectionPoint)));
                    continue;
                }
            }

            try {
                // Check if the value can be injected. This may cause duplicated config reads (to validate and to inject).
                ConfigProducerUtil.getValue(injectionPoint, config);
            } catch (Exception e) {
                adv.addDeploymentProblem(InjectionMessages.msg.retrieveConfigFailure(name, formatInjectionPoint(injectionPoint),
                        e.getLocalizedMessage(), e));
            }
        }

        try {
            registerConfigMappings(config, configMappings);
            registerConfigProperties(config, configProperties);
        } catch (ConfigValidationException e) {
            adv.addDeploymentProblem(e);
        }
    }

    protected Set<InjectionPoint> getConfigPropertyInjectionPoints() {
        return configPropertyInjectionPoints;
    }

    private static boolean isIndexed(Type type, String name, SmallRyeConfig config) {
        return type instanceof ParameterizedType &&
                (List.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()) ||
                        Set.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
                && !config.getIndexedPropertiesIndexes(name).isEmpty();
    }

    private static boolean isMap(final Type type, String name, SmallRyeConfig config) {
        return type instanceof ParameterizedType
                && Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                && !config.getMapKeys(name).isEmpty();
    }
}
