package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

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

        List<Integer> values = config.getValues("my.list", config.getConverter(Integer.class).get(), ArrayList::new);
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
                ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    void rawValueEquals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(config("my.prop", "1234")).build();

        assertTrue(config.rawValueEquals("my.prop", "1234"));
        assertFalse(config.rawValueEquals("my.prop", "0"));
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
    void getIndexedValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("dev", config.getValue("server.environments[0]", String.class));
        assertEquals("qa", environments.get(1));
        assertEquals("qa", config.getValue("server.environments[1]", String.class));
        assertEquals("prod", environments.get(2));
        assertEquals("prod", config.getValue("server.environments[2]", String.class));
    }

    @Test
    void getValuesNotIndexed() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "server.environments", "dev,qa",
                        "server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(2, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
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
                .withSources(config(
                        "server.environments", "dev,qa",
                        "server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
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

        assertFalse(config.getOptionalValues("server.environments", String.class).isPresent());

        SmallRyeConfig configIndexed = new SmallRyeConfigBuilder()
                .withSources(config("server.environments[0]", ""))
                .build();

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
                .withSources(config("my.prop", "1234", "my.expansion", "${not.available}"))
                .withSources(new MapBackedConfigSource("hidder", new HashMap<String, String>() {
                    {
                        put("my.hidden", "hidden");
                    }
                }) {
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

        Set<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertEquals(2, names.size());
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
        assertEquals("1234", config.getRawValue("smallrye.mp.config.prop"));
        assertEquals("1234", config.getRawValue("smallrye.mp-config.prop"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("SMALLRYE_MP_CONFIG_PROP"));
        assertTrue(((Set<String>) config.getPropertyNames()).contains("smallrye.mp-config.prop"));
        assertFalse(((Set<String>) config.getPropertyNames()).contains("smallrye.mp.config.prop"));
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
}
