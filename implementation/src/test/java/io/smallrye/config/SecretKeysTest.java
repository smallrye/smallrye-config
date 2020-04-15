package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class SecretKeysTest {
    @Test
    public void lock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        assertThrows("Not allowed to access secret key secret", SecurityException.class,
                () -> config.getValue("secret", String.class));
        assertEquals("value", config.getValue("not.secret", String.class));
    }

    @Test
    public void unlock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doUnlocked(() -> assertEquals("12345678", config.getValue("secret", String.class)));
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));

        assertThrows("Not allowed to access secret key secret", SecurityException.class,
                () -> config.getValue("secret", String.class));
    }

    @Test
    public void unlockAndLock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doUnlocked(() -> {
            assertEquals("12345678", config.getValue("secret", String.class));

            SecretKeys.doLocked(() -> {
                assertThrows("Not allowed to access secret key secret", SecurityException.class,
                        () -> config.getValue("secret", String.class));
            });
        });

        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));
    }

    @Test
    public void lockAndUnlock() {
        final Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doLocked(() -> {
            assertThrows("Not allowed to access secret key secret", SecurityException.class,
                    () -> config.getValue("secret", String.class));

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
