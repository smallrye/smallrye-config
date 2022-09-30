package io.smallrye.config.jasypt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class JasyptSecretKeysHandlerTest {
    @Test
    void jasypt() {
        Map<String, String> properties = new HashMap<>();
        properties.put("my.secret", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("12345678", config.getRawValue("my.secret"));
    }
}
