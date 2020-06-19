package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

public class LoggingConfigSourceInterceptorTest {
    @Test
    public void interceptor() throws Exception {
        Config config = buildConfig();

        assertEquals("abc", config.getValue("my.prop", String.class));
        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class));
        assertThrows(NoSuchElementException.class, () -> config.getValue("not.found", String.class));

        // This should not log the secret value:
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));
    }

    private static Config buildConfig() throws Exception {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(new ConfigValuePropertiesConfigSource(
                        LoggingConfigSourceInterceptorTest.class.getResource("/config-values.properties")))
                .withInterceptors(new LoggingConfigSourceInterceptor())
                .withSecretKeys("secret")
                .build();
    }
}
