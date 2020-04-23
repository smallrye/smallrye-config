package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import org.junit.Test;

import io.smallrye.config.common.MapBackedConfigSource;

public class SmallRyeConfigTest {
    @Test
    public void addConfigSource() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().addDefaultInterceptors().build();
        assertNull(config.getConfigValue("my.prop"));

        config.addConfigSource(KeyValuesConfigSource.config("my.prop", "1"));
        assertEquals("1", config.getRawValue("my.prop"));
    }

    @Test
    public void addMultiple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().addDefaultInterceptors().build();
        assertNull(config.getConfigValue("my.prop"));

        config.addConfigSource(KeyValuesConfigSource.config("my.prop", "1"));
        assertEquals("1", config.getRawValue("my.prop"));

        config.addConfigSource(new MapBackedConfigSource("higher", new HashMap<String, String>() {
            {
                put("my.prop", "2");
            }
        }, 200) {
        });
        assertEquals("2", config.getRawValue("my.prop"));
    }
}
