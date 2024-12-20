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

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingClass;
import static io.smallrye.config.ConfigSourceInterceptorFactory.DEFAULT_PRIORITY;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;
import static io.smallrye.config.ProfileConfigSourceInterceptor.convertProfile;
import static io.smallrye.config.PropertiesConfigSourceLoader.inClassPath;
import static io.smallrye.config.PropertiesConfigSourceLoader.inFileSystem;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_SECRET_HANDLERS;

import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config._private.ConfigMessages;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigBuilder implements ConfigBuilder {
    private final List<SmallRyeConfigBuilderCustomizer> customizers = new ArrayList<>();
    // sources are not sorted by their ordinals
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<ConfigSourceProvider> sourceProviders = new ArrayList<>();
    private final Map<Type, ConverterWithPriority> converters = new HashMap<>();
    private final List<String> profiles = new ArrayList<>();
    private final Set<String> secretKeys = new HashSet<>();
    private final List<InterceptorWithPriority> interceptors = new ArrayList<>();
    private final List<SecretKeysHandlerWithName> secretKeysHandlers = new ArrayList<>();
    private ConfigValidator validator = ConfigValidator.EMPTY;
    private final Map<String, String> defaultValues = new HashMap<>();
    private final MappingBuilder mappingsBuilder = new MappingBuilder();
    private ClassLoader classLoader = SecuritySupport.getContextClassLoader();
    private boolean addDiscoveredCustomizers = false;
    private boolean addDefaultSources = false;
    private boolean addSystemSources = false;
    private boolean addPropertiesSources = false;
    private boolean addDefaultInterceptors = false;
    private boolean addDiscoveredSources = false;
    private boolean addDiscoveredConverters = false;
    private boolean addDiscoveredInterceptors = false;
    private boolean addDiscoveredSecretKeysHandlers = false;
    private boolean addDiscoveredValidator = false;

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

    public SmallRyeConfigBuilder addSystemSources() {
        addSystemSources = true;
        return this;
    }

    public SmallRyeConfigBuilder addPropertiesSources() {
        addPropertiesSources = true;
        return this;
    }

    protected List<ConfigSource> getDefaultSources() {
        List<ConfigSource> defaultSources = new ArrayList<>();
        defaultSources.addAll(getSystemSources());
        defaultSources.addAll(getPropertiesSources());
        return defaultSources;
    }

    protected List<ConfigSource> getSystemSources() {
        List<ConfigSource> sources = new ArrayList<>();
        sources.add(new SysPropConfigSource());
        sources.add(new EnvConfigSource());
        sources.addAll(new DotEnvConfigSourceProvider().getConfigSources(classLoader));
        return sources;
    }

    protected List<ConfigSource> getPropertiesSources() {
        List<ConfigSource> sources = new ArrayList<>();
        sources.addAll(
                inFileSystem(Paths.get(System.getProperty("user.dir"), "config", "application.properties").toUri().toString(),
                        260, classLoader));
        sources.addAll(inClassPath("application.properties", 250, classLoader));
        sources.addAll(inClassPath("META-INF/microprofile-config.properties", 100, classLoader));
        return sources;
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
                Set<String> profiles = new LinkedHashSet<>();
                profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE_PARENT));
                profiles.addAll(getProfiles(context, SMALLRYE_CONFIG_PROFILE));
                return new ArrayList<>(profiles);
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
                    if (!name.isEmpty() && name.charAt(0) == '%') {
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

                List<SecretKeysHandler> secretKeysHandlers = new ArrayList<>();
                for (SecretKeysHandlerWithName secretKeysHandler : SmallRyeConfigBuilder.this.secretKeysHandlers) {
                    secretKeysHandlers.add(secretKeysHandler.getSecretKeysHandler(new ConfigSourceContext() {
                        @Override
                        public ConfigValue getValue(final String name) {
                            return new ExpressionConfigSourceInterceptor().getValue(context, name);
                        }

                        @Override
                        public Iterator<String> iterateNames() {
                            return context.iterateNames();
                        }
                    }));
                }

                if (isAddDiscoveredSecretKeysHandlers()) {
                    secretKeysHandlers.addAll(discoverSecretKeysHandlers(context));
                }

                return new ExpressionConfigSourceInterceptor(expressions, secretKeysHandlers);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 300);
            }

            private List<String> getEnabledHandlers(final ConfigSourceInterceptorContext context) {
                ConfigValue enabledHandlers = context.proceed(SMALLRYE_CONFIG_SECRET_HANDLERS);
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

    public SmallRyeConfigBuilder withSecretKeysHandlers(SecretKeysHandler... secretKeysHandlers) {
        for (SecretKeysHandler secretKeysHandler : secretKeysHandlers) {
            this.secretKeysHandlers.add(new SecretKeysHandlerWithName(secretKeysHandler));
        }
        return this;
    }

    public SmallRyeConfigBuilder withSecretKeyHandlerFactories(SecretKeysHandlerFactory... secretKeyHandlerFactories) {
        for (SecretKeysHandlerFactory secretKeyHandlerFactory : secretKeyHandlerFactories) {
            this.secretKeysHandlers.add(new SecretKeysHandlerWithName(secretKeyHandlerFactory));
        }
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
        this.defaultValues.put(name, value);
        return this;
    }

    public SmallRyeConfigBuilder withDefaultValues(Map<String, String> defaultValues) {
        this.defaultValues.putAll(defaultValues);
        return this;
    }

    public SmallRyeConfigBuilder withMapping(Class<?> klass) {
        return withMapping(ConfigClass.configClass(klass));
    }

    public SmallRyeConfigBuilder withMapping(ConfigClass configClass) {
        mappingsBuilder.mapping(configClass);
        return this;
    }

    public SmallRyeConfigBuilder withMapping(Class<?> klass, String prefix) {
        mappingsBuilder.mapping(klass, prefix);
        return this;
    }

    /**
     * Ignores a specified path segment when performing a Config Mapping.
     * <p>
     * By default, a Config Mapping must match every configuration path available in the Config system. However, such
     * conditions may not always be possible, and in that case, the specified path is ignored.
     * <p>
     * Examples of paths and ignores:
     * <ul>
     * <li><code>foo.bar</code> - ignores the configuration name <code>foo.bar</code></li>
     * <li><code>foo.*</code> - ignores the single configurations names under <code>foo</code></li>
     * <li><code>foo.**</code> - ignores all the configurations names under <code>foo</code></li>
     * </ul>
     *
     * @param path the configuration path to ignore
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withValidateUnknown(boolean)
     */
    public SmallRyeConfigBuilder withMappingIgnore(String path) {
        mappingsBuilder.ignoredPath(path);
        return this;
    }

    /**
     * Enable or disable the Config Mapping requirement to match every configuration path available in the Config
     * system. By default, the validation is <b>enabled</b>.
     *
     * @param validateUnknown a boolean <code>true</code> to enable the validation, or <code>false</code> to disable it.
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withValidateUnknown(boolean)
     */
    public SmallRyeConfigBuilder withValidateUnknown(boolean validateUnknown) {
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

    public List<String> getProfiles() {
        return profiles;
    }

    public ConfigValidator getValidator() {
        if (isAddDiscoveredValidator()) {
            this.validator = discoverValidator();
        }
        return validator;
    }

    public Map<String, String> getDefaultValues() {
        return defaultValues;
    }

    public MappingBuilder getMappingsBuilder() {
        return mappingsBuilder;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public boolean isAddDiscoveredCustomizers() {
        return addDiscoveredCustomizers;
    }

    public boolean isAddDefaultSources() {
        return addDefaultSources;
    }

    public boolean isAddSystemSources() {
        return addSystemSources;
    }

    public boolean isAddPropertiesSources() {
        return addPropertiesSources;
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

    public SmallRyeConfigBuilder setAddSystemSources(final boolean addSystemSources) {
        this.addSystemSources = addSystemSources;
        return this;
    }

    public SmallRyeConfigBuilder setAddPropertiesSources(final boolean addPropertiesSources) {
        this.addPropertiesSources = addPropertiesSources;
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

        customizers.sort(new Comparator<>() {
            @Override
            public int compare(SmallRyeConfigBuilderCustomizer o1, SmallRyeConfigBuilderCustomizer o2) {
                return o1.priority() - o2.priority();
            }
        });
        for (SmallRyeConfigBuilderCustomizer customizer : customizers) {
            customizer.configBuilder(this);
        }

        return new SmallRyeConfig(this);
    }

    public final class MappingBuilder {
        private final Map<Class<?>, Set<String>> mappings = new HashMap<>();
        private final Set<String> ignoredPaths = new HashSet<>();

        private final StringBuilder sb = new StringBuilder();

        public void mapping(ConfigClass configClass) {
            validateAnnotations(configClass.getKlass());
            mapping(configClass.getKlass(), configClass.getPrefix());
        }

        public void mapping(Class<?> type, String prefix) {
            Assert.checkNotNullParam("type", type);
            Assert.checkNotNullParam("path", prefix);

            Class<?> mappingClass = getConfigMappingClass(type);
            mappings.computeIfAbsent(mappingClass, k -> new HashSet<>(4)).add(prefix);

            // Load the mapping defaults, to make the defaults available to all config sources
            Map<String, String> properties = ConfigMappingLoader.configMappingProperties(mappingClass);
            sb.setLength(0);
            sb.append(prefix);
            for (Map.Entry<String, String> property : properties.entrySet()) {
                if (property.getValue() == null) {
                    continue;
                }

                // Do not override builder defaults with mapping defaults
                String path = property.getKey();
                String name;
                if (prefix.isEmpty()) {
                    name = path;
                } else if (path.isEmpty()) {
                    name = prefix;
                } else if (path.charAt(0) == '[') {
                    name = sb.append(path).toString();
                } else {
                    name = sb.append(".").append(path).toString();
                }
                sb.setLength(prefix.length());
                defaultValues.putIfAbsent(name, property.getValue());
            }

            // It is an MP ConfigProperties, so ignore unmapped properties
            if (ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(type) != null) {
                ignoredPaths.add(prefix.isEmpty() ? "*" : prefix + ".**");
            }
        }

        public void ignoredPath(String ignoredPath) {
            Assert.checkNotNullParam("ignoredPath", ignoredPath);
            ignoredPaths.add(ignoredPath);
        }

        public Map<Class<?>, Set<String>> getMappings() {
            return mappings;
        }

        public Set<String> getIgnoredPaths() {
            return ignoredPaths;
        }

        private void validateAnnotations(Class<?> type) {
            if (!type.isInterface() && type.isAnnotationPresent(ConfigMapping.class)) {
                throw ConfigMessages.msg.mappingAnnotationNotSupportedInClass(type);
            }

            if (type.isInterface() && type.isAnnotationPresent(ConfigProperties.class)) {
                throw ConfigMessages.msg.propertiesAnnotationNotSupportedInInterface(type);
            }
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

    static class SecretKeysHandlerWithName {
        private final SecretKeysHandlerFactory factory;

        SecretKeysHandlerWithName(SecretKeysHandler secretKeysHandler) {
            this(new SecretKeysHandlerFactory() {
                @Override
                public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
                    return secretKeysHandler;
                }

                @Override
                public String getName() {
                    return secretKeysHandler.getName();
                }
            });
        }

        SecretKeysHandlerWithName(SecretKeysHandlerFactory factory) {
            this.factory = factory;
        }

        io.smallrye.config.SecretKeysHandler getSecretKeysHandler(ConfigSourceContext context) {
            return factory.getSecretKeysHandler(context);
        }

        String getName() {
            return factory.getName();
        }
    }
}
