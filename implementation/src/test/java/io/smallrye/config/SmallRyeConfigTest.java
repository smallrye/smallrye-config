package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

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
}
