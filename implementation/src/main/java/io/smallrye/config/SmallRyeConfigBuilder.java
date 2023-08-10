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

import static io.smallrye.config.ConfigSourceInterceptorFactory.DEFAULT_PRIORITY;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;
import static io.smallrye.config.ProfileConfigSourceInterceptor.convertProfile;
import static io.smallrye.config.PropertiesConfigSourceProvider.classPathSources;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigBuilder implements ConfigBuilder {
    public static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES = "META-INF/microprofile-config.properties";

    private final List<SmallRyeConfigBuilderCustomizer> customizers = new ArrayList<>();
    // sources are not sorted by their ordinals
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<ConfigSourceProvider> sourceProviders = new ArrayList<>();
    private final Map<Type, ConverterWithPriority> converters = new HashMap<>();
    private final List<String> profiles = new ArrayList<>();
    private final Set<String> secretKeys = new HashSet<>();
    private final List<InterceptorWithPriority> interceptors = new ArrayList<>();
    private final List<SecretKeysHandler> secretKeysHandlers = new ArrayList<>();
    private ConfigValidator validator = ConfigValidator.EMPTY;
    private final KeyMap<String> defaultValues = new KeyMap<>();
    private final ConfigMappingProvider.Builder mappingsBuilder = ConfigMappingProvider.builder();
    private ClassLoader classLoader = SecuritySupport.getContextClassLoader();
    private boolean addDiscoveredCustomizers = false;
    private boolean addDefaultSources = false;
    private boolean addDefaultInterceptors = false;
    private boolean addDiscoveredSources = false;
    private boolean addDiscoveredConverters = false;
    private boolean addDiscoveredInterceptors = false;
    private boolean addDiscoveredSecretKeysHandlers = false;
    private boolean addDiscoveredValidator = false;

    public SmallRyeConfigBuilder() {
    }

    public SmallRyeConfigBuilder addDiscoveredCustomizers() {
        addDiscoveredCustomizers = true;
        return this;
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

    public SmallRyeConfigBuilder addDiscoveredSecretKeysHandlers() {
        addDiscoveredSecretKeysHandlers = true;
        return this;
    }

    public SmallRyeConfigBuilder addDiscoveredValidator() {
        addDiscoveredValidator = true;
        return this;
    }

    List<ConfigSource> discoverSources() {
        List<ConfigSource> discoveredSources = new ArrayList<>();
        ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class, classLoader);
        for (ConfigSource source : configSourceLoader) {
            discoveredSources.add(source);
        }

        // load all ConfigSources from ConfigSourceProviders
        ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class,
                classLoader);
        for (ConfigSourceProvider configSourceProvider : configSourceProviderLoader) {
            for (ConfigSource configSource : configSourceProvider.getConfigSources(classLoader)) {
                discoveredSources.add(configSource);
            }
        }

        ServiceLoader<ConfigSourceFactory> configSourceFactoryLoader = ServiceLoader.load(ConfigSourceFactory.class,
                classLoader);
        for (ConfigSourceFactory factory : configSourceFactoryLoader) {
            discoveredSources.add(new ConfigurableConfigSource(factory));
        }

        return discoveredSources;
    }

    List<Converter<?>> discoverConverters() {
        List<Converter<?>> discoveredConverters = new ArrayList<>();
        for (Converter<?> converter : ServiceLoader.load(Converter.class, classLoader)) {
            discoveredConverters.add(converter);
        }
        return discoveredConverters;
    }

    List<InterceptorWithPriority> discoverInterceptors() {
        List<InterceptorWithPriority> interceptors = new ArrayList<>();
        ServiceLoader<ConfigSourceInterceptor> interceptorLoader = ServiceLoader.load(ConfigSourceInterceptor.class,
                classLoader);
        for (ConfigSourceInterceptor configSourceInterceptor : interceptorLoader) {
            interceptors.add(new InterceptorWithPriority(configSourceInterceptor));
        }

        ServiceLoader<ConfigSourceInterceptorFactory> interceptorFactoryLoader = ServiceLoader
                .load(ConfigSourceInterceptorFactory.class, classLoader);
        for (ConfigSourceInterceptorFactory interceptor : interceptorFactoryLoader) {
            interceptors.add(new InterceptorWithPriority(interceptor));
        }

        return interceptors;
    }

    ConfigValidator discoverValidator() {
        ServiceLoader<ConfigValidator> validatorLoader = ServiceLoader.load(ConfigValidator.class, classLoader);
        Iterator<ConfigValidator> iterator = validatorLoader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return ConfigValidator.EMPTY;
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
        defaultSources.addAll(classPathSources(META_INF_MICROPROFILE_CONFIG_PROPERTIES, classLoader));

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
                if (profiles.isEmpty()) {
                    profiles.addAll(getProfile(context));
                }
                return new ProfileConfigSourceInterceptor(profiles);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200);
            }

            private List<String> getProfile(final ConfigSourceInterceptorContext context) {
                List<String> profiles = new ArrayList<>();
                profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE_PARENT));
                profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE));
                return profiles;
            }

            private List<String> getProfiles(final ConfigSourceInterceptorContext context, final String propertyName) {
                List<String> profiles = new ArrayList<>();
                ConfigValue profileValue = context.proceed(propertyName);
                if (profileValue != null) {
                    final List<String> convertProfiles = convertProfile(profileValue.getValue());
                    for (String profile : convertProfiles) {
                        profiles.addAll(getProfiles(context, "%" + profile + "." + SMALLRYE_CONFIG_PROFILE_PARENT));
                        profiles.add(profile);
                    }
                }
                return profiles;
            }
        }));
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                Map<String, String> relocations = new HashMap<>();
                relocations.put(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE, Config.PROFILE);

                List<MultipleProfileProperty> multipleProfileProperties = new ArrayList<>();
                Iterator<String> names = context.iterateNames();
                while (names.hasNext()) {
                    String name = names.next();
                    if (name.length() > 0 && name.charAt(0) == '%') {
                        NameIterator ni = new NameIterator(name);
                        String profileSegment = ni.getNextSegment();
                        List<String> profiles = convertProfile(profileSegment.substring(1));
                        if (profiles.size() > 1) {
                            multipleProfileProperties
                                    .add(new MultipleProfileProperty(name, name.substring(profileSegment.length()), profiles));
                        }
                    }
                }

                // Ordered properties by least number of profiles. Priority to the ones with most specific profiles.
                for (MultipleProfileProperty multipleProfileProperty : multipleProfileProperties) {
                    for (String profile : multipleProfileProperty.getProfiles()) {
                        relocations.putIfAbsent("%" + profile + multipleProfileProperty.getRelocateName(),
                                multipleProfileProperty.getName());
                    }
                }

                return new RelocateConfigSourceInterceptor(relocations);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200 - 1);
            }

            class MultipleProfileProperty implements Comparable<MultipleProfileProperty> {
                private final String name;
                private final String relocateName;
                private final List<String> profiles;

                public MultipleProfileProperty(final String name, final String relocateName, final List<String> profiles) {
                    this.name = name;
                    this.relocateName = relocateName;
                    this.profiles = profiles;
                }

                public String getName() {
                    return name;
                }

                public String getRelocateName() {
                    return relocateName;
                }

                public List<String> getProfiles() {
                    return profiles;
                }

                @Override
                public int compareTo(final MultipleProfileProperty o) {
                    return Integer.compare(this.getProfiles().size(), o.getProfiles().size());
                }
            }
        }));
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                boolean expressions = true;
                ConfigValue expressionsValue = context.proceed(Config.PROPERTY_EXPRESSIONS_ENABLED);
                if (expressionsValue != null) {
                    expressions = Boolean.parseBoolean(expressionsValue.getValue());
                }
                return new ExpressionConfigSourceInterceptor(expressions);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 300);
            }
        }));

        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new SecretKeysConfigSourceInterceptor(SmallRyeConfigBuilder.this.secretKeys);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 100);
            }
        }));
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                if (isAddDiscoveredSecretKeysHandlers()) {
                    secretKeysHandlers.addAll(discoverSecretKeysHandlers(context));
                }
                return new SecretKeysHandlerConfigSourceInterceptor(
                        isAddDiscoveredSecretKeysHandlers() || !secretKeysHandlers.isEmpty(), secretKeysHandlers);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 310);
            }

            private List<String> getEnabledHandlers(final ConfigSourceInterceptorContext context) {
                ConfigValue enabledHandlers = context.proceed("smallrye.config.secret-handlers");
                if (enabledHandlers == null || enabledHandlers.getValue().equals("all")) {
                    return List.of();
                }

                List<String> handlers = newCollectionConverter(newTrimmingConverter(STRING_CONVERTER), ArrayList::new)
                        .convert(enabledHandlers.getValue());
                return handlers != null ? handlers : List.of();
            }

            private List<SecretKeysHandler> discoverSecretKeysHandlers(final ConfigSourceInterceptorContext context) {
                List<String> enabledHandlers = getEnabledHandlers(context);

                List<SecretKeysHandler> discoveredHandlers = new ArrayList<>();
                ServiceLoader<SecretKeysHandler> secretKeysHandlers = ServiceLoader.load(SecretKeysHandler.class, classLoader);
                for (SecretKeysHandler secretKeysHandler : secretKeysHandlers) {
                    if (enabledHandlers.isEmpty() || enabledHandlers.contains(secretKeysHandler.getName())) {
                        discoveredHandlers.add(secretKeysHandler);
                    }
                }

                ServiceLoader<SecretKeysHandlerFactory> secretKeysHandlerFactories = ServiceLoader
                        .load(SecretKeysHandlerFactory.class, classLoader);
                for (SecretKeysHandlerFactory secretKeysHandlerFactory : secretKeysHandlerFactories) {
                    if (enabledHandlers.isEmpty() || enabledHandlers.contains(secretKeysHandlerFactory.getName())) {
                        discoveredHandlers.add(
                                secretKeysHandlerFactory
                                        .getSecretKeysHandler(new ConfigSourceContext() {
                                            @Override
                                            public ConfigValue getValue(final String name) {
                                                return context.proceed(name);
                                            }

                                            @Override
                                            public List<String> getProfiles() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public Iterator<String> iterateNames() {
                                                return context.iterateNames();
                                            }
                                        }));
                    }
                }

                return discoveredHandlers;
            }
        }));

        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                boolean enabled = false;
                ConfigValue enabledValue = context.proceed(SMALLRYE_CONFIG_LOG_VALUES);
                if (enabledValue != null) {
                    enabled = Boolean.parseBoolean(enabledValue.getValue());
                }
                return new LoggingConfigSourceInterceptor(enabled);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 250);
            }
        }));

        return interceptors;
    }

    @Override
    public SmallRyeConfigBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public SmallRyeConfigBuilder withCustomizers(SmallRyeConfigBuilderCustomizer... customizers) {
        Collections.addAll(this.customizers, customizers);
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

    public SmallRyeConfigBuilder withSources(ConfigSourceProvider provider) {
        sourceProviders.add(provider);
        return this;
    }

    public SmallRyeConfigBuilder withSources(ConfigSourceFactory... configSourceFactories) {
        for (ConfigSourceFactory configSourceFactory : configSourceFactories) {
            sources.add(new ConfigurableConfigSource(configSourceFactory));
        }
        return this;
    }

    public SmallRyeConfigBuilder withInterceptors(ConfigSourceInterceptor... interceptors) {
        for (ConfigSourceInterceptor interceptor : interceptors) {
            this.interceptors.add(new InterceptorWithPriority(interceptor));
        }
        return this;
    }

    public SmallRyeConfigBuilder withInterceptorFactories(ConfigSourceInterceptorFactory... interceptorFactories) {
        for (ConfigSourceInterceptorFactory interceptorFactory : interceptorFactories) {
            this.interceptors.add(new InterceptorWithPriority(interceptorFactory));
        }
        return this;
    }

    public SmallRyeConfigBuilder withSecretKeysHandlers(SecretKeysHandler... secretKeysHandler) {
        this.secretKeysHandlers.addAll(Arrays.asList(secretKeysHandler));
        return this;
    }

    public SmallRyeConfigBuilder withProfile(String profile) {
        addDefaultInterceptors();
        this.profiles.addAll(convertProfile(profile));
        return this;
    }

    public SmallRyeConfigBuilder withProfiles(List<String> profiles) {
        addDefaultInterceptors();
        this.profiles.addAll(profiles);
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
        for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
            this.defaultValues.findOrAdd(entry.getKey()).putRootValue(entry.getValue());
        }
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
        withDefaultValue(SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, Boolean.toString(validateUnknown));
        return this;
    }

    public SmallRyeConfigBuilder withValidator(ConfigValidator validator) {
        this.validator = validator;
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

    public List<ConfigSource> getSources() {
        return sources;
    }

    public List<ConfigSourceProvider> getSourceProviders() {
        return sourceProviders;
    }

    public Map<Type, ConverterWithPriority> getConverters() {
        return converters;
    }

    public List<InterceptorWithPriority> getInterceptors() {
        return interceptors;
    }

    public ConfigValidator getValidator() {
        if (isAddDiscoveredValidator()) {
            this.validator = discoverValidator();
        }
        return validator;
    }

    public KeyMap<String> getDefaultValues() {
        return defaultValues;
    }

    ClassLoader getClassLoader() {
        return classLoader;
    }

    public boolean isAddDiscoveredCustomizers() {
        return addDiscoveredCustomizers;
    }

    public boolean isAddDefaultSources() {
        return addDefaultSources;
    }

    public boolean isAddDefaultInterceptors() {
        return addDefaultInterceptors;
    }

    public boolean isAddDiscoveredSources() {
        return addDiscoveredSources;
    }

    public boolean isAddDiscoveredConverters() {
        return addDiscoveredConverters;
    }

    public boolean isAddDiscoveredInterceptors() {
        return addDiscoveredInterceptors;
    }

    public boolean isAddDiscoveredSecretKeysHandlers() {
        return addDiscoveredSecretKeysHandlers;
    }

    public boolean isAddDiscoveredValidator() {
        return addDiscoveredValidator;
    }

    public SmallRyeConfigBuilder setAddDefaultSources(final boolean addDefaultSources) {
        this.addDefaultSources = addDefaultSources;
        return this;
    }

    public SmallRyeConfigBuilder setAddDefaultInterceptors(final boolean addDefaultInterceptors) {
        this.addDefaultInterceptors = addDefaultInterceptors;
        return this;
    }

    public SmallRyeConfigBuilder setAddDiscoveredSources(final boolean addDiscoveredSources) {
        this.addDiscoveredSources = addDiscoveredSources;
        return this;
    }

    public SmallRyeConfigBuilder setAddDiscoveredConverters(final boolean addDiscoveredConverters) {
        this.addDiscoveredConverters = addDiscoveredConverters;
        return this;
    }

    public SmallRyeConfigBuilder setAddDiscoveredInterceptors(final boolean addDiscoveredInterceptors) {
        this.addDiscoveredInterceptors = addDiscoveredInterceptors;
        return this;
    }

    public SmallRyeConfigBuilder setAddDiscoveredSecretKeysHandlers(final boolean addDiscoveredSecretKeysHandlers) {
        this.addDiscoveredSecretKeysHandlers = addDiscoveredSecretKeysHandlers;
        return this;
    }

    public SmallRyeConfigBuilder setAddDiscoveredValidator(final boolean addDiscoveredValidator) {
        this.addDiscoveredValidator = addDiscoveredValidator;
        return this;
    }

    @Override
    public SmallRyeConfig build() {
        if (addDiscoveredCustomizers) {
            for (SmallRyeConfigBuilderCustomizer customizer : ServiceLoader.load(SmallRyeConfigBuilderCustomizer.class,
                    classLoader)) {
                customizers.add(customizer);
            }
        }

        customizers.stream()
                .sorted(Comparator.comparingInt(SmallRyeConfigBuilderCustomizer::priority))
                .forEach(customizer -> customizer.configBuilder(SmallRyeConfigBuilder.this));

        ConfigMappingProvider mappingProvider = mappingsBuilder.build();
        defaultValues.putAll(mappingProvider.getDefaultValues());

        SmallRyeConfig config = new SmallRyeConfig(this);
        ConfigMappings.mapConfiguration(config, mappingProvider);
        return config;
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

    static class InterceptorWithPriority implements Comparable<InterceptorWithPriority> {
        private final ConfigSourceInterceptorFactory factory;
        private final int priority;

        InterceptorWithPriority(ConfigSourceInterceptor interceptor) {
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

                    return OptionalInt.of(InterceptorWithPriority.getPriority(interceptor.getClass()));
                }
            });
        }

        InterceptorWithPriority(ConfigSourceInterceptorFactory factory) {
            this.factory = factory;
            this.priority = factory.getPriority().orElse(DEFAULT_PRIORITY);
        }

        ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
            return factory.getInterceptor(context);
        }

        int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(final InterceptorWithPriority other) {
            return Integer.compare(this.priority, other.priority);
        }

        @SuppressWarnings("unchecked")
        private static int getPriority(final Class<? extends ConfigSourceInterceptor> klass) {
            Priority priorityAnnotation = klass.getAnnotation(Priority.class);
            if (priorityAnnotation != null) {
                return priorityAnnotation.value();
            } else {
                Class<?> parentClass = klass.getSuperclass();
                if (ConfigSourceInterceptor.class.isAssignableFrom(parentClass)) {
                    return getPriority((Class<? extends ConfigSourceInterceptor>) parentClass);
                }
                return DEFAULT_PRIORITY;
            }
        }
    }
}
