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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigBuilder implements ConfigBuilder {

    private static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES = "META-INF/microprofile-config.properties";
    private static final String WEB_INF_MICROPROFILE_CONFIG_PROPERTIES = "WEB-INF/classes/META-INF/microprofile-config.properties";

    // sources are not sorted by their ordinals
    private final List<ConfigSource> sources = new ArrayList<>();
    private Function<ConfigSource, ConfigSource> sourceWrappers = UnaryOperator.identity();
    private final Map<Type, ConverterWithPriority> converters = new HashMap<>();
    private String profile = null;
    private final Set<String> secretKeys = new HashSet<>();
    private final List<InterceptorWithPriority> interceptors = new ArrayList<>();
    private final KeyMap<String> defaultValues = new KeyMap<>();
    private final ConfigMappingProvider.Builder mappingsBuilder = ConfigMappingProvider.builder();
    private ClassLoader classLoader = SecuritySupport.getContextClassLoader();
    private boolean addDefaultSources = false;
    private boolean addDefaultInterceptors = false;
    private boolean addDiscoveredSources = false;
    private boolean addDiscoveredConverters = false;
    private boolean addDiscoveredInterceptors = false;

    public SmallRyeConfigBuilder() {
    }

    @Override
    public SmallRyeConfigBuilder addDiscoveredSources() {
        addDiscoveredSources = true;
        return this;
    }

    @Override
    public SmallRyeConfigBuilder addDiscoveredConverters() {
        addDiscoveredConverters = true;
        return this;
    }

    public SmallRyeConfigBuilder addDiscoveredInterceptors() {
        addDiscoveredInterceptors = true;
        return this;
    }

    List<ConfigSource> discoverSources() {
        List<ConfigSource> discoveredSources = new ArrayList<>();
        ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class, classLoader);
        configSourceLoader.forEach(discoveredSources::add);

        // load all ConfigSources from ConfigSourceProviders
        ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class,
                classLoader);
        configSourceProviderLoader.forEach(configSourceProvider -> configSourceProvider.getConfigSources(classLoader)
                .forEach(discoveredSources::add));

        ServiceLoader<ConfigSourceFactory> configSourceFactoryLoader = ServiceLoader.load(ConfigSourceFactory.class,
                classLoader);
        configSourceFactoryLoader.forEach(factory -> discoveredSources.add(new ConfigurableConfigSource(factory)));

        return discoveredSources;
    }

    List<Converter<?>> discoverConverters() {
        List<Converter<?>> discoveredConverters = new ArrayList<>();
        ServiceLoader.load(Converter.class, classLoader).forEach(discoveredConverters::add);
        return discoveredConverters;
    }

    List<InterceptorWithPriority> discoverInterceptors() {
        List<InterceptorWithPriority> interceptors = new ArrayList<>();
        ServiceLoader<ConfigSourceInterceptor> interceptorLoader = ServiceLoader.load(ConfigSourceInterceptor.class,
                classLoader);
        interceptorLoader.forEach(interceptor -> interceptors.add(new InterceptorWithPriority(interceptor)));

        ServiceLoader<ConfigSourceInterceptorFactory> interceptorFactoryLoader = ServiceLoader
                .load(ConfigSourceInterceptorFactory.class, classLoader);
        interceptorFactoryLoader.forEach(interceptor -> interceptors.add(new InterceptorWithPriority(interceptor)));

        return interceptors;
    }

    @Override
    public SmallRyeConfigBuilder addDefaultSources() {
        addDefaultSources = true;
        return this;
    }

    protected List<ConfigSource> getDefaultSources() {
        List<ConfigSource> defaultSources = new ArrayList<>();

        defaultSources.add(new EnvConfigSource());
        defaultSources.add(new SysPropConfigSource());
        defaultSources.addAll(new PropertiesConfigSourceProvider(META_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader)
                .getConfigSources(classLoader));
        defaultSources.addAll(new PropertiesConfigSourceProvider(WEB_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader)
                .getConfigSources(classLoader));
        defaultSources
                .addAll(new ProfilePropertiesConfigSourceProvider(META_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader)
                        .getConfigSources(classLoader));
        defaultSources
                .addAll(new ProfilePropertiesConfigSourceProvider(WEB_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader)
                        .getConfigSources(classLoader));

        return defaultSources;
    }

    public SmallRyeConfigBuilder addDefaultInterceptors() {
        this.addDefaultInterceptors = true;
        return this;
    }

    List<InterceptorWithPriority> getDefaultInterceptors() {
        final List<InterceptorWithPriority> interceptors = new ArrayList<>();

        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return profile != null && !profile.isEmpty() ? new ProfileConfigSourceInterceptor(profile)
                        : new ProfileConfigSourceInterceptor(context);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 600);
            }
        }));
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                final Map<String, String> relocations = new HashMap<>();
                relocations.put(ProfileConfigSourceInterceptor.SMALLRYE_PROFILE, "mp.config.profile");
                return new RelocateConfigSourceInterceptor(relocations);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 600 - 1);
            }
        }));
        interceptors.add(new InterceptorWithPriority(new ExpressionConfigSourceInterceptor()));
        interceptors.add(new InterceptorWithPriority(new SecretKeysConfigSourceInterceptor(secretKeys)));

        return interceptors;
    }

    @Override
    public SmallRyeConfigBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public SmallRyeConfigBuilder withSources(ConfigSource... configSources) {
        Collections.addAll(sources, configSources);
        return this;
    }

    public SmallRyeConfigBuilder withSources(Collection<ConfigSource> configSources) {
        sources.addAll(configSources);
        return this;
    }

    public SmallRyeConfigBuilder withSources(ConfigSourceFactory... configSourceFactories) {
        Stream.of(configSourceFactories).forEach(configSourceFactory -> {
            sources.add(new ConfigurableConfigSource(configSourceFactory));
        });
        return this;
    }

    public SmallRyeConfigBuilder withInterceptors(ConfigSourceInterceptor... interceptors) {
        this.interceptors.addAll(Stream.of(interceptors)
                .map(InterceptorWithPriority::new)
                .collect(Collectors.toList()));
        return this;
    }

    public SmallRyeConfigBuilder withInterceptorFactories(ConfigSourceInterceptorFactory... interceptorFactories) {
        this.interceptors.addAll(Stream.of(interceptorFactories)
                .map(InterceptorWithPriority::new)
                .collect(Collectors.toList()));
        return this;
    }

    public SmallRyeConfigBuilder withProfile(String profile) {
        addDefaultInterceptors();
        this.profile = profile;
        return this;
    }

    public SmallRyeConfigBuilder withSecretKeys(String... keys) {
        secretKeys.addAll(Stream.of(keys).collect(Collectors.toSet()));
        return this;
    }

    public SmallRyeConfigBuilder withDefaultValue(String name, String value) {
        this.defaultValues.findOrAdd(name).putRootValue(value);
        return this;
    }

    public SmallRyeConfigBuilder withDefaultValues(Map<String, String> defaultValues) {
        defaultValues.forEach((key, value) -> this.defaultValues.findOrAdd(key).putRootValue(value));
        return this;
    }

    public SmallRyeConfigBuilder withMapping(Class<?> klass) {
        return withMapping(klass, ConfigMappings.getPrefix(klass));
    }

    public SmallRyeConfigBuilder withMapping(Class<?> klass, String prefix) {
        mappingsBuilder.addRoot(prefix, klass);
        return this;
    }

    public SmallRyeConfigBuilder withMappingIgnore(String path) {
        mappingsBuilder.addIgnored(path);
        return this;
    }

    public SmallRyeConfigBuilder withValidateUnknown(boolean validateUnknown) {
        mappingsBuilder.validateUnknown(validateUnknown);
        return this;
    }

    @Override
    public SmallRyeConfigBuilder withConverters(Converter<?>[] converters) {
        for (Converter<?> converter : converters) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                throw ConfigMessages.msg.unableToAddConverter(converter);
            }
            addConverter(type, getPriority(converter), converter, this.converters);
        }
        return this;
    }

    @Override
    public <T> SmallRyeConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        addConverter(type, priority, converter, converters);
        return this;
    }

    @Deprecated
    public SmallRyeConfigBuilder withWrapper(UnaryOperator<ConfigSource> wrapper) {
        sourceWrappers = sourceWrappers.andThen(wrapper);
        return this;
    }

    static void addConverter(Type type, Converter<?> converter, Map<Type, ConverterWithPriority> converters) {
        addConverter(type, getPriority(converter), converter, converters);
    }

    static void addConverter(Type type, int priority, Converter<?> converter,
            Map<Type, ConverterWithPriority> converters) {
        // add the converter only if it has a higher priority than another converter for the same type
        ConverterWithPriority oldConverter = converters.get(type);
        if (oldConverter == null || priority > oldConverter.priority) {
            converters.put(type, new ConverterWithPriority(converter, priority));
        }
    }

    private static int getPriority(Converter<?> converter) {
        int priority = 100;
        Priority priorityAnnotation = converter.getClass().getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }
        return priority;
    }

    protected List<ConfigSource> getSources() {
        return sources;
    }

    @Deprecated
    Function<ConfigSource, ConfigSource> getSourceWrappers() {
        return sourceWrappers;
    }

    protected Map<Type, ConverterWithPriority> getConverters() {
        return converters;
    }

    List<InterceptorWithPriority> getInterceptors() {
        return interceptors;
    }

    KeyMap<String> getDefaultValues() {
        return defaultValues;
    }

    protected boolean isAddDefaultSources() {
        return addDefaultSources;
    }

    boolean isAddDefaultInterceptors() {
        return addDefaultInterceptors;
    }

    protected boolean isAddDiscoveredSources() {
        return addDiscoveredSources;
    }

    protected boolean isAddDiscoveredConverters() {
        return addDiscoveredConverters;
    }

    boolean isAddDiscoveredInterceptors() {
        return addDiscoveredInterceptors;
    }

    @Override
    public SmallRyeConfig build() {
        ConfigMappingProvider mappingProvider = mappingsBuilder.build();
        KeyMap<String> mappingProviderDefaultValues = mappingProvider.getDefaultValues();
        mappingProviderDefaultValues.forEach(defaultValues::putIfAbsent);

        try {
            ConfigMappings configMappings = new ConfigMappings();
            SmallRyeConfig config = new SmallRyeConfig(this, configMappings);
            mappingProvider.mapConfiguration(config, configMappings);
            return config;
        } catch (ConfigValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    static class ConverterWithPriority {
        private final Converter<?> converter;
        private final int priority;

        private ConverterWithPriority(Converter<?> converter, int priority) {
            this.converter = converter;
            this.priority = priority;
        }

        Converter<?> getConverter() {
            return converter;
        }
    }

    static class InterceptorWithPriority {
        private static final OptionalInt OPTIONAL_DEFAULT_PRIORITY = OptionalInt
                .of(ConfigSourceInterceptorFactory.DEFAULT_PRIORITY);

        private final ConfigSourceInterceptorFactory factory;
        private final int priority;

        private InterceptorWithPriority(ConfigSourceInterceptor interceptor) {
            this(new ConfigSourceInterceptorFactory() {
                @Override
                public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                    return interceptor;
                }

                @Override
                public OptionalInt getPriority() {
                    final OptionalInt priority = ConfigSourceInterceptorFactory.super.getPriority();
                    if (priority.isPresent()) {
                        return priority;
                    }

                    return Optional.ofNullable(interceptor.getClass().getAnnotation(Priority.class))
                            .map(priority1 -> OptionalInt.of(priority1.value()))
                            .orElse(OPTIONAL_DEFAULT_PRIORITY);
                }
            });
        }

        private InterceptorWithPriority(ConfigSourceInterceptorFactory factory) {
            this.factory = factory;
            this.priority = factory.getPriority().orElse(ConfigSourceInterceptorFactory.DEFAULT_PRIORITY);
        }

        ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
            return factory.getInterceptor(context);
        }

        int getPriority() {
            return priority;
        }
    }
}
