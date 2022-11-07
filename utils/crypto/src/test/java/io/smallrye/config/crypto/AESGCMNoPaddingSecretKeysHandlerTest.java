package io.smallrye.config.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class AESGCMNoPaddingSecretKeysHandlerTest {
    @Test
    void handler() {
        Map<String, String> properties = new HashMap<>();
        properties.put("my.secret", "${aes-gcm-nopadding::encoded::x}");
        properties.put("my.expression", "${not.found:default}");
        properties.put("another.expression", "${my.expression}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
        assertEquals("default", config.getRawValue("my.expression"));
        assertEquals("default", config.getRawValue("another.expression"));
    }

    @Test
    void keystore() {
        Map<String, String> properties = new HashMap<>();
        properties.put("io.smallrye.config.source.keystore.test.path", "keystore");
        properties.put("io.smallrye.config.source.keystore.test.password", "secret");
        properties.put("io.smallrye.config.source.keystore.test.algorithm", "aes-gcm-nopadding");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("decoded", secret.getValue());
    }
}
