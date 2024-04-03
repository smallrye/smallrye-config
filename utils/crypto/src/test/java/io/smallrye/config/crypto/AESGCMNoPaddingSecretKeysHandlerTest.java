package io.smallrye.config.crypto;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_SECRET_HANDLERS;
import static io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandler.DECODE_KEY;
import static io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandler.ENCRYPTION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class AESGCMNoPaddingSecretKeysHandlerTest {
    @Test
    void handler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        ENCRYPTION_KEY, "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI",
                        "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key-decode", "true",
                        "my.secret", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}",
                        "my.expression", "${not.found:default}",
                        "another.expression", "${my.expression}"))
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
        assertEquals("default", config.getRawValue("my.expression"));
        assertEquals("default", config.getRawValue("another.expression"));
    }

    @Test
    void plainKey() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        ENCRYPTION_KEY, "somearbitrarycrazystringthatdoesnotmatter",
                        "my.secret", "${aes-gcm-nopadding::DPZqAC4GZNAXi6_43A4O2SBmaQssGkq6PS7rz8tzHDt1}"))
                .build();

        assertEquals("1234", config.getRawValue("my.secret"));
    }

    @Test
    void keystore() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        "smallrye.config.source.keystore.\"properties\".path", "properties",
                        "smallrye.config.source.keystore.\"properties\".password", "arealpassword",
                        "smallrye.config.source.keystore.\"properties\".handler", "aes-gcm-nopadding",
                        "smallrye.config.source.keystore.\"key\".path", "key",
                        "smallrye.config.source.keystore.\"key\".password", "anotherpassword"))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("1234", secret.getValue());
    }

    @Test
    void noEncriptionKey() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        "my.secret", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}"))
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getConfigValue("my.secret"));

        Map<String, String> properties = Map.of(SMALLRYE_CONFIG_SECRET_HANDLERS, "none");
        new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();
        assertTrue(true);
    }

    @Test
    void configurableSource() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        "smallrye.config.source.keystore.test.path", "keystore",
                        "smallrye.config.source.keystore.test.password", "secret",
                        "smallrye.config.source.keystore.test.handler", "aes-gcm-nopadding"))
                .withSources((ConfigSourceFactory) context -> List.of(
                        new PropertiesConfigSource(Map.of(
                                ENCRYPTION_KEY, "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI",
                                DECODE_KEY, "true"), "", 0)))
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("decoded", secret.getValue());
    }

    @Test
    void expression() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        ENCRYPTION_KEY, "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI",
                        DECODE_KEY, "true",
                        "my.secret", "${my.expression}",
                        "my.expression", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}"))
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
    }

    @Test
    void configurableExpression() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValues(Map.of(
                        ENCRYPTION_KEY, "${key}",
                        DECODE_KEY, "true",
                        "my.secret", "${my.expression}",
                        "my.expression", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}",
                        "key", "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI"))
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
    }
}
