/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import static io.smallrye.config.Converters.BOOLEAN_CONVERTER;
import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;
import io.smallrye.config.EnvConfigSource.EnvName;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
class EnvConfigSourceTest {
    @Test
    void conversionOfEnvVariableNames() {
        String envProp = System.getenv("SMALLRYE_MP_CONFIG_PROP");
        assertNotNull(envProp);

        ConfigSource cs = new EnvConfigSource();
        assertEquals(envProp, cs.getValue("SMALLRYE_MP_CONFIG_PROP"));
        // the config source returns only the name of the actual env variable
        assertTrue(cs.getPropertyNames().contains("SMALLRYE_MP_CONFIG_PROP"));

        assertEquals(envProp, cs.getValue("smallrye_mp_config_prop"));
        assertFalse(cs.getPropertyNames().contains("smallrye_mp_config_prop"));

        assertEquals(envProp, cs.getValue("smallrye.mp.config.prop"));
        assertTrue(cs.getPropertyNames().contains("smallrye.mp.config.prop"));

        assertEquals(envProp, cs.getValue("SMALLRYE.MP.CONFIG.PROP"));
        assertFalse(cs.getPropertyNames().contains("SMALLRYE.MP.CONFIG.PROP"));

        assertEquals(envProp, cs.getValue("smallrye-mp-config-prop"));
        assertFalse(cs.getPropertyNames().contains("smallrye-mp-config-prop"));

        assertEquals(envProp, cs.getValue("SMALLRYE-MP-CONFIG-PROP"));
        assertFalse(cs.getPropertyNames().contains("SMALLRYE-MP-CONFIG-PROP"));

        assertEquals("1234", cs.getValue("smallrye_mp_config_prop_lower"));
        assertTrue(cs.getPropertyNames().contains("smallrye_mp_config_prop_lower"));

        assertEquals("1234", cs.getValue("smallrye/mp/config/prop"));
    }

    @Test
    void profileEnvVariables() {
        assertNotNull(System.getenv("SMALLRYE_MP_CONFIG_PROP"));
        assertNotNull(System.getenv("_ENV_SMALLRYE_MP_CONFIG_PROP"));

        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().withProfile("env").build();

        assertEquals("5678", config.getRawValue("smallrye.mp.config.prop"));
    }

    @Test
    void empty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().build();
        assertThrows(NoSuchElementException.class, () -> config.getValue("SMALLRYE_MP_CONFIG_EMPTY", String.class));
        assertTrue(
                stream(config.getPropertyNames().spliterator(), false).collect(toList()).contains("SMALLRYE_MP_CONFIG_EMPTY"));

        Optional<ConfigSource> envConfigSource = config.getConfigSource("EnvConfigSource");

