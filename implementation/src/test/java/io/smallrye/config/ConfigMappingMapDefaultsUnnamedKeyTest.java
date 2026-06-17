package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Systematic tests for the behavior of {@link WithDefaults} and {@link WithUnnamedKey} on Maps in
 * {@link ConfigMapping} interfaces.
 * <p>
 * These tests document <b>expected</b> behavior. Some assertions currently fail because of a bug:
 * when {@code @WithUnnamedKey} is used and the config group has a config property with
 * {@code @WithDefault}, the unnamed config group key always appears in the map, even when nothing
 * is configured for it.
 *
 * @see <a href="https://quarkusio.zulipchat.com/#narrow/channel/187038-dev/topic/.40WithUnnamedKey.20and.20keySet">
 *      Zulip discussion</a>
 */
public class ConfigMappingMapDefaultsUnnamedKeyTest {

    // -- Config group interfaces (shared across multiple mappings) --

    interface ConfigGroup {
        @WithDefault("default-val")
        String configProperty1();

        Optional<String> configProperty2();
    }

    interface ConfigGroupNoPropertyDefaults {
        Optional<String> configProperty1();

        Optional<String> configProperty2();
    }

    // -- Non-map baseline config --

    @ConfigMapping(prefix = "non-map")
    interface NonMapConfig {
        ConfigGroup nested();
    }

    // -- Non-map baseline tests --

