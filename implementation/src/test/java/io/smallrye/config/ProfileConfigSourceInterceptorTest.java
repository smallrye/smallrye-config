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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

class ProfileConfigSourceInterceptorTest {
    @Test
    void profile() {
        SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));

        assertEquals("my.prop", config.getConfigValue("my.prop").getName());
        assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        assertEquals("2", config.getConfigValue("%prof.my.prop").getValue());
    }

    @Test
    void profileOnly() {
        SmallRyeConfig config = buildConfig("%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void fallback() {
        SmallRyeConfig config = buildConfig("my.prop", "1", SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    void expressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}", SMALLRYE_CONFIG_PROFILE, "prof");

        assertThrows(IllegalArgumentException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    void profileExpressions() {
        SmallRyeConfig config = buildConfig("my.prop", "1",
                "%prof.my.prop", "${%prof.my.prop.profile}",
                "%prof.my.prop.profile", "2",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void cannotExpand() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("my.prop", "${another.prop}", SMALLRYE_CONFIG_PROFILE, "prof", "config_ordinal", "1000"))
                .withSources(config("my.prop", "${another.prop}", "%prof.my.prop", "2", SMALLRYE_CONFIG_PROFILE, "prof"))
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getValue("my.prop", String.class));
    }

    @Test
    void customConfigProfile() {
        String[] configs = { "my.prop", "1", "%prof.my.prop", "2", "config.profile", "prof" };
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void noConfigProfile() {
        String[] configs = { "my.prop", "1", "%prof.my.prop", "2" };
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(configs))
                .build();

        assertEquals("1", config.getValue("my.prop", String.class));
    }

    @Test
    void priorityProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile("prof")
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
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    void priorityOverrideProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
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
                .build();

        assertEquals("higher", config.getValue("my.prop", String.class));
    }

    @Test
    void priorityProfileOverOriginal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile("prof")
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
                .build();

        assertEquals("higher-profile", config.getValue("my.prop", String.class));
    }

    @Test
    void propertyNames() {
        SmallRyeConfig config = buildConfig(
                "my.prop", "1",
                "%prof.my.prop", "2",
                "%prof.prof.only", "1",
                "%inactive.prop", "1",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("2", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("prof.only").getValue());

        List<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(properties.contains("%prof.my.prop"));
        assertTrue(properties.contains("my.prop"));
        assertTrue(properties.contains("prof.only"));
        // Inactive profile properties are included. We may need to revise this
        assertTrue(properties.contains("%inactive.prop"));
    }

    @Test
    void excludePropertiesFromInactiveProfiles() {
        SmallRyeConfig config = buildConfig("%prof.my.prop", "1", "%foo.another", "2");

        List<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertTrue(properties.contains("my.prop"));
        assertFalse(properties.contains("another"));
    }

    @Test
    void profileName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
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

        assertEquals("1234", config.getConfigValue("common.prop").getValue());
        assertEquals("5678", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void multipleProfilesDocs() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,dev"))
                .withSources(config("my.prop", "1234", "%common.my.prop", "0", "%dev.my.prop", "5678", "%common.common.prop",
                        "common", "%dev.dev.prop", "dev", "%test.test.prop", "test"))
                .addDefaultInterceptors()
                .build();

        assertEquals("common", config.getConfigValue("common.prop").getValue());
        assertEquals("dev", config.getConfigValue("dev.prop").getValue());
        assertEquals("5678", config.getConfigValue("my.prop").getValue());
        assertNull(config.getConfigValue("test.prop").getValue());
    }

    @Test
    void multipleProfilesSamePriority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("%foo.common.prop", "1234", "%bar.common.prop", "5678"))
                .addDefaultInterceptors()
                .withProfile("foo")
                .withProfile("bar")
                .build();

        assertEquals("5678", config.getConfigValue("common.prop").getValue());
    }

    @Test
    void multipleProfilesDifferentPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "common,prof", "config_ordinal", "1000"))
                .withSources(config("%prof.common.prop", "5678", "config_ordinal", "300"))
                .withSources(config("%common.common.prop", "1234", "config_ordinal", "500"))
                .addDefaultInterceptors()
                .build();

        assertEquals("5678", config.getConfigValue("common.prop").getValue());
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

        assertEquals("9", config.getConfigValue("common.prop").getValue());
    }

    @Test
    void builderProfiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("%common.common.prop", "1234", "%prof.my.prop", "5678"))
                .addDefaultInterceptors()
                .withProfile("common")
                .withProfile("prof")
                .build();

        assertEquals("1234", config.getConfigValue("common.prop").getValue());
        assertEquals("5678", config.getConfigValue("my.prop").getValue());
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

        assertEquals("1234", config.getConfigValue("common.prop").getValue());
        assertEquals("5678", config.getConfigValue("my.prop").getValue());
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

        assertEquals("custom", config.getConfigValue("my.config2").getValue());
        assertEquals("dev", config.getConfigValue("my.config1").getValue());
        assertEquals("dev", config.getConfigValue(SMALLRYE_CONFIG_PROFILE_PARENT).getValue());
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

        assertEquals("custom", config.getConfigValue("my.config2").getValue());
        assertEquals("dev", config.getConfigValue("my.config1").getValue());
        assertEquals("dev", config.getConfigValue(SMALLRYE_CONFIG_PROFILE_PARENT).getValue());
    }

    @Test
    void parentProfileInActiveProfileWithRelocate() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
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

        assertEquals("dev", config.getConfigValue(SMALLRYE_CONFIG_PROFILE_PARENT).getValue());
        assertEquals("dev", config.getConfigValue("quarkus.config.profile.parent").getValue());
        assertEquals("dev", config.getConfigValue("my.config1").getValue());
        assertEquals("custom", config.getConfigValue("my.config2").getValue());
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

        assertEquals("Goten", config.getConfigValue("child").getValue());
        assertEquals("Goku", config.getConfigValue("parent").getValue());
        assertEquals("Bardock", config.getConfigValue("grandparent").getValue());
        assertEquals("Gohan", config.getConfigValue("greatgrandparent").getValue());
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
                .withSources(config("%common,prod,dev.triple.prop", "triple", "%common,prod.triple.prop", "double"))
                .withSources(config("%commonone,prodone,devone.prop.start.with", "1"));

        SmallRyeConfig prod = builder.withProfile("prod").build();
        assertEquals("value", prod.getConfigValue("my.prop").getValue());
        assertEquals("value", prod.getConfigValue("%prod.my.prop").getValue());
        assertEquals("override", prod.getConfigValue("my.override").getValue());
        assertEquals("override", prod.getConfigValue("%prod.my.override").getValue());
        assertEquals("single", prod.getConfigValue("another.prop").getValue());
        assertEquals("double", prod.getConfigValue("triple.prop").getValue());
        Set<String> prodNames = StreamSupport.stream(prod.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(prodNames.contains("my.prop"));
        assertTrue(prodNames.contains("my.override"));
        assertTrue(prodNames.contains("another.prop"));
        assertTrue(prodNames.contains("triple.prop"));
        assertFalse(prodNames.contains("prop.start.with"));
        builder.getProfiles().clear();

        SmallRyeConfig dev = builder.withProfile("dev").build();
        assertEquals("value", dev.getConfigValue("my.prop").getValue());
        assertEquals("value", dev.getConfigValue("%dev.my.prop").getValue());
        assertEquals("value", dev.getConfigValue("my.override").getValue());
        assertEquals("value", dev.getConfigValue("%dev.my.override").getValue());
        assertEquals("triple", dev.getConfigValue("triple.prop").getValue());
        Set<String> devNames = StreamSupport.stream(dev.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(devNames.contains("my.prop"));
        assertTrue(devNames.contains("my.override"));
        assertTrue(devNames.contains("another.prop"));
        assertTrue(devNames.contains("triple.prop"));
        assertFalse(devNames.contains("prop.start.with"));
        builder.getProfiles().clear();

        SmallRyeConfig common = builder.withProfile("common").build();
        assertEquals("double", common.getConfigValue("triple.prop").getValue());
        Set<String> commonNames = StreamSupport.stream(common.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(commonNames.contains("triple.prop"));
        assertFalse(commonNames.contains("my.prop"));
        assertFalse(commonNames.contains("prop.start.with"));
        builder.getProfiles().clear();
    }

    @Test
    void duplicatedProfilesActive() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "prod,kubernetes"))
                .withSources(config(SMALLRYE_CONFIG_PROFILE_PARENT, "cluster"))
                .withSources(config("%kubernetes." + SMALLRYE_CONFIG_PROFILE_PARENT, "cluster"))
                .build();

        assertIterableEquals(List.of("kubernetes", "prod", "cluster"), config.getProfiles());
    }

    @Test
    void profilesLongerThanPropDoNotOverflowString() {
        String name = ProfileConfigSourceInterceptor.activeName("%a,b.c.d", List.of("test-with-native-agent"));
        assertEquals("%a,b.c.d", name);
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile("prof")
                .withSources(config(keyValues))
                .build();
    }
}
