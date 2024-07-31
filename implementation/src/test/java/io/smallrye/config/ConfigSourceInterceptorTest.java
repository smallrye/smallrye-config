package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder.InterceptorWithPriority;

class ConfigSourceInterceptorTest {
    @Test
    void interceptor() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop", "1234", "override", "4567"))
                .withInterceptors(new LoggingConfigSourceInterceptor(),
                        new OverridingConfigSourceInterceptor())
                .build();

        String value = config.getValue("my.prop", String.class);
        Assertions.assertEquals("4567", value);
    }

    @Test
    void priority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop", "1234"))
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        String value = config.getValue("my.prop", String.class);
        Assertions.assertEquals("higher", value);
    }

    @Test
    void serviceLoader() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop.loader", "1234"))
                .addDiscoveredInterceptors()
                .build();

        String value = config.getValue("my.prop.loader", String.class);
        Assertions.assertEquals("loader", value);
    }

    @Test
    void serviceLoaderAndPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop.loader", "1234"))
                .addDiscoveredInterceptors()
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        String value = config.getValue("my.prop.loader", String.class);
        Assertions.assertEquals("higher", value);
    }

    @Test
    void defaultInterceptors() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop", "1",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .addDefaultInterceptors()
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void notFailExpansionInactive() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("my.prop", "${expansion}",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .addDefaultInterceptors()
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void names() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .build();

        Set<String> names = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(names.contains("my.prop"));
        assertTrue(names.contains("my.prop.profile"));
        assertFalse(names.contains("%prof.my.prop"));
        assertFalse(names.contains("%prof.my.prop.profile"));
    }

    @Test
    void replaceNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1"))
                .withInterceptors(new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }

                    @Override
                    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                        return Stream.of("another.prop").collect(toSet()).iterator();
                    }
                })
                .build();

        assertEquals("1", config.getRawValue("my.prop"));
        Set<String> names = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertEquals(1, names.size());
        assertFalse(names.contains("my.prop"));
        assertTrue(names.contains("another.prop"));
    }

    @Test
    void expandActiveProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config(
                        "app.http.port", "8081",
                        "%dev.app.http.port", "8082",
                        "%test.app.http.port", "8083",
                        "real.port", "${app.http.port}"))
                .withProfile("dev")
                .build();

        assertEquals("8082", config.getRawValue("real.port"));
    }

    @Test
    void priorityInParentClass() {
        InterceptorWithPriority interceptorWithPriority = new InterceptorWithPriority(new Child());
        assertEquals(1, interceptorWithPriority.getPriority());
    }

    @Test
    void restart() {
        List<Integer> counter = new ArrayList<>();
        counter.add(0);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("final", "final"))
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                        return new ConfigSourceInterceptor() {
                            @Override
                            public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                                counter.set(0, counter.get(0) + 1);
                                return context.proceed(name);
                            }
                        };
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(DEFAULT_PRIORITY + 100);
                    }
                })
                .withInterceptors(new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        if (name.equals("restart")) {
                            return context.restart("final");
                        }
                        return context.proceed(name);
                    }
                })
                .build();

        assertEquals("final", config.getRawValue("restart"));
        assertEquals(2, counter.get(0));
        assertEquals("final", config.getConfigValue("final").getName());
        assertEquals("final", config.getConfigValue("restart").getName());
    }

    @Test
    void restartNotInitialized() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("final", "final"))
                .withInterceptors(new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        if (name.equals("restart")) {
                            return context.restart("final");
                        }
                        return context.proceed(name);
                    }
                })
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        SmallRyeConfig config = new SmallRyeConfigBuilder()
                                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                                .withMapping(Restart.class)
                                .withMappingIgnore("*")
                                .build();
                        return emptyList();
                    }
                })
                .build();

        assertEquals("final", config.getRawValue("restart"));
    }

    @ConfigMapping
    interface Restart {
        Optional<String> restart();
    }

    @Test
    void supplier() {
        Value value = new Value();

        Supplier<Value> first = new Supplier<>() {
            @Override
            public Value get() {
                return value;
            }
        };

        Supplier<Value> second = new Supplier<>() {
            @Override
            public Value get() {
                return value;
            }
        };

        System.out.println(first.get().value);
        System.out.println(second.get().value);

        value.value = "something else";

        System.out.println(first.get().value);
        System.out.println(second.get().value);
    }

    public static class Value {
        String value = "value";
    }

    private static class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
        private static final Logger LOG = Logger.getLogger("io.smallrye.config");

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            ConfigValue configValue = context.proceed(name);
            String key = configValue.getName();
            String value = configValue.getValue();
            String configSource = configValue.getConfigSourceName();

            LOG.infov("The key {0} was loaded from {1} with the value {2}", key, configSource, value);

            return configValue;
        }
    }

    private static class OverridingConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return context.proceed("override");
        }
    }

    @Priority(Priorities.APPLICATION + 300)
    private static class HighPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("higher").build();
        }
    }

    @Priority(Priorities.APPLICATION + 200)
    private static class LowerPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("lower").build();
        }
    }

    @Priority(1)
    private abstract static class Parent implements ConfigSourceInterceptor {

    }

    private static class Child extends Parent {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return context.proceed(name);
        }
    }
}
