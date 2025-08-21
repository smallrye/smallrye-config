package io.smallrye.config.test.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class MultipleSecretHandlersTest {
    @Test
    void multipleHandlers() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key",
                "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI",
                "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key-decode", "true",
                "aes-gcm-nopadding.secret", "${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}",
                "smallrye.config.secret-handler.jasypt.password", "jasypt",
                "smallrye.config.secret-handler.jasypt.algorithm", "PBEWithHMACSHA512AndAES_256",
                "jasypt.secret", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("decoded", config.getConfigValue("aes-gcm-nopadding.secret").getValue());
        assertEquals("12345678", config.getConfigValue("jasypt.secret").getValue());
    }
}
