package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.common.MapBackedConfigSource;

class ProfileConfigSourceInterceptorTest {
    @Test
    void profile() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("%prof.my.prop").getValue());
    }

    @Test
    void profileOnly() {
        final Config config = buildConfig("%prof.my.prop", "2", SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void fallback() {
        final Config config = buildConfig("my.prop", "1", SMALLRYE_PROFILE, "prof");

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    void expressions() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}", SMALLRYE_PROFILE, "prof");

        assertThrows(IllegalArgumentException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    void profileExpressions() {
        final Config config = buildConfig("my.prop", "1",
                "%prof.my.prop", "${%prof.my.prop.profile}",
                "%prof.my.prop.profile", "2",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void cannotExpand() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "${another.prop}", SMALLRYE_PROFILE, "prof", "config_ordinal", "1000"))
                .withSources(config("my.prop", "${another.prop}", "%prof.my.prop", "2", SMALLRYE_PROFILE, "prof"))
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    void customConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2", "config.profile", "prof" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void noConfigProfile() {
        final String[] configs = { "my.prop", "1", "%prof.my.prop", "2" };
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    void priorityProfile() {
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
    void priorityOverrideProfile() {
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
    void priorityProfileOverOriginal() {
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
    void propertyNames() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "2", "%prof.prof.only", "1",
                SMALLRYE_PROFILE, "prof");

        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("prof.only").getValue());

        final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(properties.contains("%prof.my.prop"));
        assertTrue(properties.contains("my.prop"));
        assertTrue(properties.contains("prof.only"));
    }

    @Test
    void excludePropertiesFromInactiveProfiles() {
        final Config config = buildConfig("%prof.my.prop", "1", "%foo.another", "2");

        final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertTrue(properties.contains("my.prop"));
        assertFalse(properties.contains("another"));
    }

    @Test
    void profileName() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1", "%prof.my.prop", "2"))
                .withProfile("prof")
                .build();

        assertEquals("2", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void multipleProfiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .build();

        assertEquals("1234", config.getRawValue("common.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void multipleProfilesSamePriority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("%foo.common.prop", "1234", "%bar.common.prop", "5678"))
                .addDefaultInterceptors()
                .withProfile("foo")
                .withProfile("bar")
                .build();

        assertEquals("5678", config.getRawValue("common.prop"));
    }

    @Test
    void multipleProfilesDifferentPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "300"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "500"))
                .addDefaultInterceptors()
                .build();

        assertEquals("5678", config.getRawValue("common.prop"));
    }

    @Test
    void multipleProfilesDifferentPrioritiesMain() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("common.prop", "9", "config_ordinal", "900"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "500"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "300"))
                .addDefaultInterceptors()
                .build();

        assertEquals("9", config.getRawValue("common.prop"));
    }

    @Test
    void builderProfiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .withProfile("common")
                .withProfile("prof")
                .build();

        assertEquals("1234", config.getRawValue("common.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void profilesClasspath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/microprofile-config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/microprofile-config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/microprofile-config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/microprofile-config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/"),
                new URL("jar:" + filePathTwo.toUri() + "!/"),
        }, contextClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("common,dev")
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("common", config.getRawValue("my.prop.common"));
        assertEquals("dev", config.getRawValue("my.prop.profile"));

        urlClassLoader.close();
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Test
    void mpProfileRelocate() {
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
                .withSources(config(keyValues))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();
    }
}
