package io.smallrye.config;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.AbstractConfigSource;

public class ConfigConfigSourceTest {
    @Test
    public void configure() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public ConfigSource getConfigSource(final ConfigSourceContext context) {
                        return new AbstractConfigSource("test", 1000) {
                            final String value = context.getValue("my.prop").getValue();

                            @Override
                            public Map<String, String> getProperties() {
                                return null;
                            }

                            @Override
                            public String getValue(final String propertyName) {
                                return value;
                            }
                        };
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
    public void lowerPriority() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public ConfigSource getConfigSource(final ConfigSourceContext context) {
                        return new AbstractConfigSource("test", 0) {
                            final ConfigValue value = context.getValue("my.prop");

                            @Override
                            public Map<String, String> getProperties() {
                                return null;
                            }

                            @Override
                            public String getValue(final String propertyName) {
                                return value != null ? value.getValue() : null;
                            }
                        };
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
}
