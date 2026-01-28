package io.smallrye.config.jasypt;

import static io.smallrye.config.EnvConfigSource.ORDINAL;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class JasyptSecretKeysHandlerTest {
    @Test
    void jasypt() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.jasypt.enabled", "true",
                "smallrye.config.secret-handler.jasypt.password", "jasypt",
                "smallrye.config.secret-handler.jasypt.algorithm", "PBEWithHMACSHA512AndAES_256",
                "my.secret", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void jasyptWithEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new EnvConfigSource(Map.of(
                        "SMALLRYE_CONFIG_SECRET_HANDLER_JASYPT_ENABLED", "true",
                        "SMALLRYE_CONFIG_SECRET_HANDLER_JASYPT_PASSWORD", "jasypt",
                        "SMALLRYE_CONFIG_SECRET_HANDLER_JASYPT_ALGORITHM", "PBEWithHMACSHA512AndAES_256"), ORDINAL))
                .withSources(new PropertiesConfigSource(
                        Map.of("my.secret", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}"),
                        "", 0))
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void expression() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.jasypt.enabled", "true",
                "smallrye.config.secret-handler.jasypt.password", "jasypt",
                "smallrye.config.secret-handler.jasypt.algorithm", "PBEWithHMACSHA512AndAES_256",
                "my.secret", "${my.expression}",
                "my.expression", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void deprecatedException() {
        Map<String, String> properties = Map.of(
                "smallrye.config.secret-handler.jasypt.password", "jasypt",
                "smallrye.config.secret-handler.jasypt.algorithm", "PBEWithHMACSHA512AndAES_256",
                "my.secret", "${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}");

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(new PropertiesConfigSource(properties, "", 0));

        assertThrows(UnsupportedOperationException.class, builder::build);
    }
}
