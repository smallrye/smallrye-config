package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class DefaultValuesTest {
    @Test
    void defaultValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withDefaultValue("my.prop", "1234")
                .withDefaultValue("my.prop.default", "1234")
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("my.prop.default"));
    }

    @Test
    void defaultValuesMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withDefaultValues(new HashMap<String, String>() {
                    {
                        put("my.prop", "1234");
                        put("my.prop.default", "1234");
                    }
                })
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("my.prop.default"));
    }
}
