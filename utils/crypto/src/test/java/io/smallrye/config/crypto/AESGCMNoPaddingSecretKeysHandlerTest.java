package io.smallrye.config.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class AESGCMNoPaddingSecretKeysHandlerTest {
    @Test
    void handler() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "somearbitrarycrazystringthatdoesnotmatter",
                "my.secret", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}",
                "my.expression", "${not.found:default}",
                "another.expression", "${my.expression}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
        assertEquals("default", config.getRawValue("my.expression"));
        assertEquals("default", config.getRawValue("another.expression"));
    }

    @Test
    void keystore() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "somearbitrarycrazystringthatdoesnotmatter",
                "io.smallrye.config.source.keystore.test.path", "keystore",
                "io.smallrye.config.source.keystore.test.password", "secret",
                "io.smallrye.config.source.keystore.test.algorithm", "aes-gcm-nopadding");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("decoded", secret.getValue());
    }

    @Test
    void noEncriptionKey() {
        assertThrows(NoSuchElementException.class, () -> new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .build());

        Map<String, String> properties = Map.of("smallrye.config.secret-handlers", "none");
        new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();
        assertTrue(true);
    }
}
