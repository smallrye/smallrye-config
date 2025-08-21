package io.smallrye.config;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class SecretKeysTest {
    @Test
    void lock() {
        Config config = buildConfig("secret", "12345678", "not.secret", "value");

        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                "Not allowed to access secret key secret");
        assertEquals("value", config.getValue("not.secret", String.class));

        Set<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertFalse(properties.contains("secret"));
        assertTrue(properties.contains("not.secret"));
    }

    @Test
    void unlock() {
        Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doUnlocked(() -> assertEquals("12345678", config.getValue("secret", String.class)));
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getValue("secret", String.class)));

        assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                "Not allowed to access secret key secret");

        Set<String> properties = SecretKeys
                .doUnlocked(() -> StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet()));
        assertTrue(properties.contains("secret"));
        assertTrue(properties.contains("not.secret"));
    }

    @Test
    void unlockAndLock() {
        Config config = buildConfig("secret", "12345678", "not.secret", "value");

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
    void lockAndUnlock() {
        Config config = buildConfig("secret", "12345678", "not.secret", "value");

        SecretKeys.doLocked(() -> {
            assertThrows(SecurityException.class, () -> config.getValue("secret", String.class),
                    "Not allowed to access secret key secret");

            SecretKeys.doUnlocked(() -> assertEquals("12345678", config.getValue("secret", String.class)));
        });
    }

    @ConfigMapping(prefix = "mapping")
    interface MappingSecret {
        String secret();
    }

    @Test
    void mapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MappingSecret.class)
                .withSources(KeyValuesConfigSource.config("mapping.secret", "secret"))
                .withSecretKeys("mapping.secret")
                .build();

        MappingSecret mapping = config.getConfigMapping(MappingSecret.class);
        assertEquals("secret", mapping.secret());
        assertThrows(SecurityException.class, () -> config.getConfigValue("mapping.secret").getValue(),
                "Not allowed to access secret key mapping.secret");
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withSecretKeys("secret")
                .build();
    }
}
