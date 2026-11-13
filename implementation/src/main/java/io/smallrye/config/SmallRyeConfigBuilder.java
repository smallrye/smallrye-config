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
import static io.smallrye.config.Converters.addConverter;
import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newTrimmingConverter;
import static io.smallrye.config.ProfileConfigSourceInterceptor.convertProfile;
import static io.smallrye.config.PropertiesConfigSourceLoader.inClassPath;
import static io.smallrye.config.PropertiesConfigSourceLoader.inFileSystem;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOG_VALUES;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_SECRET_HANDLERS;
import static java.lang.Thread.currentThread;

import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.Converters.ConverterWithPriority;
import io.smallrye.config.DefaultValuesConfigSource.Defaults;
import io.smallrye.config.SecretKeys.Secrets;
import io.smallrye.config._private.ConfigMessages;

/**
 * Builder for {@link SmallRyeConfig} instances.
 * <p>
 * Provides a fluent API to configure and build a {@link SmallRyeConfig} with custom
 * {@linkplain ConfigSource configuration sources}, {@linkplain Converter converters},
 * {@linkplain ConfigSourceInterceptor interceptors}, {@linkplain ConfigMapping config mappings},
 * profiles, default values, secret keys, validators, and customizers.
 * <p>
 * Most of the builder methods only record the intent to add a component. The {@link SmallRyeConfig} instance and its
 * components are only created when {@link #build()} is called.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigBuilder implements ConfigBuilder {
    private final List<SmallRyeConfigBuilderCustomizer> customizers = new ArrayList<>();
    // sources are not sorted by their ordinals
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<ConfigSourceProvider> sourceProviders = new ArrayList<>();
    private final Map<Type, ConverterWithPriority> converters = new HashMap<>();
    private final List<InterceptorWithPriority> interceptors = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private final Defaults defaults = new Defaults();
    private final Secrets secrets = new Secrets();
    private final List<SecretKeysHandlerWithName> secretKeysHandlers = new ArrayList<>();
    // null when no validator was set, so that a discovered one does not override a validator set in the builder
    private ConfigValidator validator = null;
    private final MappingBuilder mappingsBuilder = new MappingBuilder();
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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
    private boolean cachePropertyNames = true;

    /**
     * Constructs a new {@code SmallRyeConfigBuilder}.
     */
    public SmallRyeConfigBuilder() {
    }

    /**
     * Enables the discovery of {@link SmallRyeConfigBuilderCustomizer} instances with the
     * {@link java.util.ServiceLoader} mechanism. The customizers are loaded from the configured class loader and
     * executed when {@link #build()} is called.
     *
     * @return this {@link SmallRyeConfigBuilder}
     * @see SmallRyeConfigBuilderCustomizer
     */
    public SmallRyeConfigBuilder addDiscoveredCustomizers() {
        addDiscoveredCustomizers = true;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation enables the discovery of {@link ConfigSource}, {@link ConfigSourceProvider} and
     * {@link ConfigSourceFactory} instances with the {@link java.util.ServiceLoader} mechanism. The configuration
     * sources are loaded from the configured class loader when {@link #build()} is called.
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
    @Override
    public SmallRyeConfigBuilder addDiscoveredSources() {
        addDiscoveredSources = true;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation enables the discovery of {@link Converter} instances with the
     * {@link java.util.ServiceLoader} mechanism. The converters are loaded from the configured class loader when
     * {@link #build()} is called.
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
    @Override
    public SmallRyeConfigBuilder addDiscoveredConverters() {
        addDiscoveredConverters = true;
        return this;
    }

    /**
     * Enables the discovery of {@link ConfigSourceInterceptor} and {@link ConfigSourceInterceptorFactory} instances
     * with the {@link java.util.ServiceLoader} mechanism. The interceptors are loaded from the configured class
     * loader when {@link #build()} is called.
     *
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigSourceInterceptor
     */
    public SmallRyeConfigBuilder addDiscoveredInterceptors() {
        addDiscoveredInterceptors = true;
        return this;
    }

    /**
     * Enables the discovery of {@link SecretKeysHandler} and {@link SecretKeysHandlerFactory} instances with the
     * {@link java.util.ServiceLoader} mechanism. The handlers are loaded from the configured class loader when
     * {@link #build()} is called, and may be restricted to a subset of handler names with the
     * {@code smallrye.config.secret-handlers} configuration property. A handler set with
     * {@link #withSecretKeysHandlers(SecretKeysHandler...)} has priority over a discovered one that uses the same
     * name.
     *
     * @return this {@link SmallRyeConfigBuilder}
     * @see SecretKeysHandler
     * @see #withSecretKeysHandlers(SecretKeysHandler...)
     */
    public SmallRyeConfigBuilder addDiscoveredSecretKeysHandlers() {
        addDiscoveredSecretKeysHandlers = true;
        return this;
    }

    /**
     * Enables the discovery of a {@link ConfigValidator} instance with the {@link java.util.ServiceLoader} mechanism.
     * The validator is loaded from the configured class loader when {@link #build()} is called. A validator set with
     * {@link #withValidator(ConfigValidator)} has priority, in which case no discovery takes place.
     *
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigValidator
     * @see #withValidator(ConfigValidator)
     */
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

    /**
     * {@inheritDoc}
     *
     * This implementation adds both the system sources and the properties sources when {@link #build()} is called
     * (by descending ordinal):
     * <ul>
     * <li>({@code 400}) {@link SysPropConfigSource} for System properties</li>
     * <li>({@code 300}) {@link EnvConfigSource} for Environment variables</li>
     * <li>({@code 295}) {@link DotEnvConfigSourceProvider} for the {@code .env} file in the current working
     * directory</li>
     * <li>({@code 260}) {@code application.properties} in the {@code config} folder, located in the current working
     * directory</li>
     * <li>({@code 250}) {@code application.properties} in the classpath</li>
     * <li>({@code 100}) {@code META-INF/microprofile-config.properties} in the classpath</li>
     * </ul>
     * This supersedes {@link #addSystemSources()} and {@link #addPropertiesSources()}.
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
    @Override
    public SmallRyeConfigBuilder addDefaultSources() {
        addDefaultSources = true;
        return this;
    }

    /**
     * Adds the default system configuration sources when the configuration is built (by descending ordinal):
     * <ul>
     * <li>({@code 400}) {@link SysPropConfigSource} for System properties</li>
     * <li>({@code 300}) {@link EnvConfigSource} for Environment variables</li>
     * <li>({@code 295}) {@link DotEnvConfigSourceProvider} for the {@code .env} file in the current working
     * directory</li>
     * </ul>
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
    public SmallRyeConfigBuilder addSystemSources() {
        addSystemSources = true;
        return this;
    }

    /**
     * Adds the default properties configuration sources when the configuration is built (by descending ordinal):
     * <ul>
     * <li>({@code 260}) {@code application.properties} in the {@code config} folder, located in the current working
     * directory</li>
     * <li>({@code 250}) {@code application.properties} in the classpath</li>
     * <li>({@code 100}) {@code META-INF/microprofile-config.properties} in the classpath</li>
     * </ul>
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
    public SmallRyeConfigBuilder addPropertiesSources() {
        addPropertiesSources = true;
        return this;
    }

    List<ConfigSource> getDefaultSources() {
        List<ConfigSource> defaultSources = new ArrayList<>();
        defaultSources.addAll(getSystemSources());
        defaultSources.addAll(getPropertiesSources());
        return defaultSources;
    }

    List<ConfigSource> getSystemSources() {
        List<ConfigSource> sources = new ArrayList<>();
        sources.add(new SysPropConfigSource());
        sources.add(new EnvConfigSource());
        sources.addAll(new DotEnvConfigSourceProvider().getConfigSources(classLoader));
        return sources;
    }

    List<ConfigSource> getPropertiesSources() {
        List<ConfigSource> sources = new ArrayList<>();
        sources.addAll(
                inFileSystem(Paths.get(System.getProperty("user.dir"), "config", "application.properties").toUri().toString(),
                        260, classLoader));
        sources.addAll(inClassPath("application.properties", 250, classLoader));
        sources.addAll(inClassPath("META-INF/microprofile-config.properties", 100, classLoader));
        return sources;
    }

    /**
     * Adds the default configuration source interceptors when the configuration is built.
     * <p>
     * The default interceptors include:
     * <ul>
     * <li>{@link ProfileConfigSourceInterceptor} to handle configuration profiles</li>
     * <li>{@link RelocateConfigSourceInterceptor} to handle profile relocations</li>
     * <li>{@link ExpressionConfigSourceInterceptor} to expand configuration expressions and secret keys</li>
     * <li>{@link SecretKeysConfigSourceInterceptor} to protect secret keys</li>
     * <li>{@link LoggingConfigSourceInterceptor} to log configuration values lookup</li>
     * </ul>
     *
     * @return this {@link SmallRyeConfigBuilder}
     */
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
                if (profileValue != null && profileValue.getValue() != null) {
                    List<String> convertProfiles = convertProfile(profileValue.getValue());
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

            static class MultipleProfileProperty implements Comparable<MultipleProfileProperty> {
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

                // the handlers are mapped by name, so the last one with a given name wins. The discovered handlers
                // come first, so that a handler set in the builder has priority over a discovered one
                List<SecretKeysHandler> secretKeysHandlers = new ArrayList<>();
                if (isAddDiscoveredSecretKeysHandlers()) {
                    secretKeysHandlers.addAll(discoverSecretKeysHandlers(context));
                }

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
                return new SecretKeysConfigSourceInterceptor(SmallRyeConfigBuilder.this.secrets);
            }

            @Override
            public OptionalInt getPriority() {
                // before the profile interceptor, so a name is matched once, and not once for each profile
                return OptionalInt.of(Priorities.LIBRARY + 210);
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

    /**
     * {@inheritDoc}
     *
     * This implementation uses the class loader to:
     * <ul>
     * <li>discover the {@link ConfigSource}, {@link ConfigSourceProvider}, {@link ConfigSourceFactory},
     * {@link Converter}, {@link ConfigSourceInterceptor}, {@link ConfigSourceInterceptorFactory},
     * {@link SecretKeysHandler}, {@link SecretKeysHandlerFactory}, {@link ConfigValidator} and
     * {@link SmallRyeConfigBuilderCustomizer} instances with the {@link java.util.ServiceLoader} mechanism</li>
     * <li>look up the classpath {@linkplain ConfigSource configuration sources} added with
     * {@link #addDefaultSources()} and {@link #addPropertiesSources()}</li>
     * <li>find the {@link ConfigMappingHandler} of the registered config classes</li>
     * </ul>
     * If not set, the context class loader of the thread that created this builder is used.
     * <p>
     * A config class is resolved with the class loader set at the time it is registered, so this method must be
     * called before {@link #withMapping(Class)} and the remaining mapping methods.
     *
     * @param classLoader the class loader
     * @return this {@link SmallRyeConfigBuilder}
     * @see #getClassLoader()
     */
    @Override
    public SmallRyeConfigBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Registers {@link SmallRyeConfigBuilderCustomizer} instances to customize this builder when building the
     * configuration.
     *
     * @param customizers the customizers to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see SmallRyeConfigBuilderCustomizer
     */
    public SmallRyeConfigBuilder withCustomizers(SmallRyeConfigBuilderCustomizer... customizers) {
        Collections.addAll(this.customizers, customizers);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation registers the {@linkplain ConfigSource configuration sources} as they are. They are only
     * sorted by their ordinal when the configuration is built.
     *
     * @param configSources the configuration sources to add
     * @return this {@link SmallRyeConfigBuilder}
     */
    @Override
    public SmallRyeConfigBuilder withSources(ConfigSource... configSources) {
        Collections.addAll(sources, configSources);
        return this;
    }

    /**
     * Adds a collection of {@link ConfigSource} instances to the builder.
     *
     * @param configSources the collection of configuration sources to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withSources(ConfigSource...)
     */
    public SmallRyeConfigBuilder withSources(Collection<ConfigSource> configSources) {
        sources.addAll(configSources);
        return this;
    }

    /**
     * Adds a {@link ConfigSourceProvider} instance to the builder. The provider is called with the configured class
     * loader to retrieve its {@linkplain ConfigSource configuration sources} when the configuration is built.
     *
     * @param provider the configuration source provider to add
     * @return this {@link SmallRyeConfigBuilder}
     */
    public SmallRyeConfigBuilder withSources(ConfigSourceProvider provider) {
        sourceProviders.add(provider);
        return this;
    }

    /**
     * Adds {@link ConfigSourceFactory} instances to the builder. Each factory is initialized in priority order when
     * the configuration is built, with access to the {@linkplain ConfigSource configuration sources} initialized
     * before it.
     *
     * @param configSourceFactories the configuration source factories to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigSourceFactory
     */
    public SmallRyeConfigBuilder withSources(ConfigSourceFactory... configSourceFactories) {
        for (ConfigSourceFactory configSourceFactory : configSourceFactories) {
            sources.add(new ConfigurableConfigSource(configSourceFactory));
        }
        return this;
    }

    /**
     * Adds {@link ConfigSourceInterceptor} instances to the builder. An interceptor priority is assigned with the
     * {@code jakarta.annotation.Priority} annotation, or {@link ConfigSourceInterceptorFactory#DEFAULT_PRIORITY} if
     * the annotation is not present, and the interceptor with the highest priority is executed first in the chain.
     *
     * @param interceptors the interceptors to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigSourceInterceptor
     */
    public SmallRyeConfigBuilder withInterceptors(ConfigSourceInterceptor... interceptors) {
        for (ConfigSourceInterceptor interceptor : interceptors) {
            this.interceptors.add(new InterceptorWithPriority(interceptor));
        }
        return this;
    }

    /**
     * Adds {@link ConfigSourceInterceptorFactory} instances to the builder. Each factory is initialized when the
     * configuration is built, with access to the interceptors already initialized, and its
     * {@link ConfigSourceInterceptorFactory#getPriority()} determines the position of the interceptor in the chain.
     *
     * @param interceptorFactories the interceptor factories to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigSourceInterceptorFactory
     */
    public SmallRyeConfigBuilder withInterceptorFactories(ConfigSourceInterceptorFactory... interceptorFactories) {
        for (ConfigSourceInterceptorFactory interceptorFactory : interceptorFactories) {
            this.interceptors.add(new InterceptorWithPriority(interceptorFactory));
        }
        return this;
    }

    /**
     * Adds {@link SecretKeysHandler} instances to the builder. A handler decodes or decrypts the secret configuration
     * values expressed as {@code ${handler::value}}, where the {@code handler} is the name of the handler. A handler
     * added here has priority over a handler discovered with {@link #addDiscoveredSecretKeysHandlers()} that uses the
     * same name.
     *
     * @param secretKeysHandlers the secret keys handlers to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see SecretKeysHandler
     */
    public SmallRyeConfigBuilder withSecretKeysHandlers(SecretKeysHandler... secretKeysHandlers) {
        for (SecretKeysHandler secretKeysHandler : secretKeysHandlers) {
            this.secretKeysHandlers.add(new SecretKeysHandlerWithName(secretKeysHandler));
        }
        return this;
    }

    /**
     * Adds {@link SecretKeysHandlerFactory} instances to the builder. Each factory is initialized when the
     * configuration is built, with access to the {@linkplain ConfigSource configuration sources}, which allows the
     * handler to be configured with configuration values. A handler added here has priority over a handler
     * discovered with {@link #addDiscoveredSecretKeysHandlers()} that uses the same name.
     *
     * @param secretKeyHandlerFactories the secret keys handler factories to add
     * @return this {@link SmallRyeConfigBuilder}
     * @see SecretKeysHandlerFactory
     */
    public SmallRyeConfigBuilder withSecretKeyHandlerFactories(SecretKeysHandlerFactory... secretKeyHandlerFactories) {
        for (SecretKeysHandlerFactory secretKeyHandlerFactory : secretKeyHandlerFactories) {
            this.secretKeysHandlers.add(new SecretKeysHandlerWithName(secretKeyHandlerFactory));
        }
        return this;
    }

    /**
     * Sets the active profile name. Multiple profiles can be specified as a comma-separated string, in which case
     * the last listed profile has priority.
     * <p>
     * A profile set with this method has priority over the profiles set with the {@code smallrye.config.profile}
     * configuration property, which are not looked up at all in that case. Calling this method automatically enables
     * the default interceptors.
     *
     * @param profile the profile name or comma-separated list of profile names
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withProfiles(List)
     * @see #addDefaultInterceptors()
     */
    public SmallRyeConfigBuilder withProfile(String profile) {
        addDefaultInterceptors();
        this.profiles.addAll(convertProfile(profile));
        return this;
    }

    /**
     * Sets the active profile names, where the last profile of the list has priority.
     * <p>
     * Profiles set with this method have priority over the profiles set with the {@code smallrye.config.profile}
     * configuration property, which are not looked up at all in that case. Calling this method automatically enables
     * the default interceptors.
     *
     * @param profiles the list of profiles to activate
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withProfile(String)
     * @see #addDefaultInterceptors()
     */
    public SmallRyeConfigBuilder withProfiles(List<String> profiles) {
        addDefaultInterceptors();
        this.profiles.addAll(profiles);
        return this;
    }

    /**
     * Registers configuration property names as secret keys.
     * <p>
     * Secret keys are omitted from {@link SmallRyeConfig#getPropertyNames()}, excluded from mapping {@code toString()}
     * representations, and require an unlocked context to be retrieved programmatically.
     *
     * @param keys the configuration property names to treat as secrets
     * @return this {@link SmallRyeConfigBuilder}
     * @see SecretKeys
     */
    public SmallRyeConfigBuilder withSecretKeys(String... keys) {
        for (String key : keys) {
            secrets.add(key);
        }
        return this;
    }

    /**
     * Registers a default value for a configuration property name. The default values are provided by the
     * {@link DefaultValuesConfigSource}, with the lowest possible ordinal, so a value from any other
     * {@linkplain ConfigSource configuration source} overrides the default.
     *
     * @param name the configuration property name
     * @param value the default value
     * @return this {@link SmallRyeConfigBuilder}
     * @see DefaultValuesConfigSource
     */
    public SmallRyeConfigBuilder withDefaultValue(String name, String value) {
        this.defaults.put(name, value);
        return this;
    }

    /**
     * Registers a map of default values for configuration property names.
     *
     * @param defaultValues a map of configuration property names and their default values
     * @return this {@link SmallRyeConfigBuilder}
     * @see #withDefaultValue(String, String)
     */
    public SmallRyeConfigBuilder withDefaultValues(Map<String, String> defaultValues) {
        this.defaults.put(defaultValues);
        return this;
    }

    /**
     * Registers a {@link ConfigMapping} class or interface, with the prefix set in {@link ConfigMapping#prefix()}.
     * The mapping is populated when the configuration is built and the instance is retrieved with
     * {@link SmallRyeConfig#getConfigMapping(Class)}.
     *
     * @param type the config mapping class or interface
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigMapping
     */
    public SmallRyeConfigBuilder withMapping(Class<?> type) {
        return withMapping(ConfigClass.configClass(type));
    }

    /**
     * Registers a {@link ConfigMapping} class or interface with a configuration property prefix. The mapping is
     * populated when the configuration is built and the instance is retrieved with
     * {@link SmallRyeConfig#getConfigMapping(Class, String)}.
     *
     * @param type the config mapping class or interface
     * @param prefix the prefix to override {@link ConfigMapping#prefix()}
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigMapping
     */
    public SmallRyeConfigBuilder withMapping(Class<?> type, String prefix) {
        return withMapping(ConfigClass.configClass(type, prefix));
    }

    /**
     * Registers a {@link ConfigClass} mapping, which pairs the config type with the configuration prefix it maps to
     * and the handler responsible for its processing.
     *
     * @param configClass the config class mapping
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigClass
     */
    public SmallRyeConfigBuilder withMapping(ConfigClass configClass) {
        mappingsBuilder.mapping(configClass);
        return this;
    }

    /**
     * Registers a config class mapping that populates the {@code template} instance, instead of creating a new
     * instance of the config class.
     *
     * @param type the config class
     * @param prefix the prefix of the mapping
     * @param template the instance of the config class to populate
     * @return this {@link SmallRyeConfigBuilder}
     * @param <T> the type of the config class
     * @see MappingBuilder#mappingTemplate(ConfigClass, Object)
     */
    public <T> SmallRyeConfigBuilder withMapping(Class<T> type, String prefix, T template) {
        return withMapping(ConfigClass.configClass(type, prefix), template);
    }

    /**
     * Registers a config class mapping that populates the {@code template} instance, instead of creating a new
     * instance of the config class.
     *
     * @param configClass the config class mapping
     * @param template the instance of the config class to populate
     * @return this {@link SmallRyeConfigBuilder}
     * @see MappingBuilder#mappingTemplate(ConfigClass, Object)
     */
    public SmallRyeConfigBuilder withMapping(ConfigClass configClass, Object template) {
        mappingsBuilder.mappingTemplate(configClass, template);
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

    /**
     * Sets the {@link ConfigValidator} used to validate the config mapping instances when the configuration is built.
     * The validator replaces any previously set validator and has priority over a validator discovered with
     * {@link #addDiscoveredValidator()}.
     *
     * @param validator the validator to use
     * @return this {@link SmallRyeConfigBuilder}
     * @see ConfigValidator
     */
    public SmallRyeConfigBuilder withValidator(ConfigValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation resolves the target type of each {@link Converter} from its generic type, and assigns the
     * priority with the {@code jakarta.annotation.Priority} annotation, or {@code 100} if the annotation is not
     * present. A converter is only registered if no other converter with a higher priority was registered for the
     * same target type.
     *
     * @param converters the converters to add
     * @return this {@link SmallRyeConfigBuilder}
     * @throws IllegalStateException if the target type of a converter cannot be resolved
     */
    @Override
    public SmallRyeConfigBuilder withConverters(Converter<?>[] converters) {
        for (Converter<?> converter : converters) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                throw ConfigMessages.msg.unableToAddConverter(converter);
            }
            addConverter(type, converter, this.converters);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation only registers the {@link Converter} if no other converter with a higher priority was
     * registered for the same target type.
     *
     * @param type the target type of the converter
     * @param priority the priority of the converter
     * @param converter the converter to add
     * @return this {@link SmallRyeConfigBuilder}
     * @param <T> the target type of the converter
     */
    @Override
    public <T> SmallRyeConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        addConverter(type, priority, converter, converters);
        return this;
    }

    /**
     * Returns the list of configuration sources added to this builder. The list is mutable and backs the builder, so
     * changes to it are reflected in the built configuration.
     *
     * @return the list of configuration sources
     */
    public List<ConfigSource> getSources() {
        return sources;
    }

    /**
     * Returns the list of configuration source providers added to this builder. The list is mutable and backs the
     * builder, so changes to it are reflected in the built configuration.
     *
     * @return the list of configuration source providers
     */
    public List<ConfigSourceProvider> getSourceProviders() {
        return sourceProviders;
    }

    /**
     * Returns the converters registered with this builder. The map is mutable and backs the builder, so changes to
     * it are reflected in the built configuration.
     *
     * @return the map of converters keyed by target type
     */
    public Map<Type, ConverterWithPriority> getConverters() {
        return converters;
    }

    /**
     * Returns the interceptors registered with this builder, not sorted by their priority. The list is mutable and
     * backs the builder, so changes to it are reflected in the built configuration.
     *
     * @return the list of interceptors with priorities
     */
    List<InterceptorWithPriority> getInterceptors() {
        return interceptors;
    }

    /**
     * Returns the active profiles set in this builder. The list is mutable and backs the builder, so changes to it
     * are reflected in the built configuration.
     *
     * @return the list of active profiles
     */
    public List<String> getProfiles() {
        return profiles;
    }

    /**
     * Returns the {@link ConfigValidator} to validate the config mapping instances. A validator set with
     * {@link #withValidator(ConfigValidator)} has priority over a discovered one, so the discovery only runs if no
     * validator was set and {@link #addDiscoveredValidator()} is enabled. The discovered validator is set in this
     * builder, so the discovery runs only once.
     *
     * @return the config validator, or {@link ConfigValidator#EMPTY} if none is set or discovered
     * @see #withValidator(ConfigValidator)
     */
    public ConfigValidator getValidator() {
        if (validator == null && isAddDiscoveredValidator()) {
            this.validator = discoverValidator();
        }
        return validator == null ? ConfigValidator.EMPTY : validator;
    }

    /**
     * Returns the default values registered with this builder, which are provided to the configuration by the
     * {@link DefaultValuesConfigSource}.
     *
     * @return the default values
     * @see #withDefaultValue(String, String)
     */
    public Defaults getDefaults() {
        return defaults;
    }

    /**
     * Returns the secret keys registered with this builder, including the ones collected from the
     * {@linkplain ConfigMapping config mappings} members wrapped with {@link Secret}.
     *
     * @return the secret keys
     * @see #withSecretKeys(String...)
     */
    public Secrets getSecretKeys() {
        return secrets;
    }

    /**
     * Returns the {@link MappingBuilder} of this builder, which holds the registered config mappings, instances and
     * templates.
     *
     * @return the mapping builder
     */
    public MappingBuilder getMappingsBuilder() {
        return mappingsBuilder;
    }

    /**
     * Returns the {@link ClassLoader} set in this builder, or the context class loader of the thread that created
     * this builder if none was set.
     *
     * @return the class loader
     * @see #forClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Checks if the {@linkplain SmallRyeConfigBuilderCustomizer customizers} discovery is enabled.
     *
     * @return {@code true} if discovered customizers will be added, or {@code false} otherwise
     * @see #addDiscoveredCustomizers()
     */
    public boolean isAddDiscoveredCustomizers() {
        return addDiscoveredCustomizers;
    }

    /**
     * Checks if the default configuration sources will be added to the configuration.
     *
     * @return {@code true} if default sources will be added, or {@code false} otherwise
     * @see #addDefaultSources()
     */
    public boolean isAddDefaultSources() {
        return addDefaultSources;
    }

    /**
     * Checks if the default system configuration sources will be added to the configuration.
     *
     * @return {@code true} if system sources will be added, or {@code false} otherwise
     * @see #addSystemSources()
     */
    public boolean isAddSystemSources() {
        return addSystemSources;
    }

    /**
     * Checks if the default properties configuration sources will be added to the configuration.
     *
     * @return {@code true} if properties sources will be added, or {@code false} otherwise
     * @see #addPropertiesSources()
     */
    public boolean isAddPropertiesSources() {
        return addPropertiesSources;
    }

    /**
     * Checks if the default interceptors will be added to the configuration.
     *
     * @return {@code true} if default interceptors will be added, or {@code false} otherwise
     * @see #addDefaultInterceptors()
     */
    public boolean isAddDefaultInterceptors() {
        return addDefaultInterceptors;
    }

    /**
     * Checks if the {@linkplain ConfigSource configuration sources} discovery is enabled.
     *
     * @return {@code true} if discovered sources will be added, or {@code false} otherwise
     * @see #addDiscoveredSources()
     */
    public boolean isAddDiscoveredSources() {
        return addDiscoveredSources;
    }

    /**
     * Checks if the {@linkplain Converter converters} discovery is enabled.
     *
     * @return {@code true} if discovered converters will be added, or {@code false} otherwise
     * @see #addDiscoveredConverters()
     */
    public boolean isAddDiscoveredConverters() {
        return addDiscoveredConverters;
    }

    /**
     * Checks if the {@linkplain ConfigSourceInterceptor interceptors} discovery is enabled.
     *
     * @return {@code true} if discovered interceptors will be added, or {@code false} otherwise
     * @see #addDiscoveredInterceptors()
     */
    public boolean isAddDiscoveredInterceptors() {
        return addDiscoveredInterceptors;
    }

    /**
     * Checks if the {@linkplain SecretKeysHandler secret keys handlers} discovery is enabled.
     *
     * @return {@code true} if discovered secret keys handlers will be added, or {@code false} otherwise
     * @see #addDiscoveredSecretKeysHandlers()
     */
    public boolean isAddDiscoveredSecretKeysHandlers() {
        return addDiscoveredSecretKeysHandlers;
    }

    /**
     * Checks if the {@linkplain ConfigValidator validator} discovery is enabled.
     *
     * @return {@code true} if a discovered validator will be added, or {@code false} otherwise
     * @see #addDiscoveredValidator()
     */
    public boolean isAddDiscoveredValidator() {
        return addDiscoveredValidator;
    }

    /**
     * Checks if the property names are cached in the created {@link SmallRyeConfig}. An updated list of property
     * names may always be retrieved with {@link SmallRyeConfig#getLatestPropertyNames()}.
     *
     * @return {@code true} if property names caching is enabled, or {@code false} otherwise
     * @see SmallRyeConfig#getPropertyNames()
     */
    public boolean isCachePropertyNames() {
        return cachePropertyNames;
    }

    /**
     * Sets whether to add default configuration sources.
     *
     * @param addDefaultSources {@code true} to add default sources, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDefaultSources()
     */
    public SmallRyeConfigBuilder setAddDefaultSources(final boolean addDefaultSources) {
        this.addDefaultSources = addDefaultSources;
        return this;
    }

    /**
     * Sets whether to add default system configuration sources.
     *
     * @param addSystemSources {@code true} to add system sources, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addSystemSources()
     */
    public SmallRyeConfigBuilder setAddSystemSources(final boolean addSystemSources) {
        this.addSystemSources = addSystemSources;
        return this;
    }

    /**
     * Sets whether to add default properties configuration sources.
     *
     * @param addPropertiesSources {@code true} to add properties sources, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addPropertiesSources()
     */
    public SmallRyeConfigBuilder setAddPropertiesSources(final boolean addPropertiesSources) {
        this.addPropertiesSources = addPropertiesSources;
        return this;
    }

    /**
     * Sets whether to add default interceptors.
     *
     * @param addDefaultInterceptors {@code true} to add default interceptors, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDefaultInterceptors()
     */
    public SmallRyeConfigBuilder setAddDefaultInterceptors(final boolean addDefaultInterceptors) {
        this.addDefaultInterceptors = addDefaultInterceptors;
        return this;
    }

    /**
     * Sets whether to add discovered configuration sources.
     *
     * @param addDiscoveredSources {@code true} to add discovered sources, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDiscoveredSources()
     */
    public SmallRyeConfigBuilder setAddDiscoveredSources(final boolean addDiscoveredSources) {
        this.addDiscoveredSources = addDiscoveredSources;
        return this;
    }

    /**
     * Sets whether to add discovered converters.
     *
     * @param addDiscoveredConverters {@code true} to add discovered converters, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDiscoveredConverters()
     */
    public SmallRyeConfigBuilder setAddDiscoveredConverters(final boolean addDiscoveredConverters) {
        this.addDiscoveredConverters = addDiscoveredConverters;
        return this;
    }

    /**
     * Sets whether to add discovered interceptors.
     *
     * @param addDiscoveredInterceptors {@code true} to add discovered interceptors, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDiscoveredInterceptors()
     */
    public SmallRyeConfigBuilder setAddDiscoveredInterceptors(final boolean addDiscoveredInterceptors) {
        this.addDiscoveredInterceptors = addDiscoveredInterceptors;
        return this;
    }

    /**
     * Sets whether to add discovered secret keys handlers.
     *
     * @param addDiscoveredSecretKeysHandlers {@code true} to add discovered secret keys handlers, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDiscoveredSecretKeysHandlers()
     */
    public SmallRyeConfigBuilder setAddDiscoveredSecretKeysHandlers(final boolean addDiscoveredSecretKeysHandlers) {
        this.addDiscoveredSecretKeysHandlers = addDiscoveredSecretKeysHandlers;
        return this;
    }

    /**
     * Sets whether to add a discovered validator.
     *
     * @param addDiscoveredValidator {@code true} to add a discovered validator, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #addDiscoveredValidator()
     */
    public SmallRyeConfigBuilder setAddDiscoveredValidator(final boolean addDiscoveredValidator) {
        this.addDiscoveredValidator = addDiscoveredValidator;
        return this;
    }

    /**
     * Sets whether to cache property names in the created {@link SmallRyeConfig}.
     *
     * @param cachePropertyNames {@code true} to enable caching of property names, or {@code false} otherwise
     * @return this {@link SmallRyeConfigBuilder}
     * @see #isCachePropertyNames()
     */
    public SmallRyeConfigBuilder setCachePropertyNames(boolean cachePropertyNames) {
        this.cachePropertyNames = cachePropertyNames;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation creates the {@link SmallRyeConfig} instance in the following order:
     * <ol>
     * <li>the {@linkplain SmallRyeConfigBuilderCustomizer customizers}, registered and discovered, are executed in
     * ascending priority order, and may still change this builder</li>
     * <li>the {@linkplain ConfigSource configuration sources}, registered, provided by a
     * {@link ConfigSourceProvider}, discovered and default, are collected, together with the
     * {@link DefaultValuesConfigSource} holding the default values, and sorted by descending ordinal</li>
     * <li>the {@linkplain ConfigSourceInterceptor interceptors}, registered, discovered and default, are initialized
     * in priority order and chained</li>
     * <li>the {@linkplain ConfigSourceFactory configuration source factories} are initialized with the interceptor
     * chain, so that their {@linkplain ConfigSource configuration sources} can be configured with the configuration
     * available so far</li>
     * <li>the {@linkplain Converter converters}, registered and discovered, are merged by target type</li>
     * <li>the {@linkplain ConfigMapping config mappings} are populated and validated with the
     * {@link ConfigValidator}</li>
     * </ol>
     * The {@link SmallRyeConfig} instance is not registered in the {@link SmallRyeConfigProviderResolver}.
     *
     * @return the {@link SmallRyeConfig} instance
     * @throws ConfigValidationException if a {@linkplain ConfigMapping config mapping} cannot be populated from the
     *         configuration. Every mapping problem is reported in a single exception
     * @see SmallRyeConfigBuilderCustomizer
     */
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
                return Integer.compare(o1.priority(), o2.priority());
            }
        });
        for (SmallRyeConfigBuilderCustomizer customizer : customizers) {
            customizer.configBuilder(this);
        }

        mappingsBuilder.addNotSecrets();

        return new SmallRyeConfig(this);
    }

    /**
     * Builder for the config class mappings, instances and templates registered with the
     * {@link SmallRyeConfigBuilder}.
     * <p>
     * Registering a config class also registers the defaults, the secrets and the ignored paths it requires, so a
     * config class must always be registered with this builder and not added directly to the collections it holds.
     */
    public final class MappingBuilder {
        private final Set<ConfigClass> mappings = new HashSet<>();
        private final Map<ConfigClass, Object> mappingsInstances = new IdentityHashMap<>();
        private final Map<ConfigClass, Object> mappingsTemplates = new HashMap<>();
        private final PropertyNamesMatcher<?> ignores = new PropertyNamesMatcher<>();

        /**
         * Registers a {@link ConfigClass} mapping. The config class is resolved again with the configured class
         * loader if it is not the context class loader of the current thread.
         *
         * @param configClass the config class mapping
         */
        public void mapping(ConfigClass configClass) {
            Assert.checkNotNullParam("configClass", configClass);
            ConfigClass resolved = !classLoader.equals(currentThread().getContextClassLoader())
                    ? configClass.forClassLoader(classLoader)
                    : configClass;
            mappings.add(resolved);
            addDefaultsAndIgnores(resolved);
        }

        /**
         * Registers an existing instance for the specified {@link ConfigClass} mapping. The instance is used as is,
         * so it is not populated with the configuration values, and the configuration names of the mapping are
         * ignored when validating the unknown properties.
         *
         * @param configClass the config class mapping
         * @param instance the config class instance
         * @see #mappingTemplate(ConfigClass, Object)
         */
        public void mappingInstance(ConfigClass configClass, Object instance) {
            Assert.checkNotNullParam("configClass", configClass);
            Assert.checkNotNullParam("instance", instance);
            ConfigClass resolved = !classLoader.equals(currentThread().getContextClassLoader())
                    ? configClass.forClassLoader(classLoader)
                    : configClass;

            // TODO - Validate that instance is an implementation of type?
            mappingsInstances.put(resolved, instance);
            if (!ignores.matches(resolved.getPrefix())) {
                ignores.add(resolved.getProperties().keySet());
            }
            addDefaultsAndIgnores(resolved);
        }

        /**
         * Registers a config class mapping that populates a template instance, instead of creating a new instance of
         * the config class. The values of the template are the default values of the mapping, in the same way the
         * field initializers of a config class are the default values of a regular mapping, so:
         * <ul>
         * <li>a field with a configuration value is populated with it, and every other field keeps the value of the
         * template</li>
         * <li>the values of the template have priority over the defaults of the config class (initializers or
         * {@link WithDefault}), and the default values of the builder have priority over the template</li>
         * <li>a final field is never populated, and keeps the value of the template, including a final nested
         * config class instance or map, which is not populated either</li>
         * <li>a nested config class instance, a collection or a map of the template is replaced by a new instance,
         * which keeps the template values without a configuration value</li>
         * <li>a template value must be converted back by the converter of the field, with
         * {@link Object#toString()}, in the same way as a field initializer</li>
         * </ul>
         * The config class does not require a no-args constructor. The template is populated when the
         * {@link SmallRyeConfig} is built, and it is the instance returned by
         * {@link SmallRyeConfig#getConfigMapping(Class, String)}.
         *
         * @param configClass the config class mapping
         * @param template the instance of the config class to populate
         */
        public void mappingTemplate(ConfigClass configClass, Object template) {
            Assert.checkNotNullParam("configClass", configClass);
            Assert.checkNotNullParam("template", template);
            ConfigClass resolved = !classLoader.equals(currentThread().getContextClassLoader())
                    ? configClass.forClassLoader(classLoader)
                    : configClass;
            if (resolved.getType().isInterface()) {
                throw ConfigMessages.msg.templateRequiresClass(resolved.getType());
            }
            if (!resolved.getType().isInstance(template)) {
                throw ConfigMessages.msg.templateNotInstanceOfType(template, resolved.getType());
            }

            mappingsTemplates.put(resolved, template);
            defaults.add(ConfigMappingClass.defaults(resolved.getType(), template, resolved.getPrefix(),
                    resolved.getHandler()));
            mapping(resolved);
        }

        /**
         * Registers a configuration path to ignore when performing config mapping validations. The path may use the
         * wildcards described in {@link SmallRyeConfigBuilder#withMappingIgnore(String)}.
         *
         * @param ignoredPath the path to ignore
         * @see SmallRyeConfigBuilder#withMappingIgnore(String)
         */
        public void ignoredPath(String ignoredPath) {
            Assert.checkNotNullParam("ignoredPath", ignoredPath);
            ignores.add(ignoredPath);
        }

        /**
         * Returns the registered {@link ConfigClass} mappings, including the config classes registered with a
         * template. The set is mutable and backs the builder, but a config class should be registered with
         * {@link #mapping(ConfigClass)}, so its defaults, secrets and ignored paths are registered as well.
         *
         * @return the set of config class mappings
         */
        public Set<ConfigClass> getMappings() {
            return mappings;
        }

        /**
         * Returns the registered {@link ConfigClass} mapping instances. The map is mutable and backs the builder,
         * but an instance should be registered with {@link #mappingInstance(ConfigClass, Object)}, so its defaults,
         * secrets and ignored paths are registered as well.
         *
         * @return the map of config class mapping instances
         */
        public Map<ConfigClass, Object> getMappingsInstances() {
            return mappingsInstances;
        }

        /**
         * Returns the registered {@link ConfigClass} mapping templates. The map is mutable and backs the builder,
         * but a template should be registered with {@link #mappingTemplate(ConfigClass, Object)}, so its defaults,
         * secrets and ignored paths are registered as well.
         *
         * @return the map of config class mapping templates
         */
        public Map<ConfigClass, Object> getMappingsTemplates() {
            return mappingsTemplates;
        }

        PropertyNamesMatcher<?> getIgnores() {
            return ignores;
        }

        boolean isEmpty() {
            return mappings.isEmpty() && mappingsInstances.isEmpty();
        }

        private void addDefaultsAndIgnores(ConfigClass configClass) {
            // Do not override defaults set by the builder directly, which have priority over mapping defaults
            defaults.add(configClass.getProperties());
            secrets.add(configClass.getSecrets());
            if (configClass.getHandler().ignoreUnmappedProperties(configClass.getType())) {
                ignores.add(configClass.getPrefix().isEmpty() ? "*" : configClass.getPrefix() + ".**");
            }
        }

        /**
         * A secret name of a mapping uses a wildcard for each map key, like {@code datasource.*.password}, which also
         * matches the names of a nested group, like {@code datasource.devservices.password}. The names of the mappings
         * that are not secrets are more specific, so they do not match.
         */
        void addNotSecrets() {
            List<ConfigClass> allMappings = new ArrayList<>(mappings.size() + mappingsInstances.size());
            allMappings.addAll(mappings);
            allMappings.addAll(mappingsInstances.keySet());

            PropertyNamesMatcher<?> wildcardSecrets = new PropertyNamesMatcher<>();
            for (ConfigClass mapping : allMappings) {
                for (String secret : mapping.getSecrets()) {
                    int wildcard = secret.indexOf('*');
                    if (wildcard != -1) {
                        wildcardSecrets.add(secret);
                    }
                }
            }

            if (wildcardSecrets.isEmpty()) {
                return;
            }

            for (ConfigClass mapping : allMappings) {
                for (String name : mapping.getProperties().keySet()) {
                    if (name.indexOf('*') == -1 && wildcardSecrets.matches(name)) {
                        secrets.addNotSecret(name);
                    }
                }
            }
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
        private static int getPriority(final Class<? extends ConfigSourceInterceptor> type) {
            Priority priorityAnnotation = type.getAnnotation(Priority.class);
            if (priorityAnnotation != null) {
                return priorityAnnotation.value();
            } else {
                Class<?> parentClass = type.getSuperclass();
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
