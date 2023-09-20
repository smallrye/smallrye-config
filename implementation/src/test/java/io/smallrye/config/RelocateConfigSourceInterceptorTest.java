package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class RelocateConfigSourceInterceptorTest {
    @Test
    void relocateAndFallback() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "mp.jwt.token.cookie", "Bearer");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        assertEquals("Bearer", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void relocate() {
        Config config = buildConfig(
                "smallrye.jwt.token.header", "Cookie",
                "mp.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void fallback() {
        Config config = builder()
                .withSources(config("smallrye.jwt.token.cookie", "jwt", "config_ordinal", "1000"))
                .withSources(config("mp.jwt.token.cookie", "Bearer")).build();

        assertEquals("jwt", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void fallbackEmpty() {
        Config config = builder()
                .withSources(config("smallrye.jwt.token.header", "Authorization", "config_ordinal", "1000"))
                .withSources(config("mp.jwt.token.header", "")).build();

        ConfigValue configValue = (ConfigValue) config.getConfigValue("smallrye.jwt.token.header");
        assertEquals("smallrye.jwt.token.header", configValue.getName());
        assertEquals("Authorization", configValue.getValue());
    }

    @Test
    void relocateWithProfile() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void relocateWithProfileWithMappingProperties() {
        Map<String, String> relocations = new HashMap<>();
        relocations.put("original.name", "relocated.name");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withInterceptorFactories(new ConfigSourceInterceptorFactory() {
                    @Override
                    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                        return new RelocateConfigSourceInterceptor(relocations);
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(Priorities.LIBRARY + 300);
                    }
                })
                .withSources(config(SMALLRYE_CONFIG_PROFILE, "custom"))
                .withSources(config("%custom.original.name", "original", "%custom.relocated.name", "relocated"))
                .addDefaultInterceptors()
                .build();

        assertEquals("relocated", config.getRawValue("original.name"));
        assertEquals("relocated", config.getRawValue("relocated.name"));
    }

    @Test
    void relocateWithProfileAndExpression() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void relocateWithProfileExpressionAndFallback() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                "smallrye.jwt.token.cookie", "jwt",
                "%prof.smallrye.jwt.token.cookie", "Basic",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Basic", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void relocateIsSecret() {
        Config config = buildConfig(
                Collections.singleton("mp.jwt.token.header"),
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertThrows(SecurityException.class, () -> config.getValue("smallrye.jwt.token.header", String.class));
        assertThrows(SecurityException.class, () -> config.getValue("mp.jwt.token.header", String.class));
    }

    @Test
    void relocatePropertyNames() {
        Config config = buildConfig("smallrye.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        List<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("smallrye.jwt.token.header"));
        assertTrue(names.contains("mp.jwt.token.header"));

        RelocateConfigSourceInterceptor relocateInterceptor = new RelocateConfigSourceInterceptor(
                s -> s.replaceAll("smallrye\\.jwt\\.token\\.header", "mp.jwt.token.header"));
        Iterator<ConfigValue> configValues = relocateInterceptor.iterateValues(new ConfigSourceInterceptorContext() {
            @Override
            public ConfigValue proceed(final String name) {
                return null;
            }

            @Override
            public Iterator<String> iterateNames() {
                return null;
            }

            @Override
            public Iterator<ConfigValue> iterateValues() {
                Set<ConfigValue> values = new HashSet<>();
                values.add(
                        ConfigValue.builder().withName("smallrye.jwt.token.header").withValue("Authorization").build());
                return values.iterator();
            }
        });

        Map<String, ConfigValue> values = new HashMap<>();
        while (configValues.hasNext()) {
            ConfigValue configValue = configValues.next();
            values.put(configValue.getName(), configValue);
        }

        assertEquals(2, values.size());
        assertEquals("Authorization", values.get("smallrye.jwt.token.header").getValue());
        assertEquals("Authorization", values.get("mp.jwt.token.header").getValue());
    }

    @Test
    void fallbackPropertyNames() {
        SmallRyeConfig config = buildConfig("mp.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        List<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("smallrye.jwt.token.header"));
        assertTrue(names.contains("mp.jwt.token.header"));

        FallbackConfigSourceInterceptor fallbackInterceptor = new FallbackConfigSourceInterceptor(
                s -> s.replaceAll("mp\\.jwt\\.token\\.header", "smallrye.jwt.token.header"));
        Iterator<ConfigValue> configValues = fallbackInterceptor.iterateValues(new ConfigSourceInterceptorContext() {
            @Override
            public ConfigValue proceed(final String name) {
                return null;
            }

            @Override
            public Iterator<String> iterateNames() {
                return null;
            }

            @Override
            public Iterator<ConfigValue> iterateValues() {
                Set<ConfigValue> values = new HashSet<>();
                values.add(ConfigValue.builder().withName("mp.jwt.token.header").withValue("Authorization").build());
                return values.iterator();
            }
        });

        Map<String, ConfigValue> values = new HashMap<>();
        while (configValues.hasNext()) {
            ConfigValue configValue = configValues.next();
            values.put(configValue.getName(), configValue);
        }

        assertEquals(2, values.size());
        assertEquals("Authorization", values.get("smallrye.jwt.token.header").getValue());
        assertEquals("Authorization", values.get("mp.jwt.token.header").getValue());
    }

    @Test
    void relocatePropertyNameToProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new RelocateConfigSourceInterceptor(singletonMap("old", "new")))
                .withSources(config("old", "0", "new", "1234", "%dev.new", "5678"))
                .withProfile("dev")
                .build();

        ConfigValue value = config.getConfigValue("old");
        assertEquals("5678", value.getValue());
        assertEquals("new", value.getName());
        assertEquals("dev", value.getProfile());
        assertEquals("%dev.new", value.getNameProfiled());
    }

    @Test
    void fallbackPropertyNameToProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new FallbackConfigSourceInterceptor(singletonMap("new", "old")))
                .withSources(config("old", "1234", "%dev.old", "5678"))
                .withProfile("dev")
                .build();

        ConfigValue value = config.getConfigValue("new");
        assertEquals("5678", value.getValue());
        assertEquals("old", value.getName());
        assertEquals("dev", value.getProfile());
        assertEquals("%dev.old", value.getNameProfiled());
    }

    @Test
    void fallbackMaps() {
        FallbackConfigSourceInterceptor fallbackConfigSourceInterceptor = new FallbackConfigSourceInterceptor(name -> {
            if (name.startsWith("child.")) {
                return "parent." + name.substring(6);
            }
            return name;
        }) {
            @Override
            public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                Set<String> names = new HashSet<>();
                Set<String> hierarchyCandidates = new HashSet<>();
                Iterator<String> namesIterator = context.iterateNames();
                while (namesIterator.hasNext()) {
                    String name = namesIterator.next();
                    names.add(name);
                    if (name.startsWith("parent.")) {
                        hierarchyCandidates.add("child." + name.substring(7));
                    }
                }
                names.addAll(ConfigMappings.mappedProperties(
                        ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Child.class), hierarchyCandidates));
                return names.iterator();
            }
        };

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "parent.parent", "parent", "child.child", "child",
                        "parent.value", "parent",
                        "parent.labels.parent", "parent",
                        "parent.nested.parent.value", "parent-nested"))
                .withInterceptors(fallbackConfigSourceInterceptor)
                .withMapping(Parent.class)
                .withMapping(Child.class)
                .build();

        Parent parent = config.getConfigMapping(Parent.class);
        Child child = config.getConfigMapping(Child.class);

        assertEquals("parent", parent.value().orElse(null));
        assertEquals("parent", child.value().orElse(null));
        assertEquals("parent", parent.labels().get("parent"));
        assertEquals("parent", child.labels().get("parent"));
        assertEquals("parent-nested", parent.nested().get("parent").value());
        assertEquals("parent-nested", child.nested().get("parent").value());

        config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "parent.parent", "parent", "child.child", "child",
                        "parent.value", "parent", "child.value", "child",
                        "parent.labels.parent", "parent", "child.labels.child", "child",
                        "parent.nested.parent.value", "parent-nested", "child.nested.child.value", "child-nested"))
                .withInterceptors(fallbackConfigSourceInterceptor)
                .withMapping(Parent.class)
                .withMapping(Child.class)
                .build();

        parent = config.getConfigMapping(Parent.class);
        child = config.getConfigMapping(Child.class);

        assertEquals("parent", parent.value().orElse(null));
        assertEquals("child", child.value().orElse(null));
        assertEquals(1, parent.labels().size());
        assertEquals("parent", parent.labels().get("parent"));
        assertEquals(2, child.labels().size());
        assertEquals("child", child.labels().get("child"));
        assertEquals(1, parent.nested().size());
        assertEquals("parent-nested", parent.nested().get("parent").value());
        assertEquals(2, child.nested().size());
        assertEquals("child-nested", child.nested().get("child").value());
    }

    @ConfigMapping(prefix = "parent")
    interface Parent {
        String parent();

        Optional<String> value();

        Map<String, String> labels();

        Map<String, Nested> nested();

        interface Nested {
            String value();
        }
    }

    @ConfigMapping(prefix = "child")
    interface Child {
        String child();

        Optional<String> value();

        Map<String, String> labels();

        Map<String, Nested> nested();

        interface Nested {
            String value();
        }
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return builder(Collections.emptySet(), keyValues).build();
    }

    private static SmallRyeConfig buildConfig(Set<String> secretKeys, String... keyValues) {
        return builder(secretKeys, keyValues).build();
    }

    private static SmallRyeConfigBuilder builder(String... keyValues) {
        return builder(Collections.emptySet(), keyValues);
    }

    private static SmallRyeConfigBuilder builder(Set<String> secretKeys, String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config(keyValues))
                .withInterceptors(
                        new RelocateConfigSourceInterceptor(
                                s -> s.replaceAll("smallrye\\.jwt\\.token\\.header", "mp.jwt.token.header")),
                        new FallbackConfigSourceInterceptor(
                                s -> s.replaceAll("mp\\.jwt\\.token\\.header", "smallrye.jwt.token.header")),
                        new FallbackConfigSourceInterceptor(
                                s -> s.replaceAll("smallrye\\.jwt", "mp.jwt")))
                .withSecretKeys(secretKeys.toArray(new String[0]));
    }
}
