package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

public class SecretKeysTest {
    @Test
    public void lock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                "Not allowed to access secret key secret");
        assertEquals("value", config.getValue("not.secret", String.class));
    }

    @Test
    public void unlock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doUnlocked(() -> assertEquals("12345678", config.getValue("secret", String.class)));
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));

        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                "Not allowed to access secret key secret");
    }

    @Test
    public void unlockAndLock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doUnlocked(() -> {
            assertEquals("12345678", config.getValue("secret", String.class));

            SecretKeys.doLocked(() -> {
                assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                        "Not allowed to access secret key secret");
            });
        });

        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));
    }

    @Test
    public void lockAndUnlock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doLocked(() -> {
            assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                    "Not allowed to access secret key secret");

            SecretKeys.doUnlocked(() -> assertEquals("12345678", config.getValue("secret", String.class)));
        });
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withSecretKeys("secret")
                .build();
    }
}
