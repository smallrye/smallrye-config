package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DefaultValuesTest {
    @Test
    void defaultValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234"))
                .withDefaultValue("my.prop", "1234")
                .withDefaultValue("my.prop.default", "1234")
                .build();

        assertEquals("1234", config.getConfigValue("my.prop").getValue());
        assertEquals("1234", config.getConfigValue("my.prop.default").getValue());
    }

    @Test
    void defaultValuesMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.value", "5678"))
                .withDefaultValues(Map.of(
                        "my.value", "1234",
                        "my.default-value", "1234",
                        "my.list", "1234",
                        "my.map.key", "1234",
                        "my.list-nested[0].value", "1234",
                        "my.map-nested.key.value", "1234"))
                .withMapping(DefaultValues.class)
                .build();

        DefaultValues mapping = config.getConfigMapping(DefaultValues.class);

        assertEquals("5678", config.getConfigValue("my.value").getValue());
        assertEquals("1234", config.getConfigValue("my.default-value").getValue());
        assertEquals("5678", mapping.value());
        assertEquals("1234", mapping.defaultValue());
        assertEquals("1234", mapping.list().get(0));
        assertEquals("1234", mapping.map().get("key"));
        assertEquals("1234", mapping.listNested().get(0).value());
        assertEquals("1234", mapping.mapNested().get("key").value());
    }

    @ConfigMapping(prefix = "my")
    interface DefaultValues {
        String value();

        String defaultValue();

        List<String> list();

        Map<String, String> map();

        List<Nested> listNested();

        Map<String, Nested> mapNested();

        interface Nested {
            String value();
        }
    }
}
