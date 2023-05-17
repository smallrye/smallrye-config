package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class SecretKeysHandlerTest {
    @Test
    void notFound() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${missing::secret}")
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getConfigValue("my.secret"));
    }

    @Test
    void handler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSecretKeysHandlers(new SecretKeysHandler() {
                    @Override
                    public String decode(final String secret) {
                        return "decoded";
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertEquals("decoded", config.getRawValue("my.secret"));
    }

    @Test
    void disabled() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .setAddDiscoveredSecretKeysHandlers(false)
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertNotNull(config.getRawValue("my.secret"));
    }
}