    @Test
    void nonMap_configProperty2Configured() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NonMapConfig.class)
                .withSources(config("non-map.nested.config-property2", "user-val"))
                .build();

        NonMapConfig nonMap = config.getConfigMapping(NonMapConfig.class);
        assertEquals("default-val", nonMap.nested().configProperty1());
        assertEquals(Optional.of("user-val"), nonMap.nested().configProperty2());
    }

    @Test
    void nonMap_noPropertiesConfigured() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NonMapConfig.class)
                .build();

        NonMapConfig nonMap = config.getConfigMapping(NonMapConfig.class);
        assertEquals("default-val", nonMap.nested().configProperty1());
        assertEquals(Optional.empty(), nonMap.nested().configProperty2());
    }

    // -- Map tests --

    @ParameterizedTest
    @MethodSource("mapMappingsAndConfigGroupKeys")
    <E> void keyConfigured(ConfigGroupMapMapping<E> mapping, ConfigGroupKey configGroupKey) {
        SmallRyeConfig config = mapping.buildConfig(mapping.configProperty2Key(configGroupKey), "user-val");
        Map<String, E> map = mapping.getConfigGroupMap(config);

        assertTrue(map.containsKey(configGroupKey.key));

        E configGroup = map.get(configGroupKey.key);
        assertNotNull(configGroup, "get(configured key) should not be null");
        assertEquals(Optional.of("user-val"), mapping.configProperty2Value(configGroup));
        if (mapping.hasConfigProperty1Default()) {
            assertEquals("default-val", mapping.configProperty1Value(configGroup));
        } else {
            assertNull(mapping.configProperty1Value(configGroup));
        }
    }

    @ParameterizedTest
    @MethodSource("mapMappingsAndConfigGroupKeys")
    <E> void noKeysConfigured(ConfigGroupMapMapping<E> mapping, ConfigGroupKey configGroupKey) {
        SmallRyeConfig config = mapping.buildConfig();
        Map<String, E> map = mapping.getConfigGroupMap(config);

        // ISSUE: when @WithUnnamedKey is used and the config group has a config property with
        // @WithDefault, the unnamed config group key materializes because @WithDefault generates
        // defaults-source properties (e.g. "with-unnamed-key-only.map.config-property1=default-val")
        // that match the unnamed key path (no key segment), causing the config group to materialize.
        // This happens with @WithUnnamedKey alone — @WithDefaults on the map is not required.
        assertFalse(map.containsKey(configGroupKey.key),
                "containsKey should be false when nothing is configured");

        E configGroup = map.get(configGroupKey.key);
        if (mapping.hasWithDefaults()) {
            assertNotNull(configGroup, "@WithDefaults should provide a default config group via get()");
            assertEquals(Optional.empty(), mapping.configProperty2Value(configGroup));
            if (mapping.hasConfigProperty1Default()) {
                assertEquals("default-val", mapping.configProperty1Value(configGroup));
            } else {
                assertNull(mapping.configProperty1Value(configGroup));
            }
        } else {
            assertNull(configGroup,
                    "get should return null when nothing is configured and @WithDefaults is not used");
        }
    }

    @ParameterizedTest
    @MethodSource("mapMappingsAndConfigGroupKeys")
    <E> void bothConfigGroupKeysConfigured(ConfigGroupMapMapping<E> mapping, ConfigGroupKey configGroupKey) {
        assumeTrue(mapping.hasWithUnnamedKey(), "Only applicable to mappings with @WithUnnamedKey");

        SmallRyeConfig config = mapping.buildConfig(
                mapping.configProperty2Key(ConfigGroupKey.NAMED), "named-val",
                mapping.configProperty2Key(ConfigGroupKey.UNNAMED), "unnamed-val");
        Map<String, E> map = mapping.getConfigGroupMap(config);

        String expectedOptVal = configGroupKey == ConfigGroupKey.NAMED ? "named-val" : "unnamed-val";

        assertTrue(map.containsKey(configGroupKey.key));

        E configGroup = map.get(configGroupKey.key);
        assertNotNull(configGroup);
        assertEquals(Optional.of(expectedOptVal), mapping.configProperty2Value(configGroup));
    }

    @ParameterizedTest
    @MethodSource("mapMappingsAndConfigGroupKeys")
    <E> void otherConfigGroupKeyConfigured(ConfigGroupMapMapping<E> mapping, ConfigGroupKey configGroupKey) {
        assumeTrue(mapping.hasWithUnnamedKey(), "Only applicable to mappings with @WithUnnamedKey");

        ConfigGroupKey otherKey = configGroupKey == ConfigGroupKey.NAMED
                ? ConfigGroupKey.UNNAMED
                : ConfigGroupKey.NAMED;
        SmallRyeConfig config = mapping.buildConfig(mapping.configProperty2Key(otherKey), "other-val");
        Map<String, E> map = mapping.getConfigGroupMap(config);

        // ISSUE: same unnamed config group key materialization as in noKeysConfigured.
        assertFalse(map.containsKey(configGroupKey.key),
                "containsKey should be false for a config group key that was not configured");

        E configGroup = map.get(configGroupKey.key);
        if (mapping.hasWithDefaults()) {
            assertNotNull(configGroup, "@WithDefaults should provide a default config group via get()");
            assertEquals(Optional.empty(), mapping.configProperty2Value(configGroup));
            if (mapping.hasConfigProperty1Default()) {
                assertEquals("default-val", mapping.configProperty1Value(configGroup));
            } else {
                assertNull(mapping.configProperty1Value(configGroup));
            }
        } else {
            assertNull(configGroup,
                    "get should return null for a config group key that was not configured");
        }
    }

    // ============================
    // Infrastructure
    // ============================

    enum ConfigGroupKey {
        NAMED("mykey"),
        UNNAMED("<default>");

        final String key;

        ConfigGroupKey(String key) {
            this.key = key;
        }
    }

    // -- ConfigGroupMapMapping abstraction --

    static abstract class ConfigGroupMapMapping<E> {
        abstract String name();

        abstract Class<?>[] mappingClasses();

        abstract Map<String, E> getConfigGroupMap(SmallRyeConfig config);

        abstract String namedKeyConfigProperty2Key();

        abstract String unnamedKeyConfigProperty2Key();

        String configProperty2Key(ConfigGroupKey configGroupKey) {
            return configGroupKey == ConfigGroupKey.NAMED
                    ? namedKeyConfigProperty2Key()
                    : unnamedKeyConfigProperty2Key();
        }

        abstract boolean hasWithDefaults();

        abstract boolean hasWithUnnamedKey();

        abstract boolean hasConfigProperty1Default();

        abstract Optional<String> configProperty2Value(E configGroup);

        abstract String configProperty1Value(E configGroup);

        SmallRyeConfig buildConfig(String... keyValues) {
            SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
            for (Class<?> c : mappingClasses()) {
                builder.withMapping(c);
            }
            if (keyValues.length > 0) {
                builder.withSources(config(keyValues));
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return name();
        }
    }

    static abstract class PropertyDefaultsConfigGroupMapMapping extends ConfigGroupMapMapping<ConfigGroup> {
        @Override
        boolean hasConfigProperty1Default() {
            return true;
        }

        @Override
        Optional<String> configProperty2Value(ConfigGroup configGroup) {
            return configGroup.configProperty2();
        }

        @Override
        String configProperty1Value(ConfigGroup configGroup) {
            return configGroup.configProperty1();
        }
    }

    static abstract class NoPropertyDefaultsConfigGroupMapMapping
            extends ConfigGroupMapMapping<ConfigGroupNoPropertyDefaults> {
        @Override
        boolean hasConfigProperty1Default() {
            return false;
        }

        @Override
        Optional<String> configProperty2Value(ConfigGroupNoPropertyDefaults configGroup) {
            return configGroup.configProperty2();
        }

        @Override
        String configProperty1Value(ConfigGroupNoPropertyDefaults configGroup) {
            return configGroup.configProperty1().orElse(null);
        }
    }

    // -- Method source --

    static Stream<Arguments> mapMappingsAndConfigGroupKeys() {
        return Stream.<ConfigGroupMapMapping<?>> of(
                new PlainMapping(),
                new WithDefaultsOnlyMapping(),
                new WithUnnamedKeyOnlyMapping(),
                new WithDefaultsAndUnnamedKeyMapping(),
                new WithParentNameDefaultsAndUnnamedKeyMapping(),
                new WithUnnamedKeyNoPropertyDefaultsMapping())
                .flatMap(m -> {
                    Stream.Builder<Arguments> b = Stream.builder();
                    b.add(Arguments.of(m, ConfigGroupKey.NAMED));
                    if (m.hasWithUnnamedKey()) {
                        b.add(Arguments.of(m, ConfigGroupKey.UNNAMED));
                    }
                    return b.build();
                });
    }

    // -- ConfigGroupMapMapping implementations --

    static class PlainMapping extends PropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "plain-map")
        interface Config {
            Map<String, ConfigGroup> map();
        }

        @Override
        String name() {
            return "plain-map";
        }

        @Override
        Map<String, ConfigGroup> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "plain-map.map.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return null;
        }

        @Override
        boolean hasWithDefaults() {
            return false;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return false;
        }
    }

    static class WithDefaultsOnlyMapping extends PropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "with-defaults-only")
        interface Config {
            @WithDefaults
            Map<String, ConfigGroup> map();
        }

        @Override
        String name() {
            return "with-defaults-only";
        }

        @Override
        Map<String, ConfigGroup> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "with-defaults-only.map.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return null;
        }

        @Override
        boolean hasWithDefaults() {
            return true;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return false;
        }
    }

    static class WithUnnamedKeyOnlyMapping extends PropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "with-unnamed-key-only")
        interface Config {
            @WithUnnamedKey("<default>")
            Map<String, ConfigGroup> map();
        }

        @Override
        String name() {
            return "with-unnamed-key-only";
        }

        @Override
        Map<String, ConfigGroup> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "with-unnamed-key-only.map.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return "with-unnamed-key-only.map.config-property2";
        }

        @Override
        boolean hasWithDefaults() {
            return false;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return true;
        }
    }

    static class WithDefaultsAndUnnamedKeyMapping extends PropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "with-defaults-and-unnamed-key")
        interface Config {
            @WithDefaults
            @WithUnnamedKey("<default>")
            Map<String, ConfigGroup> map();
        }

        @Override
        String name() {
            return "with-defaults-and-unnamed-key";
        }

        @Override
        Map<String, ConfigGroup> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "with-defaults-and-unnamed-key.map.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return "with-defaults-and-unnamed-key.map.config-property2";
        }

        @Override
        boolean hasWithDefaults() {
            return true;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return true;
        }
    }

    static class WithParentNameDefaultsAndUnnamedKeyMapping extends PropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "with-parent-name-defaults-and-unnamed-key")
        interface Config {
            @WithParentName
            @WithDefaults
            @WithUnnamedKey("<default>")
            Map<String, ConfigGroup> map();
        }

        @Override
        String name() {
            return "with-parent-name-defaults-and-unnamed-key";
        }

        @Override
        Map<String, ConfigGroup> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "with-parent-name-defaults-and-unnamed-key.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return "with-parent-name-defaults-and-unnamed-key.config-property2";
        }

        @Override
        boolean hasWithDefaults() {
            return true;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return true;
        }
    }

    static class WithUnnamedKeyNoPropertyDefaultsMapping extends NoPropertyDefaultsConfigGroupMapMapping {
        @ConfigMapping(prefix = "with-unnamed-key-no-property-defaults")
        interface Config {
            @WithUnnamedKey("<default>")
            Map<String, ConfigGroupNoPropertyDefaults> map();
        }

        @Override
        String name() {
            return "with-unnamed-key-no-property-defaults";
        }

        @Override
        Map<String, ConfigGroupNoPropertyDefaults> getConfigGroupMap(SmallRyeConfig config) {
            return config.getConfigMapping(Config.class).map();
        }

        @Override
        Class<?>[] mappingClasses() {
            return new Class<?>[] { Config.class };
        }

        @Override
        String namedKeyConfigProperty2Key() {
            return "with-unnamed-key-no-property-defaults.map.mykey.config-property2";
        }

        @Override
        String unnamedKeyConfigProperty2Key() {
            return "with-unnamed-key-no-property-defaults.map.config-property2";
        }

        @Override
        boolean hasWithDefaults() {
            return false;
        }

        @Override
        boolean hasWithUnnamedKey() {
            return true;
        }
    }
}