        assertTrue(envConfigSource.isPresent());
        assertEquals("", envConfigSource.get().getValue("SMALLRYE_MP_CONFIG_EMPTY"));
    }

    @Test
    void ordinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(new EnvConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertTrue(configSource instanceof EnvConfigSource);
        assertEquals(301, configSource.getOrdinal());
    }

    @Test
    void indexed() {
        Map<String, String> env = new HashMap<>() {
            {
                put("INDEXED_0_", "foo");
                put("INDEXED_0__PROP", "bar");
                put("INDEXED_0__PROPS_0_", "0");
                put("INDEXED_0__PROPS_1_", "1");
            }
        };

        EnvConfigSource envConfigSource = new EnvConfigSource(env, 300);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(envConfigSource)
                .build();

        List<String> indexed = config.getValues("indexed", String.class, ArrayList::new);
        assertTrue(indexed.contains("foo"));
        assertEquals(1, indexed.size());
        assertTrue(config.getValues("indexed[0].props", String.class, ArrayList::new).contains("0"));
        assertTrue(config.getValues("indexed[0].props", String.class, ArrayList::new).contains("1"));
    }

    @Test
    void numbers() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new EnvConfigSource(Map.of("999_MY_VALUE", "foo", "_999_MY_VALUE", "bar"), 300))
                .build();

        assertEquals("foo", config.getRawValue("999.my.value"));
        assertEquals("foo", config.getRawValue("999_MY_VALUE"));
        assertEquals("bar", config.getRawValue("_999_MY_VALUE"));
        assertEquals("bar", config.getRawValue("%999.my.value"));
    }

    @Test
    void map() {
        Map<String, String> env = new HashMap<>() {
            {
                put("TEST_LANGUAGE__DE_ETR__", "Einfache Sprache");
                put("TEST_LANGUAGE_PT_BR", "FROM ENV");
            }
        };

        EnvConfigSource envConfigSource = new EnvConfigSource(env, 300);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(config("test.language.pt-br", "value"))
                .withSources(envConfigSource)
                .build();

        assertEquals("Einfache Sprache", config.getRawValue("test.language.\"de.etr\""));

        Map<String, String> map = config.getValues("test.language", STRING_CONVERTER, STRING_CONVERTER);
        assertEquals(map.get("de.etr"), "Einfache Sprache");
        assertEquals(map.get("pt-br"), "FROM ENV");
    }

    @Test
    void envEquals() {
        assertTrue(EnvName.equals("", new String("")));
        assertTrue(EnvName.equals(" ", new String(" ")));
        assertFalse(EnvName.equals(" ", new String("foo.bar")));
        assertFalse(EnvName.equals(" ", new String("FOO_BAR")));
        assertFalse(EnvName.equals("foo.bar", new String("")));
        assertFalse(EnvName.equals("FOO_BAR", new String("")));

        assertFalse(EnvName.equals("BAR", new String("foo.bar")));
        assertFalse(EnvName.equals("foo.bar", new String("BAR")));

        assertTrue(envSourceEquals("FOO_BAR", new String("FOO_BAR")));
        assertTrue(envSourceEquals("FOO_BAR", new String("foo.bar")));
        assertTrue(envSourceEquals("FOO_BAR", new String("FOO.BAR")));
        assertTrue(envSourceEquals("FOO_BAR", new String("foo-bar")));
        assertTrue(envSourceEquals("FOO_BAR", new String("foo_bar")));

        assertTrue(EnvName.equals("foo.bar", new String("foo.bar")));
        assertTrue(EnvName.equals("foo-bar", new String("foo-bar")));
        assertTrue(EnvName.equals("foo.bar", new String("FOO_BAR")));
        assertTrue(EnvName.equals("FOO.BAR", new String("FOO_BAR")));
        assertTrue(EnvName.equals("foo-bar", new String("FOO_BAR")));
        assertTrue(EnvName.equals("foo_bar", new String("FOO_BAR")));

        assertTrue(EnvName.equals("FOO__BAR__BAZ", new String("foo.\"bar\".baz")));
        assertTrue(EnvName.equals("foo.\"bar\".baz", new String("FOO__BAR__BAZ")));
        assertTrue(envSourceEquals("FOO__BAR__BAZ", new String("foo.\"bar\".baz")));
        assertTrue(EnvName.equals("FOO__BAR__BAZ_0__Z_0_", new String("foo.\"bar\".baz[0].z[0]")));
        assertTrue(envSourceEquals("FOO__BAR__BAZ_0__Z_0_", new String("foo.\"bar\".baz[0].z[0]")));

        assertTrue(EnvName.equals("_DEV_FOO_BAR", new String("%dev.foo.bar")));
        assertTrue(EnvName.equals("%dev.foo.bar", new String("_DEV_FOO_BAR")));
        assertTrue(envSourceEquals("_DEV_FOO_BAR", new String("%dev.foo.bar")));
        assertTrue(EnvName.equals("_ENV_SMALLRYE_MP_CONFIG_PROP", new String("_ENV_SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(EnvName.equals("%env.smallrye.mp.config.prop", new String("%env.smallrye.mp.config.prop")));
        assertTrue(EnvName.equals("_ENV_SMALLRYE_MP_CONFIG_PROP", new String("%env.smallrye.mp.config.prop")));
        assertTrue(EnvName.equals("%env.smallrye.mp.config.prop", new String("_ENV_SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("%env.smallrye.mp.config.prop", new String("%env.smallrye.mp.config.prop")));
        assertTrue(envSourceEquals("_ENV_SMALLRYE_MP_CONFIG_PROP", new String("%env.smallrye.mp.config.prop")));

        assertTrue(EnvName.equals("indexed[0]", new String("indexed[0]")));
        assertTrue(EnvName.equals("INDEXED_0_", new String("INDEXED_0_")));
        assertTrue(EnvName.equals("indexed[0]", new String("INDEXED_0_")));
        assertTrue(EnvName.equals("INDEXED_0_", new String("indexed[0]")));
        assertTrue(envSourceEquals("indexed[0]", new String("indexed[0]")));
        assertTrue(envSourceEquals("INDEXED_0_", new String("INDEXED_0_")));
        assertTrue(envSourceEquals("INDEXED_0_", new String("indexed[0]")));
        assertTrue(envSourceEquals("foo.bar.indexed[0]", new String("foo.bar.indexed[0]")));
        assertTrue(envSourceEquals("FOO_BAR_INDEXED_0_", new String("foo.bar.indexed[0]")));
        assertTrue(envSourceEquals("foo.bar[0].indexed[0]", new String("foo.bar[0].indexed[0]")));
        assertTrue(envSourceEquals("FOO_BAR_0__INDEXED_0_", new String("foo.bar[0].indexed[0]")));

        assertTrue(EnvName.equals("env.\"quoted.key\".value", new String("env.\"quoted.key\".value")));
        assertTrue(EnvName.equals("ENV__QUOTED_KEY__VALUE", new String("ENV__QUOTED_KEY__VALUE")));
        assertTrue(EnvName.equals("ENV__QUOTED_KEY__VALUE", new String("env.\"quoted.key\".value")));
        assertTrue(EnvName.equals("env.\"quoted.key\".value", new String("ENV__QUOTED_KEY__VALUE")));
        assertTrue(EnvName.equals("env.\"quoted.key\".value", new String("env.\"quoted-key\".value")));
        assertTrue(EnvName.equals("env.\"quoted-key\".value", new String("env.\"quoted.key\".value")));
        assertTrue(envSourceEquals("env.\"quoted.key\".value", new String("env.\"quoted.key\".value")));
        assertTrue(envSourceEquals("ENV__QUOTED_KEY__VALUE", new String("ENV__QUOTED_KEY__VALUE")));
        assertTrue(envSourceEquals("ENV__QUOTED_KEY__VALUE", new String("env.\"quoted.key\".value")));
        assertTrue(envSourceEquals("env.\"quoted.key\".value", new String("env.\"quoted-key\".value")));
        assertTrue(envSourceEquals("env.\"quoted-key\".value", new String("env.\"quoted.key\".value")));

        assertTrue(EnvName.equals("TEST_LANGUAGE__DE_ETR__", new String("test.language.\"de.etr\"")));
        assertTrue(EnvName.equals("test.language.\"de.etr\"", new String("TEST_LANGUAGE__DE_ETR__")));
        assertEquals(new EnvName("TEST_LANGUAGE__DE_ETR_").hashCode(), new EnvName("test.language.\"de.etr\"").hashCode());

        assertTrue(envSourceEquals("SMALLRYE_MP_CONFIG_PROP", new String("smallrye/mp/config/prop")));
        assertTrue(envSourceEquals("__SMALLRYE", new String("$$smallrye")));
        assertTrue(envSourceEquals("$$smallrye", new String("__SMALLRYE")));

        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("$$SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("&&SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("$$SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("$$SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE$MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE$MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("&&SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("&&SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("##SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("##SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("!!SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("!!SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("++SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("++SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("__SMALLRYE_MP_CONFIG_PROP", new String("??SMALLRYE_MP_CONFIG_PROP")));
        assertTrue(envSourceEquals("??SMALLRYE_MP_CONFIG_PROP", new String("__SMALLRYE_MP_CONFIG_PROP")));
    }

    @Test
    void sameSemanticMeaning() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "foo.bar-baz", "fromOther",
                        "%dev.foo.bar-devbaz", "fromOther",
                        "foo.bar-commonbaz", "fromOther"))
                .withSources(new EnvConfigSource(Map.of(
                        "FOO_BAR_BAZ", "fromEnv",
                        "FOO_BAR_DEVBAZ", "fromEnv",
                        "_COMMON_FOO_BAR_COMMONBAZ", "fromEnv"), 300))
                .withProfiles(List.of("dev", "common"))
                .build();

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains("FOO_BAR_BAZ"));
        assertTrue(properties.contains("foo.bar-baz"));
        assertFalse(properties.contains("foo.bar.baz"));
        assertEquals("fromEnv", config.getRawValue("foo.bar-baz"));

        assertTrue(properties.contains("FOO_BAR_DEVBAZ"));
        assertTrue(properties.contains("foo.bar-devbaz"));
        assertFalse(properties.contains("foo.bar.devbaz"));
        assertEquals("fromEnv", config.getRawValue("foo.bar-devbaz"));

        assertTrue(properties.contains("foo.bar-commonbaz"));
        assertEquals("fromEnv", config.getRawValue("foo.bar-commonbaz"));
    }

    @Test
    void sameNames() {
        assertTrue(envSourceEquals("FOOBAR", "foobar"));
        assertTrue(envSourceEquals("FOOBAR", "fooBar"));

        EnvConfigSource envConfigSource = new EnvConfigSource(
                Map.of("my_string_property", "lower", "MY_STRING_PROPERTY", "upper"), 100);
        assertEquals(2, envConfigSource.getProperties().size());
        assertEquals("lower", envConfigSource.getValue("my_string_property"));
        assertEquals("upper", envConfigSource.getValue("MY_STRING_PROPERTY"));
    }

    @Test
    void dashedEnvNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DashedEnvNames.class)
                .withSources(new EnvConfigSource(Map.of(
                        "DASHED_ENV_NAMES_VALUE", "value",
                        "DASHED_ENV_NAMES_NESTED__DASHED_KEY__ANOTHER", "value"), 100))
                .build();

        DashedEnvNames mapping = config.getConfigMapping(DashedEnvNames.class);

        assertEquals("value", mapping.value());
        // Unfortunately, we still don't have a good way to determine if the Map key is dashed or not
        assertEquals("value", mapping.nested().get("dashed.key").another());
    }

    @Test
    void dottedDashedEnvNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DashedEnvNames.class)
                .withSources(new EnvConfigSource(emptyMap(), 300))
                .withSources(new EnvConfigSource(Map.of(
                        "DASHED_ENV_NAMES_VALUE", "value",
                        "dashed-env-names.nested.dashed-key.another", "value"), 100))
                .build();

        DashedEnvNames mapping = config.getConfigMapping(DashedEnvNames.class);

        assertEquals("value", mapping.value());
        assertEquals("value", mapping.nested().get("dashed-key").another());

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains("dashed-env-names.value"));
        assertTrue(properties.contains("DASHED_ENV_NAMES_VALUE"));
        assertFalse(properties.contains("dashed.env.names.value"));
        assertTrue(properties.contains("dashed-env-names.nested.dashed-key.another"));

        config = new SmallRyeConfigBuilder()
                .withMapping(DashedEnvNames.class)
                .withSources(new EnvConfigSource(emptyMap(), 300))
                .withSources(new EnvConfigSource(Map.of(
                        "%DEV_DASHED_ENV_NAMES_VALUE", "value"), 100))
                .withProfile("dev")
                .build();

        properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains("dashed-env-names.value"));
        assertTrue(properties.contains("%DEV_DASHED_ENV_NAMES_VALUE"));
        assertFalse(properties.contains("dashed.env.names.value"));
    }

    @ConfigMapping(prefix = "dashed-env-names")
    interface DashedEnvNames {
        String value();

        Map<String, Nested> nested();

        interface Nested {
            String another();
        }
    }

    @Test
    void dashedEnvNamesWithEmpty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of("DASHED_IGNORED_ERRORS_0_", "error0"), 300))
                .withMapping(DashedEnvNamesWithEmpty.class)
                .build();

        DashedEnvNamesWithEmpty mapping = config.getConfigMapping(DashedEnvNamesWithEmpty.class);

        assertTrue(mapping.ignoredErrors().isPresent());
        mapping.ignoredErrors().ifPresent(ignoredErrors -> assertIterableEquals(List.of("error0"), ignoredErrors));
    }

    @ConfigMapping(prefix = "dashed")
    interface DashedEnvNamesWithEmpty {
        @WithParentName
        Optional<Boolean> enable();

        Optional<List<String>> ignoredErrors();
    }

    @Test
    void ignoreUnmappedWithMappingIgnore() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMappingIgnore("ignore.**")
                .withMapping(IgnoreUnmappedWithMappingIgnore.class)
                .withSources(new EnvConfigSource(Map.of(
                        "IGNORE_VALUE", "value",
                        "IGNORE_LIST_0_", "0",
                        "IGNORE_NESTED_VALUE", "nested",
                        "IGNORE_IGNORE", "ignore",
                        "IGNORE_NESTED_IGNORE", "ignore"), 100))
                .build();

        IgnoreUnmappedWithMappingIgnore mapping = config.getConfigMapping(IgnoreUnmappedWithMappingIgnore.class);

        assertEquals("value", mapping.value());
        assertEquals(0, mapping.list().get(0));
        assertEquals("nested", mapping.nested().value());
    }

    @ConfigMapping(prefix = "ignore")
    interface IgnoreUnmappedWithMappingIgnore {
        String value();

        List<Integer> list();

        Nested nested();

        interface Nested {
            String value();
        }
    }

    @Test
    void unmappedProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(UnmappedProfile.class)
                .withSources(new EnvConfigSource(Map.of(
                        "_DEV_UNMAPPED_A_VALUE", "value",
                        "_DEV_UNMAPPED_NESTED_TYPE_VALUE", "value"), 100))
                .withProfile("dev")
                .build();

        UnmappedProfile mapping = config.getConfigMapping(UnmappedProfile.class);
        assertTrue(mapping.aValue().isPresent());
        assertEquals("value", mapping.aValue().get());
        assertEquals("value", mapping.nestedType().value());

        config = new SmallRyeConfigBuilder()
                .withMapping(UnmappedProfile.class)
                .withSources(new EnvConfigSource(Map.of(
                        "_DEV_UNMAPPED_A_VALUE", "value",
                        "_DEV_UNMAPPED_NESTED_TYPE_VALUE", "value"), 200))
                .withSources(config(
                        "%dev.unmapped.nested-type.value", "value"))
                .withSources(config(
                        "unmapped.a.value", "value",
                        "unmapped.nested.type.value", "value"))
                .withProfile("dev")
                .build();

        mapping = config.getConfigMapping(UnmappedProfile.class);
        assertTrue(mapping.aValue().isPresent());
        assertEquals("value", mapping.aValue().get());
        assertEquals("value", mapping.nestedType().value());

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(UnmappedProfile.class)
                .withSources(new EnvConfigSource(Map.of(
                        "_DEV_UNMAPPED_A_VALUE", "value",
                        "_DEV_UNMAPPED_NESTED_TYPE_VALUE", "value",
                        "_DEV_UNMAPPED_UNMAPPED", "value"), 200))
                .withSources(config(
                        "%dev.unmapped.nested-type.value", "value"))
                .withSources(config(
                        "unmapped.a.value", "value",
                        "unmapped.nested.type.value", "value"))
                .withProfile("dev");

        // TODO - https://github.com/quarkusio/quarkus/issues/38479
        // ConfigValidationException exception = assertThrows(ConfigValidationException.class, builder::build);
        // assertEquals("SRCFG00050: unmapped.unmapped in EnvConfigSource does not map to any root",
        //         exception.getProblem(0).getMessage());
    }

    @ConfigMapping(prefix = "unmapped")
    interface UnmappedProfile {
        Optional<String> aValue();

        Nested nestedType();

        interface Nested {
            String value();
        }
    }

    @Test
    void ignoreUnmappedWithMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(IgnoreUnmappedWithMap.class)
                .withSources(new EnvConfigSource(Map.of(
                        "IGNORE_VALUE", "value",
                        "IGNORE_LIST_0_", "0",
                        "IGNORE_NESTED_VALUE", "nested",
                        "IGNORE_IGNORE", "ignore",
                        "IGNORE_NESTED_IGNORE", "ignore"), 100))
                .build();

        IgnoreUnmappedWithMap mapping = config.getConfigMapping(IgnoreUnmappedWithMap.class);

        assertEquals("value", mapping.value());
        assertEquals(0, mapping.list().get(0));
        assertEquals("nested", mapping.nested().value());
    }

    @ConfigMapping(prefix = "ignore")
    interface IgnoreUnmappedWithMap {
        String value();

        List<Integer> list();

        Nested nested();

        @WithParentName
        Map<String, String> ignore();

        interface Nested {
            String value();
        }
    }

    @Test
    void mappingFactory() {
        ConfigurableConfigSourceFactory<MappingFactory> sourceFactory = new ConfigurableConfigSourceFactory<>() {
            @Override
            public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context, final MappingFactory mapping) {
                assertEquals("value", mapping.aValue());
                assertEquals("value", mapping.aMap().get("key"));
                return emptyList();
            }
        };

        new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of(
                        "MAPPING_A_VALUE", "value",
                        "MAPPING_A_MAP_KEY", "value"), 300))
                .withSources(sourceFactory)
                .build();
    }

    @ConfigMapping(prefix = "mapping")
    interface MappingFactory {
        String aValue();

        Map<String, String> aMap();
    }

    @Test
    void propertyNamesCache() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("mp.messaging.incoming.words-in.topic", "from-properties"))
                .withSources(new EnvConfigSource(Map.of("MP_MESSAGING_INCOMING_WORDS_IN_TOPIC", "from-env"), 300))
                .withMapping(PropertyNamesCache.class)
                .build();

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertEquals(2, properties.size());
        assertTrue(properties.contains("mp.messaging.incoming.words-in.topic"));
        assertTrue(properties.contains("MP_MESSAGING_INCOMING_WORDS_IN_TOPIC"));
        assertFalse(properties.contains("mp.messaging.incoming.words.in.topic"));
    }

    @ConfigMapping
    interface PropertyNamesCache {
        Map<String, String> values();
    }

    @Test
    void mappingMapsWithEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "services.service.service-discovery.type", "from-properties",
                        "services.service.service-discovery.address-list", "",
                        "services.\"quoted\".service-discovery.type", "from-properties",
                        "services.\"quoted\".service-discovery.address-list", "",
                        "services.\"dashed-key\".service-discovery.type", "from-properties",
                        "services.\"dashed-key\".service-discovery.address-list", ""))
                .withSources(new EnvConfigSource(Map.of(
                        "SERVICES_SERVICE_SERVICE_DISCOVERY_ADDRESS_LIST", "from-env",
                        "SERVICES__QUOTED__SERVICE_DISCOVERY_ADDRESS_LIST", "from-env",
                        "SERVICES__DASHED_KEY__SERVICE_DISCOVERY_ADDRESS_LIST", "from-env"), 300))
                .withMapping(Services.class)
                .build();

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains("services.service.service-discovery.type"));
        assertTrue(properties.contains("services.service.service-discovery.address-list"));
        assertTrue(properties.contains("SERVICES_SERVICE_SERVICE_DISCOVERY_ADDRESS_LIST"));
        assertTrue(properties.contains("services.\"quoted\".service-discovery.type"));
        assertTrue(properties.contains("SERVICES__QUOTED__SERVICE_DISCOVERY_ADDRESS_LIST"));
        assertTrue(properties.contains("services.\"quoted\".service-discovery.address-list"));

        Services mapping = config.getConfigMapping(Services.class);
        assertEquals("from-properties", mapping.serviceConfiguration().get("service").serviceDiscovery().type());
        assertEquals("from-env", mapping.serviceConfiguration().get("service").serviceDiscovery().params().get("address-list"));
        assertEquals("from-properties", mapping.serviceConfiguration().get("quoted").serviceDiscovery().type());
        assertEquals("from-env", mapping.serviceConfiguration().get("quoted").serviceDiscovery().params().get("address-list"));
        assertEquals("from-properties", mapping.serviceConfiguration().get("dashed-key").serviceDiscovery().type());
        assertEquals("from-env",
                mapping.serviceConfiguration().get("dashed-key").serviceDiscovery().params().get("address-list"));
    }

    @ConfigMapping(prefix = "services")
    interface Services {
        @WithParentName
        Map<String, ServiceConfiguration> serviceConfiguration();

        interface ServiceConfiguration {
            ServiceDiscoveryConfiguration serviceDiscovery();

            interface ServiceDiscoveryConfiguration {
                String type();

                @WithParentName
                Map<String, String> params();
            }
        }
    }

    @Test
    void mappingMapsWithEnvMultiplePrefixes() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of(
                        "PREFIX_COMPOSED_SERVICE_SERVICE_DISCOVERY_ADDRESS", "from-env"), 300))
                .withMapping(SimplePrefix.class)
                .withMapping(ComposedPrefix.class)
                .build();

        ComposedPrefix mapping = config.getConfigMapping(ComposedPrefix.class);
        assertEquals("from-env", mapping.serviceConfiguration().get("service").serviceDiscovery().params().get("address"));
    }

    @ConfigMapping(prefix = "prefix")
    interface SimplePrefix {
        Optional<String> value();
    }

    @ConfigMapping(prefix = "prefix.composed")
    interface ComposedPrefix {
        @WithParentName
        Map<String, ServiceConfiguration> serviceConfiguration();

        interface ServiceConfiguration {
            ServiceDiscoveryConfiguration serviceDiscovery();

            interface ServiceDiscoveryConfiguration {
                @WithParentName
                Map<String, String> params();
            }
        }
    }

    @Test
    void mappingsMapsWithEnvSplit() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of(
                        "SERVICES_SERVICE_SERVICE_DISCOVERY_ADDRESS", "from-env",
                        "SERVICES_SERVICE_ANOTHER_DISCOVERY_ADDRESS", "from-env"), 300))
                .withMapping(ServicesOne.class)
                .withMapping(ServicesTwo.class)
                .build();

        ServicesOne servicesOne = config.getConfigMapping(ServicesOne.class);
        ServicesTwo servicesTwo = config.getConfigMapping(ServicesTwo.class);

        assertEquals("from-env", servicesOne.serviceConfiguration().get("service").serviceDiscovery().params().get("address"));
        assertEquals("from-env", servicesTwo.serviceConfiguration().get("service").anotherDiscovery().params().get("address"));
    }

    @ConfigMapping(prefix = "services")
    interface ServicesOne {
        @WithParentName
        Map<String, ServiceConfiguration> serviceConfiguration();

        interface ServiceConfiguration {
            ServiceDiscoveryConfiguration serviceDiscovery();

            interface ServiceDiscoveryConfiguration {
                @WithParentName
                Map<String, String> params();
            }
        }
    }

    @ConfigMapping(prefix = "services")
    interface ServicesTwo {
        @WithParentName
        Map<String, ServiceConfiguration> serviceConfiguration();

        interface ServiceConfiguration {
            ServiceDiscoveryConfiguration anotherDiscovery();

            interface ServiceDiscoveryConfiguration {
                @WithParentName
                Map<String, String> params();
            }
        }
    }

    @Test
    void prefixAsEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of("PREFIX", "VALUE", "PREFIX_VALUE", "VALUE"), 300))
                .withMapping(PrefixAsEnv.class)
                .build();
    }

    @ConfigMapping(prefix = "prefix")
    interface PrefixAsEnv {
        Optional<String> value();
    }

    @Test
    void clashMapKeysWithNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of("MAP_CLIENT_ID", "VALUE"), 300))
                .withMapping(ClashMapKeysWithNames.class)
                .build();

        ClashMapKeysWithNames mapping = config.getConfigMapping(ClashMapKeysWithNames.class);
        assertTrue(mapping.clashes().get(null).clientId().isPresent());

        EnvConfigSource envConfigSource = new EnvConfigSource(Map.of("MAP_CLIENT_ID", "VALUE"), 300);
        envConfigSource.matchEnvWithProperties(List.of(Map.entry("", new Supplier<Iterator<String>>() {
            @Override
            public Iterator<String> get() {
                return List.of("map.*.id", "map.client-id").iterator();
            }
        })), List.of());
        assertTrue(envConfigSource.getPropertyNames().contains("map.client-id"));

        envConfigSource = new EnvConfigSource(Map.of("MAP_CLIENT_ID", "VALUE"), 300);
        envConfigSource.matchEnvWithProperties(List.of(Map.entry("", new Supplier<Iterator<String>>() {
            @Override
            public Iterator<String> get() {
                return List.of("map.client-id", "map.*.id").iterator();
            }
        })), List.of());
        assertTrue(envConfigSource.getPropertyNames().contains("map.client-id"));
    }

    @ConfigMapping(prefix = "map")
    interface ClashMapKeysWithNames {
        @WithParentName
        @WithUnnamedKey
        @WithDefaults
        Map<String, Clash> clashes();

        interface Clash {
            Optional<String> id();

            Optional<String> clientId();
        }
    }

    @Test
    void mapNumericKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of("MAP_3", "100"), 300))
                .withMapping(MapNumericKeys.class)
                .build();

        MapNumericKeys mapping = config.getConfigMapping(MapNumericKeys.class);
        assertEquals(100, mapping.map().get("3"));
    }

    @ConfigMapping(prefix = "map")
    public interface MapNumericKeys {
        @WithParentName
        Map<String, Integer> map();
    }

    @Test
    void upperCaseKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of(
                        "UPPERCASE_KEYS_MAP_FOO_0__LIST", "one,two",
                        "UPPERCASE_KEYS_MAP_DASHED_FOO_0__LIST", "one,two"), 300))
                .withSources(config(
                        "uppercase.keys.map.FOO[0].value", "value",
                        "uppercase.keys.map.FOO[0].list", "one",
                        "uppercase.keys.map-dashed.FOO[0].value", "value",
                        "uppercase.keys.map-dashed.FOO[0].list", "one"))
                .withMapping(UpperCaseKeys.class)
                .build();

        UpperCaseKeys mapping = config.getConfigMapping(UpperCaseKeys.class);
        assertIterableEquals(List.of("one", "two"), mapping.map().get("FOO").get(0).list());
        assertIterableEquals(List.of("one", "two"), mapping.mapDashed().get("FOO").get(0).list());
    }

    @ConfigMapping(prefix = "uppercase.keys")
    interface UpperCaseKeys {
        Map<String, List<Nested>> map();

        Map<String, List<Nested>> mapDashed();

        interface Nested {
            String value();

            List<String> list();
        }
    }

    @Test
    void matchEnvVarWithFactory() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new MatchEnvVarConfigSourceFactory())
                .withSources(new EnvConfigSource(Map.of(
                        "MAP_KEY", "value"), 300))
                .withMapping(MatchEnvVarWithFactory.class)
                .build();

        MatchEnvVarWithFactory mapping = config.getConfigMapping(MatchEnvVarWithFactory.class);

        assertEquals("value", mapping.map().get("key"));
    }

    @ConfigMapping(prefix = "")
    interface MatchEnvVarWithFactory {
        Map<String, String> map();
    }

    static class MatchEnvVarConfigSourceFactory implements ConfigurableConfigSourceFactory<MatchEnvVarWithFactory> {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context, final MatchEnvVarWithFactory config) {
            return Collections.emptyList();
        }
    }

    private static boolean envSourceEquals(String name, String lookup) {
        return BOOLEAN_CONVERTER.convert(new EnvConfigSource(Map.of(name, "true"), 100).getValue(lookup));
    }
}
