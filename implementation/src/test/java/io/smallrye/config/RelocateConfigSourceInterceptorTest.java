package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.mappedProperties;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
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
import java.util.stream.Collectors;

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
        assertEquals("mp.jwt.token.header", configValue.getName());
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
        SmallRyeConfig config = buildConfig("smallrye.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getRawValue("smallrye.jwt.token.header"));
        assertEquals("Authorization", config.getRawValue("mp.jwt.token.header"));
        List<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("smallrye.jwt.token.header"));
        assertTrue(names.contains("mp.jwt.token.header"));
    }

    @Test
    void fallbackPropertyNames() {
        SmallRyeConfig config = buildConfig("mp.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getRawValue("smallrye.jwt.token.header"));
        assertEquals("Authorization", config.getRawValue("mp.jwt.token.header"));
        List<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("smallrye.jwt.token.header"));
        assertTrue(names.contains("mp.jwt.token.header"));
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
        assertEquals("new", value.getName());
        assertEquals("dev", value.getProfile());
        assertEquals("%dev.new", value.getNameProfiled());
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
                names.addAll(mappedProperties(configClassWithPrefix(Child.class), hierarchyCandidates));
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

    @Test
    void relocateMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new RelocateConfigSourceInterceptor(name -> name.replaceFirst("old", "new")))
                .withSources(config("config_ordinal", "1",
                        "reloc.old.value", "old",
                        "reloc.old.list[0]", "old", "reloc.old.list[1]", "old",
                        "reloc.old.map.key", "old", "reloc.old.map.old", "old"))
                .withSources(config("config_ordinal", "2",
                        "reloc.new.value", "new",
                        "reloc.new.list[0]", "new",
                        "reloc.new.map.key", "new", "reloc.old.map.new", "new"))
                .withMapping(RelocateMapping.class)
                .build();

        RelocateMapping mapping = config.getConfigMapping(RelocateMapping.class);

        assertEquals("new", mapping.value());
        // TODO - Maps and Lists are merged. Is this what we want? Related with https://github.com/quarkusio/quarkus/issues/38786
        assertEquals(2, mapping.list().size());
        assertEquals("new", mapping.list().get(0));
        assertEquals("old", mapping.list().get(1));
        assertEquals(3, mapping.map().size());
        assertEquals("new", mapping.map().get("key"));
        assertEquals("new", mapping.map().get("new"));
        assertEquals("old", mapping.map().get("old"));

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        Set<String> mappedProperties = mappedProperties(configClassWithPrefix(RelocateMapping.class), properties);
        properties.removeAll(mappedProperties);
        Set<String> relocateProperties = new HashSet<>();
        for (String property : properties) {
            if (mappedProperties.contains(property)) {
                ConfigValue configValue = config.getConfigValue(property);
                relocateProperties.add(configValue.getName());
            }
        }
        properties.removeAll(relocateProperties);
    }

    @Test
    void fallbackMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new RelocateConfigSourceInterceptor(name -> name.replaceFirst("old", "new")))
                .withInterceptors(new FallbackConfigSourceInterceptor(name -> name.replaceFirst("new", "old")))
                .withSources(config(
                        "fall.old.value", "old",
                        "fall.old.list[0]", "old", "fall.old.list[1]", "old",
                        "fall.old.map.key", "old", "fall.old.map.old", "old"))
                .withMapping(FallbackMapping.class)
                .build();

        FallbackMapping mapping = config.getConfigMapping(FallbackMapping.class);

        assertEquals("old", mapping.value());
        assertEquals(2, mapping.list().size());
        assertEquals("old", mapping.list().get(0));
        assertEquals("old", mapping.list().get(1));
        assertEquals(2, mapping.map().size());
        assertEquals("old", mapping.map().get("key"));
        assertEquals("old", mapping.map().get("old"));

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        Set<String> mappedProperties = mappedProperties(configClassWithPrefix(FallbackMapping.class), properties);
        properties.removeAll(mappedProperties);
        Set<String> fallbackProperties = new HashSet<>();
        for (String property : properties) {
            ConfigValue configValue = config.getConfigValue(property);
            if (mappedProperties.contains(configValue.getName())) {
                fallbackProperties.add(property);
            }
        }
        properties.removeAll(fallbackProperties);
        assertTrue(properties.isEmpty());
    }

    @ConfigMapping(prefix = "reloc.old")
    interface RelocateMapping {
        String value();

        List<String> list();

        Map<String, String> map();
    }

    @ConfigMapping(prefix = "fall.new")
    interface FallbackMapping {
        String value();

        List<String> list();

        Map<String, String> map();
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
