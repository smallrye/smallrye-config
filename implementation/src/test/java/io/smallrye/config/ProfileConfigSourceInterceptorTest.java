package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.microprofile.config.Config.PROFILE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

class ProfileConfigSourceInterceptorTest {
    @Test
    void profile() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("%prof.my.prop").getValue());
    }

    @Test
    void profileOnly() {
        final Config config = buildConfig("%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void fallback() {
        final Config config = buildConfig("my.prop", "1", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    void expressions() {
        final Config config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}", SMALLRYE_CONFIG_PROFILE, "prof");

        assertThrows(IllegalArgumentException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    void profileExpressions() {
        final Config config = buildConfig("my.prop", "1",
                "%prof.my.prop", "${%prof.my.prop.profile}",
                "%prof.my.prop.profile", "2",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void cannotExpand() {
        final Config config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "${another.prop}", SMALLRYE_CONFIG_PROFILE, "prof", "config_ordinal", "1000"))
                .withSources(config("my.prop", "${another.prop}", "%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof"))
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
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("prof.only").getValue());

        final List<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(properties.contains("%prof.my.prop"));
        assertTrue(properties.contains("my.prop"));
        assertTrue(properties.contains("prof.only"));
    }

    @Test
    void excludePropertiesFromInactiveProfiles() {
        final Config config = buildConfig("%prof.my.prop", "1", "%foo.another", "2");

        final List<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toList());
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
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .build();

        assertEquals("1234", config.getRawValue("common.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void multipleProfilesDocs() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,dev"))
                .withSources(config("my.prop", "1234", "%common.my.prop", "0", "%dev.my.prop", "5678", "%common.common.prop",
                        "common", "%dev.dev.prop", "dev", "%test.test.prop", "test"))
                .addDefaultInterceptors()
                .build();

        assertEquals("common", config.getRawValue("common.prop"));
        assertEquals("dev", config.getRawValue("dev.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("test.prop"));
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
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "300"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "500"))
                .addDefaultInterceptors()
                .build();

        assertEquals("5678", config.getRawValue("common.prop"));
    }

    @Test
    void multipleProfilesDifferentPrioritiesMain() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,prof", "config_ordinal", "1000"))
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
    void mpProfileRelocate() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "1", "%prof.my.prop", "2", PROFILE, "prof"))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("prof", config.getConfigValue(PROFILE).getValue());
        assertEquals("prof", config.getConfigValue(SMALLRYE_CONFIG_PROFILE).getValue());
        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains(PROFILE));
        assertFalse(properties.contains(SMALLRYE_CONFIG_PROFILE));

        config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "sr"))
                .build();

        properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains(SMALLRYE_CONFIG_PROFILE));
        assertTrue(properties.contains(PROFILE));
        assertEquals("sr", config.getConfigValue(SMALLRYE_CONFIG_PROFILE).getValue());
        assertNull(config.getConfigValue(PROFILE).getValue());
    }

    @Test
    void parentProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "prof"))
                .withSources(config(SMALLRYE_CONFIG_PROFILE_PARENT, "common"))
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .build();

        assertEquals("1234", config.getRawValue("common.prop"));
        assertEquals("5678", config.getRawValue("my.prop"));
    }

    @Test
    void parentProfileInActiveProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "custom"))
                .withSources(config("my.config1", "prod",
                        "my.config2", "prod",
                        "%dev.my.config1", "dev",
                        "%custom.smallrye.config.profile.parent", "dev",
                        "%custom.my.config2", "custom"))
                .addDefaultInterceptors()
                .build();

        assertEquals("custom", config.getRawValue("my.config2"));
        assertEquals("dev", config.getRawValue("my.config1"));
        assertEquals("dev", config.getRawValue(SMALLRYE_CONFIG_PROFILE_PARENT));
    }

    @Test
    void multipleProfilesSingleParent() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "another,custom"))
                .withSources(config("my.config1", "prod",
                        "my.config2", "prod",
                        "%dev.my.config1", "dev",
                        "%custom.my.config2", "custom"))
                .withSources(config("config_ordinal", "1000",
                        "%custom.smallrye.config.profile.parent", "dev"))
                .withSources(config("config_ordinal", "0",
                        "%another.smallrye.config.profile.parent", "prod"))
                .addDefaultInterceptors()
                .build();

        assertEquals("custom", config.getRawValue("my.config2"));
        assertEquals("dev", config.getRawValue("my.config1"));
        assertEquals("dev", config.getRawValue(SMALLRYE_CONFIG_PROFILE_PARENT));
    }

    @Test
    void parentProfileInActiveProfileWithRelocate() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                        Map<String, String> relocations = new HashMap<>();
                        relocations.put(SMALLRYE_CONFIG_PROFILE_PARENT, "quarkus.config.profile.parent");

                        ConfigValue profileValue = context.proceed(SMALLRYE_CONFIG_PROFILE);
                        if (profileValue != null) {
                            List<String> profiles = ProfileConfigSourceInterceptor.convertProfile(profileValue.getValue());
                            for (String profile : profiles) {
                                relocations.put("%" + profile + "." + SMALLRYE_CONFIG_PROFILE_PARENT,
                                        "%" + profile + "." + "quarkus.config.profile.parent");
                            }
                        }

                        return new RelocateConfigSourceInterceptor(relocations);
                    }

                    @Override
                    public OptionalInt getPriority() {
                        // Profile is 200, needs to execute before
                        return OptionalInt.of(Priorities.LIBRARY + 200 - 5);
                    }
                })
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "custom"))
                .withSources(config(
                        "my.config1", "prod",
                        "my.config2", "prod",
                        "%dev.my.config1", "dev",
                        "%custom.quarkus.config.profile.parent", "dev",
                        "%custom.my.config2", "custom"))
                .addDefaultInterceptors()
                .build();

        assertEquals("dev", config.getRawValue(SMALLRYE_CONFIG_PROFILE_PARENT));
        assertEquals("dev", config.getRawValue("quarkus.config.profile.parent"));
        assertEquals("dev", config.getRawValue("my.config1"));
        assertEquals("custom", config.getRawValue("my.config2"));
    }

    @Test
    void whitespaceProfiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, ""))
                .addDefaultInterceptors()
                .build();
        assertTrue(config.getProfiles().isEmpty());

        config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, " "))
                .addDefaultInterceptors()
                .build();
        assertTrue(config.getProfiles().isEmpty());

        config = new SmallRyeConfigBuilder()
                .withSources(config(Config.PROFILE, ""))
                .addDefaultInterceptors()
                .build();
        assertTrue(config.getProfiles().isEmpty());
    }

    @Test
    void profileInConfigValue() {
        SmallRyeConfig config = buildConfig("%prof.my.prop", "1234");

        ConfigValue configValue = config.getConfigValue("my.prop");
        assertEquals("1234", configValue.getValue());
        assertEquals("prof", configValue.getProfile());
        assertEquals("%prof.my.prop", configValue.getNameProfiled());
    }

    @Test
    void hierarchicalParentProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("%child." + SMALLRYE_CONFIG_PROFILE_PARENT, "parent", "%child.child", "Goten"))
                .withSources(config("%parent." + SMALLRYE_CONFIG_PROFILE_PARENT, "grandparent", "%parent.parent", "Goku"))
                .withSources(config("%grandparent." + SMALLRYE_CONFIG_PROFILE_PARENT, "greatgrandparent",
                        "%grandparent.grandparent", "Bardock"))
                .withSources(config("%greatgrandparent." + SMALLRYE_CONFIG_PROFILE_PARENT, "end",
                        "%greatgrandparent.greatgrandparent", "Gohan"))
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "child"))
                .build();

        assertArrayEquals(new String[] { "child", "parent", "grandparent", "greatgrandparent", "end" },
                config.getProfiles().toArray(new String[5]));

        assertEquals("Goten", config.getRawValue("child"));
        assertEquals("Goku", config.getRawValue("parent"));
        assertEquals("Bardock", config.getRawValue("grandparent"));
        assertEquals("Gohan", config.getRawValue("greatgrandparent"));
    }

    @Test
    void hierarchicalParentProfileMultiple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "a,b",
                        SMALLRYE_CONFIG_PROFILE_PARENT, "c,d",
                        "%a." + SMALLRYE_CONFIG_PROFILE_PARENT, "1,2"))
                .build();

        assertArrayEquals(new String[] { "b", "a", "2", "1", "d", "c" }, config.getProfiles().toArray(new String[6]));
    }

    @Test
    void multipleProfileProperty() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("%prod.my.override", "override", "config_ordinal", "1000"))
                .withSources(config("%prod,dev.my.prop", "value", "%prod,dev.my.override", "value", "config_ordinal", "100"))
                .withSources(config("%dev.my.prop", "minimal", "config_ordinal", "0"))
                .withSources(config("%prod,dev.another.prop", "multi", "%prod.another.prop", "single"))
                .withSources(config("%common,prod,dev.triple.prop", "triple", "%common,prod.triple.prop", "double"));

        SmallRyeConfig prod = builder.withProfile("prod").build();
        assertEquals("value", prod.getRawValue("my.prop"));
        assertEquals("value", prod.getRawValue("%prod.my.prop"));
        assertEquals("override", prod.getRawValue("my.override"));
        assertEquals("override", prod.getRawValue("%prod.my.override"));
        assertEquals("single", prod.getRawValue("another.prop"));
        assertEquals("double", prod.getRawValue("triple.prop"));

        SmallRyeConfig dev = builder.withProfile("dev").build();
        assertEquals("value", dev.getRawValue("my.prop"));
        assertEquals("value", dev.getRawValue("%dev.my.prop"));
        assertEquals("value", dev.getRawValue("my.override"));
        assertEquals("value", dev.getRawValue("%dev.my.override"));
        assertEquals("triple", dev.getRawValue("triple.prop"));

        SmallRyeConfig common = builder.withProfile("common").build();
        assertEquals("double", common.getRawValue("triple.prop"));
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .withSources(config(keyValues))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();
    }
}
