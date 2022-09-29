package io.smallrye.config.source.keystore;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyStoreConfigSourceTest {
    @Test
    void keystore() throws Exception {
        Map<String, String> properties = new HashMap<>();
        // keytool -importpass -alias my.secret -keystore keystore -storepass secret -storetype PKCS12 -v
        properties.put("io.smallrye.config.source.keystore.test.path", "keystore");
        properties.put("io.smallrye.config.source.keystore.test.password", "secret");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .addDiscoveredSources()
            .withSources(new PropertiesConfigSource(properties, "", 0))
            .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("secret", secret.getValue());
    }
}
