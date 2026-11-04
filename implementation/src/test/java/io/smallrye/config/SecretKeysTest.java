package io.smallrye.config;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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
    void propertyNamesIterableMultipleTimes() {
        Config config = buildConfig("secret", "12345678", "not.secret", "value");

        Iterable<String> locked = config.getPropertyNames();
        for (int i = 0; i < 2; i++) {
            Set<String> properties = StreamSupport.stream(locked.spliterator(), false).collect(toSet());
            assertFalse(properties.contains("secret"));
            assertTrue(properties.contains("not.secret"));
        }

        Iterable<String> unlocked = SecretKeys.doUnlocked(config::getPropertyNames);
        for (int i = 0; i < 2; i++) {
            Set<String> properties = StreamSupport.stream(unlocked.spliterator(), false).collect(toSet());
            assertTrue(properties.contains("secret"));
            assertTrue(properties.contains("not.secret"));
        }
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

    @Test
    void expressionReferencingSecretIsSecret() {
        SmallRyeConfig config = buildConfig("secret", "12345678", "not.secret", "value",
                "expression", "url:${secret}", "nested", "${expression}", "plain", "url:${not.secret}")
                .unwrap(SmallRyeConfig.class);

        assertThrows(SecurityException.class, () -> config.getConfigValue("expression"),
                "Not allowed to access secret key secret");

        SecretKeys.doUnlocked(() -> {
            ConfigValue expression = config.getConfigValue("expression");
            assertEquals("url:12345678", expression.getValue());
            assertTrue(expression.isSecret());

            ConfigValue nested = config.getConfigValue("nested");
            assertEquals("url:12345678", nested.getValue());
            assertTrue(nested.isSecret());

            ConfigValue plain = config.getConfigValue("plain");
            assertEquals("url:value", plain.getValue());
            assertFalse(plain.isSecret());
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
        assertThrows(SecurityException.class, () -> config.getConfigValue("mapping.secret"),
                "Not allowed to access secret key mapping.secret");
    }

    @Test
    void profiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfiles(List.of("dev", "test"))
                .withSources(KeyValuesConfigSource.config(
                        "secret", "default",
                        "%dev.secret", "dev",
                        "%test.db.named.password", "test",
                        "%dev,test.multiple", "multiple",
                        "%prod.secret", "prod",
                        "%prod.db.named.password", "prod",
                        "not.secret", "value"))
                .withSecretKeys("secret", "db.*.password", "multiple")
                .build();

        assertThrows(SecurityException.class, () -> config.getConfigValue("secret"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("%dev.secret"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("db.named.password"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("%test.db.named.password"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("multiple"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("%dev,test.multiple"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("%prod.secret"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("%prod.db.named.password"));
        assertEquals("value", config.getConfigValue("not.secret").getValue());

        ConfigValue secret = SecretKeys.doUnlocked(() -> config.getConfigValue("secret"));
        assertEquals("dev", secret.getValue());
        assertTrue(secret.isSecret());
        ConfigValue password = SecretKeys.doUnlocked(() -> config.getConfigValue("db.named.password"));
        assertEquals("test", password.getValue());
        assertTrue(password.isSecret());
        ConfigValue multiple = SecretKeys.doUnlocked(() -> config.getConfigValue("multiple"));
        assertEquals("multiple", multiple.getValue());
        assertTrue(multiple.isSecret());

        Set<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertFalse(properties.contains("secret"));
        assertFalse(properties.contains("db.named.password"));
        assertFalse(properties.contains("multiple"));
        assertFalse(properties.contains("%prod.secret"));
        assertFalse(properties.contains("%prod.db.named.password"));
        assertTrue(properties.contains("not.secret"));
    }

    @Test
    void relocateAndFallbackToSecret() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withInterceptors(new RelocateConfigSourceInterceptor(Map.of("relocate", "secret")))
                .withInterceptors(new FallbackConfigSourceInterceptor(Map.of("fallback", "secret")))
                .withSources(KeyValuesConfigSource.config("secret", "12345678"))
                .withSecretKeys("secret")
                .build();

        assertThrows(SecurityException.class, () -> config.getConfigValue("relocate"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("fallback"));
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getConfigValue("relocate")).getValue());
        assertEquals("12345678", SecretKeys.doUnlocked(() -> config.getConfigValue("fallback")).getValue());
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withSecretKeys("secret")
                .build();
    }
}
