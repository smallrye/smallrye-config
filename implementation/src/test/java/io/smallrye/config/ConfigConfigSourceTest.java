package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.MapBackedConfigSource;

class ConfigConfigSourceTest {
    @Test
    void configure() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1234"))
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        return singletonList(new AbstractConfigSource("test", 1000) {
                            final String value = context.getValue("my.prop").getValue();

                            @Override
                            public Map<String, String> getProperties() {
                                return Collections.emptyMap();
                            }

                            @Override
                            public Set<String> getPropertyNames() {
                                return Collections.emptySet();
                            }

                            @Override
                            public String getValue(final String propertyName) {
                                return value;
                            }
                        });
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(1000);
                    }
                })
                .build();

        final List<ConfigSource> configSources = StreamSupport.stream(config.getConfigSources().spliterator(), false)
                .collect(toList());
        assertEquals(1, configSources.stream().filter(source -> source.getName().equals("test")).count());

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("test", config.getConfigValue("my.prop").getConfigSourceName());
        assertEquals("1234", config.getRawValue("any"));
        assertEquals("test", config.getConfigValue("any").getConfigSourceName());
    }

    @Test
    void lowerPriority() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1234"))
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        return singletonList(new AbstractConfigSource("test", 0) {
                            final ConfigValue value = context.getValue("my.prop");

                            @Override
                            public Map<String, String> getProperties() {
                                return Collections.emptyMap();
                            }

                            @Override
                            public Set<String> getPropertyNames() {
                                return Collections.emptySet();
                            }

                            @Override
                            public String getValue(final String propertyName) {
                                return value != null ? value.getValue() : null;
                            }
                        });
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(0);
                    }
                })
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("KeyValuesConfigSource", config.getConfigValue("my.prop").getConfigSourceName());
        assertEquals("1234", config.getRawValue("any"));
        assertEquals("test", config.getConfigValue("any").getConfigSourceName());
    }

    @Test
    void iterate() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("smallrye.prop", "1", "smallrye.another", "2", "mp.prop", "1"))
                .withSources(new ConfigurableConfigSource(new ConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        Map<String, String> properties = new HashMap<>();
                        context.iterateNames().forEachRemaining(s -> {
                            if (s.startsWith("smallrye")) {
                                properties.put(s, "1234");
                            }
                        });
                        return singletonList(config(properties));
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(1000);
                    }
                })).build();

        assertEquals("1234", config.getRawValue("smallrye.prop"));
        assertEquals("1234", config.getRawValue("smallrye.another"));
        assertEquals("1", config.getRawValue("mp.prop"));
    }

    @Test
    void doNotOverrideInitialChain() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "foo", "%foo.my.prop", "1234", "%bar.my.prop", "5678"))
                .withSources(new ConfigurableConfigSource(context -> singletonList(config(SMALLRYE_CONFIG_PROFILE, "bar"))))
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
    }

    @Test
    void configOrdinal() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(config("config_ordinal", "200", "my.prop", "1234"))
                .withSources(new ConfigurableConfigSource(
                        context -> singletonList(config("config_ordinal", "400", "my.prop", "5678"))))
                .build();

        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void profiles() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withProfile("foo,bar")
                .withSources(new ConfigurableConfigSource(
                        context -> singletonList(config("profiles", String.join(",", context.getProfiles())))))
                .build();

        // Profiles come in priority order
        assertEquals("bar,foo", config.getRawValue("profiles"));
    }

    @Test
    void interceptorsFirst() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                        return new ConfigSourceInterceptor() {
                            @Override
                            public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                                if (name.equals("my.prop")) {
                                    throw new RuntimeException();
                                }

                                return context.proceed(name);
                            }
                        };
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(Integer.MIN_VALUE);
                    }
                })
                .withSources(new MapBackedConfigSource("test", new HashMap<String, String>() {
                    {
                        put("my.prop", "1234");
                    }
                }, Integer.MAX_VALUE) {
                })
                .build();

        assertEquals(Integer.MAX_VALUE, config.getConfigSources().iterator().next().getOrdinal());
        assertEquals("1234", config.getConfigSources().iterator().next().getValue("my.prop"));
        assertThrows(RuntimeException.class, () -> config.getRawValue("my.prop"));
    }
}
