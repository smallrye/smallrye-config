package io.smallrye.config;

import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

public class ProfileConfigSourceInterceptorTest {
    @Test
    public void profile() {
        final SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_PROFILE,
                "prof");

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
    }

    @Test
    public void profileOnly() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void fallback() {
        final Config config = buildConfig("my.prop", "1", SMALLRYE_PROFILE, "prof");

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    public void expressions() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}", SMALLRYE_PROFILE, "prof");

        assertThrows(IllegalArgumentException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    public void profileExpressions() {
        final Config config = buildConfig("my.prop", "1",
                "%prof.my.prop", "${%prof.my.prop.profile}",
                "%prof.my.prop.profile", "2",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void customConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2", "config.profile", "prof" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredInterceptors()
                .withSources(KeyValuesConfigSource.config(configs))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void noConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(configs))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityProfile() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("%prof.my.prop", "higher-profile");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityOverrideProfile() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("my.prop", "higher");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher", config.getValue("my.prop", String.class));
    }

    @Test
    public void priorityProfileOverOriginal() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(
                        new MapBackedConfigSource("higher", new HashMap<String, String>() {
                            {
                                put("my.prop", "higher");
                                put("%prof.my.prop", "higher-profile");
                            }
                        }, 200) {
                        },
                        new MapBackedConfigSource("lower", new HashMap<String, String>() {
                            {
                                put("my.prop", "lower");
                                put("%prof.my.prop", "lower-profile");
                            }
                        }, 100) {
                        })
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    public void propertyNames() {
        final SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1", "%prof.my.prop", "2", "%prof.prof.only", "1",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("prof.only").getValue());

        final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(properties.contains("%prof.my.prop"));
        assertTrue(properties.contains("my.prop"));
        assertTrue(properties.contains("prof.only"));
    }

    @Test
    public void profileName() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("my.prop", "1", "%prof.my.prop", "2"))
                .withProfile("prof")
                .build();

        assertEquals("2", config.getConfigValue("my.prop").getValue());
    }

    @Test
    public void mpProfileRelocate() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(
                        KeyValuesConfigSource.config("my.prop", "1", "%prof.my.prop", "2", "mp.config.profile", "prof"))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("my.prop").getValue());
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();
    }
}
