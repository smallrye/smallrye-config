package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.common.AbstractConfigSource;
import io.smallrye.config.common.MapBackedConfigSource;

class SmallRyeConfigTest {
    @Test
    void getValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        List<Integer> values = config.getValues("my.list", Integer.class, ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    void getValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        List<Integer> values = config.getValues("my.list", config.getConverter(Integer.class).get(),
                (IntFunction<List<Integer>>) value -> new ArrayList<>());
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    void getOptionalValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", Integer.class, ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    void getOptionalValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.list", "1,2,3,4")).build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", config.getConverter(Integer.class).get(),
                (IntFunction<List<Integer>>) value -> new ArrayList<>());
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    void convert() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();

        assertEquals(1234, config.convert("1234", Integer.class).intValue());
        assertNull(config.convert(null, Integer.class));
    }

    @Test
    void configValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.prop", "1234")).build();

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals("1234", config.getValue("my.prop", ConfigValue.class).getValue());
        assertEquals("1234", config.getOptionalValue("my.prop", ConfigValue.class).get().getValue());
    }

    @Test
    void profiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withProfile("profile").build();
        assertEquals("profile", config.getProfiles().get(0));
    }

    @Test
    void unwrap() {
        Config config = new SmallRyeConfigBuilder().build();
        SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);

        assertNotNull(smallRyeConfig);
        assertThrows(IllegalArgumentException.class, () -> config.unwrap(Object.class));
    }

    @Test
    void getValueArray() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("array", "one,two,three"))
                .build();

        String[] strings = config.getValue("array", String[].class);
        assertEquals(3, strings.length);
        assertArrayEquals(new String[] { "one", "two", "three" }, strings);

        config = new SmallRyeConfigBuilder()
                .withSources(config("array[0]", "one", "array[1]", "two", "array[2]", "three"))
                .build();
        strings = config.getValue("array", String[].class);
        assertEquals(3, strings.length);
        assertArrayEquals(new String[] { "one", "two", "three" }, strings);
    }

    @Test
    void indexedNegativeOrdinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("config_ordinal", "-1000", "indexed[0]", "one", "indexed[1]", "two", "indexed[2]", "three"))
                .build();

        List<String> indexed = config.getValues("indexed", String.class);
        assertEquals(3, indexed.size());
        assertEquals("one", indexed.get(0));
        assertEquals("two", indexed.get(1));
        assertEquals("three", indexed.get(2));

        Optional<List<String>> indexedOptional = config.getOptionalValues("indexed", String.class);
        assertTrue(indexedOptional.isPresent());
        indexedOptional.ifPresent(new Consumer<List<String>>() {
            @Override
            public void accept(final List<String> indexed) {
                assertEquals(3, indexed.size());
                assertEquals("one", indexed.get(0));
                assertEquals("two", indexed.get(1));
                assertEquals("three", indexed.get(2));
            }
        });

        String[] array = config.getValue("indexed", String[].class);
        assertEquals(3, array.length);
        assertEquals("one", array[0]);
        assertEquals("two", array[1]);
        assertEquals("three", array[2]);

        Optional<String[]> arrayOptional = config.getOptionalValue("indexed", String[].class);
        assertTrue(arrayOptional.isPresent());
        arrayOptional.ifPresent(new Consumer<String[]>() {
            @Override
            public void accept(final String[] array) {
                assertEquals(3, array.length);
                assertEquals("one", array[0]);
                assertEquals("two", array[1]);
                assertEquals("three", array[2]);
            }
        });
    }

    @Test
    void getIndexedValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getIndexedValues("server.environments", config.requireConverter(String.class),
                ArrayList::new);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("dev", config.getValue("server.environments[0]", String.class));
        assertEquals("qa", environments.get(1));
        assertEquals("qa", config.getValue("server.environments[1]", String.class));
        assertEquals("prod", environments.get(2));
        assertEquals("prod", config.getValue("server.environments[2]", String.class));

        assertThrows(NoSuchElementException.class,
                () -> config.getIndexedValues("not.found", config.requireConverter(String.class), ArrayList::new));
    }

    @Test
    void indexedOrder() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes);

        Map<String, String> properties = new HashMap<>();
        for (Integer index : indexes) {
            properties.put("prop[" + index + "]", index.toString());
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, ""))
                .build();

        List<Integer> values = config.getValues("prop", Integer.class, ArrayList::new);
        for (int i = 0; i < 100; i++) {
            assertEquals(i, values.get(i));
        }
    }

    @Test
    void getValuesNotIndexed() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "server.environments", "dev,qa"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(2, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
    }

    @Test
    void getValuesIndexedPriority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "server.environments", "dev,qa",
                        "server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
        assertEquals("prod", environments.get(2));
    }

    @Test
    void getValuesNotFound() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments", ""))
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getValues("server.environments", String.class));

        SmallRyeConfig configIndexed = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", ""))
                .build();

        assertThrows(NoSuchElementException.class, () -> configIndexed.getValues("server.environments", String.class));
    }

    @Test
    void getOptionalValuesIndexed() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        Optional<List<String>> environments = config.getOptionalValues("server.environments", String.class);
        assertTrue(environments.isPresent());
        assertEquals(3, environments.get().size());
        assertEquals("dev", environments.get().get(0));
        assertEquals("qa", environments.get().get(1));
        assertEquals("prod", environments.get().get(2));
    }

    @Test
    void getOptionalValuesNotIndexed() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments", "dev,qa"))
                .build();

        Optional<List<String>> environments = config.getOptionalValues("server.environments", String.class);
        assertTrue(environments.isPresent());
        assertEquals(2, environments.get().size());
        assertEquals("dev", environments.get().get(0));
        assertEquals("qa", environments.get().get(1));
    }

    @Test
    void getOptionalValuesEmpty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments", ""))
                .build();

        assertFalse(
                config.getIndexedOptionalValues("server.environments", config.requireConverter(String.class), ArrayList::new)
                        .isPresent());
        assertFalse(config.getOptionalValues("server.environments", String.class).isPresent());

        SmallRyeConfig configIndexed = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", ""))
                .build();

        assertFalse(configIndexed
                .getIndexedOptionalValues("server.environments", config.requireConverter(String.class), ArrayList::new)
                .isPresent());
        assertFalse(configIndexed.getOptionalValues("server.environments", String.class).isPresent());
    }

    @Test
    void invalidIndexes() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "dev", "",
                        "dev[x", "",
                        "qa", "",
                        "qa[[1]]", "",
                        "prod", "",
                        "prod[x]", "",
                        "perf", "",
                        "perf[]", ""))
                .build();

        assertTrue(config.getIndexedPropertiesIndexes("dev").isEmpty());
        assertTrue(config.getIndexedPropertiesIndexes("qa").isEmpty());
        assertTrue(config.getIndexedPropertiesIndexes("prod").isEmpty());
        assertTrue(config.getIndexedPropertiesIndexes("perf").isEmpty());
    }

    @Test
    void nestedIndexes() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "map.roles.admin[0].name", "",
                        "map.roles.admin[1].name", "",
                        "map.roles.admin[0].address", ""))
                .build();

        List<Integer> indexes = config.getIndexedPropertiesIndexes("map.roles.admin");
        assertEquals(2, indexes.size());
        assertTrue(indexes.contains(0));
        assertTrue(indexes.contains(1));
    }

    @Test
    void quotedIndexes() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("map.roles.\"quoted.key\"[0].name", ""))
                .build();

        List<Integer> indexes = config.getIndexedPropertiesIndexes("map.roles.\"quoted.key\"");
        assertEquals(1, indexes.size());
        assertTrue(indexes.contains(0));
    }

    @Test
    void overrideIndexedValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "config_ordinal", "100",
                        "server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .withSources(config(
                        "config_ordinal", "1000",
                        "server.environments[2]", "prd",
                        "server.environments[3]", "perf"))
                .build();

        List<String> values = config.getValues("server.environments", String.class);
        assertEquals("dev", values.get(0));
        assertEquals("qa", values.get(1));
        assertEquals("prd", values.get(2));
        assertEquals("perf", values.get(3));
    }

    @Test
    void isPropertyPresent() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234", "my.expansion", "${not.available}", "empty", ""))
                .withSources(new MapBackedConfigSource("hidder", Map.of("my.hidden", "hidden")) {
                    @Override
                    public Set<String> getPropertyNames() {
                        return Collections.emptySet();
                    }
                })
                .build();

        assertTrue(config.isPropertyPresent("my.prop"));
        assertTrue(config.isPropertyPresent("my.expansion"));
        assertFalse(config.isPropertyPresent("not.available"));
        assertTrue(config.isPropertyPresent("my.hidden"));
        assertFalse(config.isPropertyPresent("empty"));

        Set<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertEquals(3, names.size());
        assertTrue(names.contains("my.prop"));
        assertTrue(names.contains("my.expansion"));
    }

    @Test
    void getPropertyNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultInterceptors().addDefaultSources().build();
        assertEquals("1234", config.getRawValue("SMALLRYE_MP_CONFIG_PROP"));
        assertEquals("1234", config.getRawValue("smallrye.mp.config.prop"));
        assertEquals("1234", config.getRawValue("smallrye.mp-config.prop"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("SMALLRYE_MP_CONFIG_PROP"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("smallrye.mp.config.prop"));

        config = new SmallRyeConfigBuilder().addDefaultInterceptors().addDefaultSources()
                .withSources(KeyValuesConfigSource.config("smallrye.mp-config.prop", "5678")).build();
        assertEquals("1234", config.getRawValue("SMALLRYE_MP_CONFIG_PROP"));
        assertEquals("1234", config.getRawValue("smallrye.mp-config.prop"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("SMALLRYE_MP_CONFIG_PROP"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("smallrye.mp-config.prop"));
    }

    @Test
    void getConfigSource() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config()).build();

        assertFalse(config.getConfigSource("something").isPresent());
        assertTrue(config.getConfigSource("KeyValuesConfigSource").isPresent());
        assertFalse(config.getConfigSource(null).isPresent());

        config = new SmallRyeConfigBuilder().withSources(new AbstractConfigSource(null, 100) {
            @Override
            public Set<String> getPropertyNames() {
                return null;
            }

            @Override
            public String getValue(final String propertyName) {
                return null;
            }
        }).build();
        assertFalse(config.getConfigSource("something").isPresent());
        assertFalse(config.getConfigSource(null).isPresent());
    }

    @Test
    void builderSourcesProvider() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(classLoader -> singletonList(config("my.prop", "1234")))
                .build();
        assertEquals("1234", config.getRawValue("my.prop"));
    }

    @Test
    void builderWithFlagSetters() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .setAddDefaultSources(true)
                .setAddDefaultInterceptors(true)
                .setAddDiscoveredSources(true)
                .setAddDiscoveredConverters(true)
                .setAddDiscoveredInterceptors(true)
                .setAddDiscoveredValidator(true)
                .withSources(config("my.prop", "${replacement}"))
                .withSources(config("replacement", "1234"))
                .build();

        assertTrue(config.getConfigSource("EnvConfigSource").isPresent());
        assertEquals("1234", config.getRawValue("my.prop"));
    }

    @Test
    void getValuesMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "my.prop.key", "value",
                        "my.prop.key.nested", "value",
                        "my.prop.\"key.quoted\"", "value",
                        "my.prop.key.indexed[0]", "value"))
                .build();

        Map<String, String> map = config.getValues("my.prop", String.class, String.class);
        assertEquals(4, map.size());
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("key.nested"));
        assertEquals("value", map.get("key.quoted"));
        assertEquals("value", map.get("key.indexed[0]"));

        Converter<String> stringConverter = config.requireConverter(String.class);
        Map<String, String> treeMap = config.getValues("my.prop", stringConverter, stringConverter, t -> new TreeMap<>());
        assertInstanceOf(TreeMap.class, treeMap);

        Optional<Map<String, String>> optionalMap = config.getOptionalValues("my.prop", String.class, String.class);
        assertTrue(optionalMap.isPresent());
        assertEquals(4, optionalMap.get().size());
        assertEquals("value", optionalMap.get().get("key"));
        assertEquals("value", optionalMap.get().get("key.nested"));
        assertEquals("value", optionalMap.get().get("key.quoted"));
        assertEquals("value", optionalMap.get().get("key.indexed[0]"));

        Optional<Map<String, String>> optionalTreeMap = config.getOptionalValues("my.prop", stringConverter, stringConverter,
                t -> new TreeMap<>());
        assertTrue(optionalTreeMap.isPresent());
        assertInstanceOf(TreeMap.class, optionalTreeMap.get());

        assertTrue(config.getOptionalValues("my.optional", String.class, String.class).isEmpty());
    }

    @Test
    void getValuesMapInline() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "key=value;key.nested=value;\"key.quoted\"=value"))
                .build();

        Map<String, String> map = config.getValues("my.prop", String.class, String.class);
        assertEquals(3, map.size());
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("key.nested"));
        assertEquals("value", map.get("key.quoted"));

        Optional<Map<String, String>> optionalMap = config.getOptionalValues("my.prop", String.class, String.class);
        assertTrue(optionalMap.isPresent());
        assertEquals(3, optionalMap.get().size());
        assertEquals("value", optionalMap.get().get("key"));
        assertEquals("value", optionalMap.get().get("key.nested"));
        assertEquals("value", optionalMap.get().get("key.quoted"));
    }

    @Test
    void getValuesMapList() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "my.prop.key[0]", "value",
                        "my.prop.key[1]", "value",
                        "my.prop.key.nested[0]", "value",
                        "my.prop.key.nested[1]", "value",
                        "my.prop.\"key.quoted\"[0]", "value",
                        "my.prop.\"key.quoted\"[1]", "value"))
                .build();

        Map<String, List<String>> map = config.getValues("my.prop", String.class, String.class, ArrayList::new);
        assertEquals(3, map.size());
        assertEquals("value", map.get("key").get(0));
        assertEquals("value", map.get("key").get(1));
        assertEquals("value", map.get("key.nested").get(0));
        assertEquals("value", map.get("key.nested").get(1));
        assertEquals("value", map.get("key.quoted").get(0));
        assertEquals("value", map.get("key.quoted").get(1));

        Converter<String> stringConverter = config.requireConverter(String.class);
        Map<String, List<String>> treeMap = config.getValues("my.prop", stringConverter, stringConverter, t -> new TreeMap<>(),
                ArrayList::new);
        assertInstanceOf(TreeMap.class, treeMap);

        Optional<Map<String, List<String>>> optionalMap = config.getOptionalValues("my.prop", String.class, String.class,
                ArrayList::new);
        assertTrue(optionalMap.isPresent());
        assertEquals(3, optionalMap.get().size());
        assertEquals("value", optionalMap.get().get("key").get(0));
        assertEquals("value", optionalMap.get().get("key").get(1));
        assertEquals("value", optionalMap.get().get("key.nested").get(0));
        assertEquals("value", optionalMap.get().get("key.nested").get(1));
        assertEquals("value", optionalMap.get().get("key.quoted").get(0));
        assertEquals("value", optionalMap.get().get("key.quoted").get(1));

        Optional<Map<String, List<String>>> optionalTreeMap = config.getOptionalValues("my.prop", stringConverter,
                stringConverter, t -> new TreeMap<>(), ArrayList::new);
        assertTrue(optionalTreeMap.isPresent());
        assertInstanceOf(TreeMap.class, optionalTreeMap.get());

        assertTrue(config.getOptionalValues("my.optional", String.class, String.class, ArrayList::new).isEmpty());
    }

    @Test
    void getValuesMapListInline() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "key=value,value;key.nested=value,value;\"key.quoted\"=value,value"))
                .build();

        Map<String, List<String>> map = config.getValues("my.prop", String.class, String.class, ArrayList::new);
        assertEquals(3, map.size());
        assertEquals("value", map.get("key").get(0));
        assertEquals("value", map.get("key").get(1));
        assertEquals("value", map.get("key.nested").get(0));
        assertEquals("value", map.get("key.nested").get(1));
        assertEquals("value", map.get("key.quoted").get(0));
        assertEquals("value", map.get("key.quoted").get(1));

        Optional<Map<String, List<String>>> optionalMap = config.getOptionalValues("my.prop", String.class, String.class,
                ArrayList::new);
        assertTrue(optionalMap.isPresent());
        assertEquals(3, optionalMap.get().size());
        assertEquals("value", optionalMap.get().get("key").get(0));
        assertEquals("value", optionalMap.get().get("key").get(1));
        assertEquals("value", optionalMap.get().get("key.nested").get(0));
        assertEquals("value", optionalMap.get().get("key.nested").get(1));
        assertEquals("value", optionalMap.get().get("key.quoted").get(0));
        assertEquals("value", optionalMap.get().get("key.quoted").get(1));
    }

    @Test
    void getValuesMapIntegers() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "my", "nothing",
                        "my.prop", "nothing",
                        "my.prop.1", "1",
                        "my.prop.2", "2",
                        "my.prop.3", "3"))
                .build();

        Map<Integer, Integer> map = config.getValues("my.prop", Integer.class, Integer.class);
        assertEquals(3, map.size());
        assertEquals(1, map.get(1));
        assertEquals(2, map.get(2));
        assertEquals(3, map.get(3));
    }

    @Test
    void getValuesMapEmpty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        assertThrows(NoSuchElementException.class, () -> config.getValues("my.prop", String.class, String.class));
        assertThrows(NoSuchElementException.class,
                () -> config.getValues("my.prop", String.class, String.class, ArrayList::new));
    }

    @Test
    void quotedKeysInEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new EnvConfigSource(singletonMap("ENV__QUOTED_KEY__VALUE", "env"), 300))
                .withSources(config("env.\"quoted-key\".value", "default"))
                .build();

        assertEquals("env", config.getRawValue("env.\"quoted-key\".value"));

        ConfigSource keymap = config.getConfigSource("KeyValuesConfigSource").get();
        assertEquals("default", keymap.getValue("env.\"quoted-key\".value"));
    }

    @Test
    void emptyPropertyNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new EnvConfigSource(singletonMap("", "value"), 300))
                .build();

        assertEquals("value", config.getRawValue(""));
    }

    @Test
    void systemSources() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addSystemSources()
                .build();

        assertTrue(config.getConfigSources(SysPropConfigSource.class).iterator().hasNext());
        assertTrue(config.getConfigSources(EnvConfigSource.class).iterator().hasNext());
    }

    @Test
    void propertiesSources(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("application.properties").toFile();
        Properties properties = new Properties();
        properties.setProperty("my.prop", "1234");
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(new URLClassLoader(new URL[] { tempDir.toUri().toURL() }))
                .addPropertiesSources()
                .build();

        assertFalse(config.getConfigSources(SysPropConfigSource.class).iterator().hasNext());
        assertFalse(config.getConfigSources(EnvConfigSource.class).iterator().hasNext());
        assertEquals("1234", config.getRawValue("my.prop"));
    }

    @Test
    void overrideIndexed() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("list[0]", "one", "list[1]", "two"))
                .build();

        String[] listArray = config.getValue("list", String[].class);
        assertEquals(2, listArray.length);
        assertEquals("one", listArray[0]);
        assertEquals("two", listArray[1]);
        List<String> listList = config.getValues("list", String.class);
        assertEquals(2, listList.size());
        assertEquals("one", listList.get(0));
        assertEquals("two", listList.get(1));
        Optional<String[]> listOptionalArray = config.getOptionalValue("list", String[].class);
        assertTrue(listOptionalArray.isPresent());
        listOptionalArray.ifPresent(list -> {
            assertEquals(2, list.length);
            assertEquals("one", list[0]);
            assertEquals("two", list[1]);
        });
        Optional<List<String>> listOptionalList = config.getOptionalValues("list", String.class);
        assertTrue(listOptionalList.isPresent());
        listOptionalList.ifPresent(new Consumer<List<String>>() {
            @Override
            public void accept(final List<String> list) {
                assertEquals(2, list.size());
                assertEquals("one", list.get(0));
                assertEquals("two", list.get(1));
            }
        });

        config = new SmallRyeConfigBuilder()
                .withSources(config("list[0]", "one", "list[1]", "two"))
                .withSources(new PropertiesConfigSource(Map.of("list", "three,four"), "", 1000))
                .build();

        listArray = config.getValue("list", String[].class);
        assertEquals(2, listArray.length);
        assertEquals("three", listArray[0]);
        assertEquals("four", listArray[1]);
        listList = config.getValues("list", String.class);
        assertEquals(2, listList.size());
        assertEquals("three", listList.get(0));
        assertEquals("four", listList.get(1));
        listOptionalArray = config.getOptionalValue("list", String[].class);
        assertTrue(listOptionalArray.isPresent());
        listOptionalArray.ifPresent(list -> {
            assertEquals(2, list.length);
            assertEquals("three", list[0]);
            assertEquals("four", list[1]);
        });
        listOptionalList = config.getOptionalValues("list", String.class);
        assertTrue(listOptionalList.isPresent());
        listOptionalList.ifPresent(new Consumer<List<String>>() {
            @Override
            public void accept(final List<String> list) {
                assertEquals(2, list.size());
                assertEquals("three", list.get(0));
                assertEquals("four", list.get(1));
            }
        });
    }

    @Test
    void overrideCommaSeparated() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("list", "one,two"))
                .withSources(new PropertiesConfigSource(Map.of("list[0]", "three", "list[1]", "four"), "", 1000))
                .build();

        String[] listArray = config.getValue("list", String[].class);
        assertEquals(2, listArray.length);
        assertEquals("three", listArray[0]);
        assertEquals("four", listArray[1]);
        List<String> listList = config.getValues("list", String.class);
        assertEquals(2, listList.size());
        assertEquals("three", listList.get(0));
        assertEquals("four", listList.get(1));
        Optional<String[]> listOptionalArray = config.getOptionalValue("list", String[].class);
        assertTrue(listOptionalArray.isPresent());
        listOptionalArray.ifPresent(list -> {
            assertEquals(2, list.length);
            assertEquals("three", list[0]);
            assertEquals("four", list[1]);
        });
        Optional<List<String>> listOptionalList = config.getOptionalValues("list", String.class);
        assertTrue(listOptionalList.isPresent());
        listOptionalList.ifPresent(new Consumer<List<String>>() {
            @Override
            public void accept(final List<String> list) {
                assertEquals(2, list.size());
                assertEquals("three", list.get(0));
                assertEquals("four", list.get(1));
            }
        });
    }
}
