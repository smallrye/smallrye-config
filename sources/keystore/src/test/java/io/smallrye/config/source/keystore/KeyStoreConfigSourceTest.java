package io.smallrye.config.source.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class KeyStoreConfigSourceTest {
    @Test
    void keystore() {
        // keytool -importpass -alias my.secret -keystore keystore -storepass secret -storetype PKCS12 -v
        Map<String, String> properties = Map.of(
                "io.smallrye.config.source.keystore.test.path", "keystore",
                "io.smallrye.config.source.keystore.test.password", "secret");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withProfile("prod")
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("secret", secret.getValue());
    }

    @Test
    void keyStoreNotFound() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withProfile("prod")
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(Map.of(
                        "io.smallrye.config.source.keystore.test.path", "not.found",
                        "io.smallrye.config.source.keystore.test.password", "secret"), "", 0))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertNull(secret.getValue());

        assertThrows(IllegalStateException.class, () -> new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(Map.of(
                        "io.smallrye.config.source.keystore.test.path", "file:/not.found",
                        "io.smallrye.config.source.keystore.test.password", "secret"), "", 0))
                .build());
    }
}
