package io.smallrye.config.test.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SecretKeysHandler;
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
                "reverse.secret", "${reverse::drowssap}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSecretKeysHandlers(new ReverseSecretKeysHandler())
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("decoded", config.getConfigValue("aes-gcm-nopadding.secret").getValue());
        assertEquals("password", config.getConfigValue("reverse.secret").getValue());
    }

    static class ReverseSecretKeysHandler implements SecretKeysHandler {
        @Override
        public String decode(final String secret) {
            return new StringBuilder(secret).reverse().toString();
        }

        @Override
        public String getName() {
            return "reverse";
        }
    }
}
