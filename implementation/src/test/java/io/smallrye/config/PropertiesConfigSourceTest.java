package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class PropertiesConfigSourceTest {
    @Test
    void interceptor() throws Exception {
        SmallRyeConfig config = buildConfig().unwrap(SmallRyeConfig.class);

        assertEquals("1", config.getValue("my.prop", String.class));
        assertEquals("20", config.getValue("my.prop.20", String.class));
        assertEquals("1", config.getConfigValue("my.prop").getValue());
        assertEquals("1", config.getConfigValue("my.prop").getValue());
        assertEquals("abc", config.getConfigValue("my.prop").getRawValue());
    }

    @Test
    void configSourceMap() throws IOException {
        PropertiesConfigSource configSource = new PropertiesConfigSource(
                PropertiesConfigSourceTest.class.getResource("/config-values.properties"));
        Map<String, String> properties = configSource.getProperties();

        assertEquals("abc", properties.get("my.prop"));
        assertEquals("abc", properties.get("my.prop"));
        assertThrows(UnsupportedOperationException.class, () -> properties.remove("x"));
        assertThrows(UnsupportedOperationException.class, () -> properties.put("x", "x"));
        assertThrows(UnsupportedOperationException.class, () -> properties.putAll(new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, properties::clear);
    }

    @Test
    void names() {
        PropertiesConfigSource configSource = new PropertiesConfigSource(Map.of("my.prop", "1234"), "name", 100);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(configSource)
                .build();

        assertEquals(configSource.getName(), config.getConfigValue("my.prop").getConfigSourceName());
    }

    private static Config buildConfig() throws Exception {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(new PropertiesConfigSource(
                        PropertiesConfigSourceTest.class.getResource("/config-values.properties")))
                .withInterceptors((ConfigSourceInterceptor) (context, name) -> {
                    ConfigValue configValue = context.proceed(name);
                    // Return the line number instead for asssert
                    if (configValue != null) {
                        configValue = configValue.withValue(configValue.getLineNumber() + "");
                    }

                    return configValue;
                })
                .build();
    }
}
