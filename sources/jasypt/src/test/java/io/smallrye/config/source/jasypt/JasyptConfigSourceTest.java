package io.smallrye.config.source.jasypt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class JasyptConfigSourceTest {
    @Test
    void secret() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredInterceptors()
                .build();

        assertEquals("username", config.getRawValue("username"));
        assertEquals("username", config.getRawValue("expression"));

        ConfigValue secret = config.getConfigValue("secret");
        assertNotNull(secret, secret.getValue());

        assertEquals("12345678", secret.getValue());
        assertEquals("${jasypt:ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}", secret.getRawValue());
    }
}
