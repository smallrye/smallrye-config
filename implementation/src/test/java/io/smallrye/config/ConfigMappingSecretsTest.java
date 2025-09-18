package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SecretKeys.doUnlocked;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

class ConfigMappingSecretsTest {
    @Test
    void secrets() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MappingSecrets.class)
                .withSources(config(
                        "secrets.secret", "secret",
                        "secrets.optional", "secret",
                        "secrets.list[0]", "secret",
                        "secrets.optional-list[0]", "secret",
                        "secrets.map.key", "secret",
                        "secrets.map-list.key[0]", "secret"))
                .withSecretKeys()
                .build();

        MappingSecrets mapping = config.getConfigMapping(MappingSecrets.class);
        assertEquals("secret", mapping.secret().get());
        assertTrue(mapping.optional().isPresent());
        assertEquals("secret", mapping.optional().get().get());
        assertEquals("secret", mapping.list().get(0).get());
        assertTrue(mapping.optionalList().isPresent());
        assertEquals("secret", mapping.optionalList().get().get(0).get());
        assertEquals("secret", mapping.map().get("key").get());
        assertEquals("secret", mapping.mapList().get("key").get(0).get());

        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.secret"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.optional"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.list[0]"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.optional-list[0]"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.map.key"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.map-list.key[0]"));

        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.secret").getValue()));
        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.optional").getValue()));
        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.list[0]").getValue()));
        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.optional-list[0]").getValue()));
        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.map.key").getValue()));
        assertEquals("secret", doUnlocked(() -> config.getConfigValue("secrets.map-list.key[0]").getValue()));

        Set<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertFalse(names.contains("secrets.secret"));
        assertFalse(names.contains("secrets.optional"));
        assertFalse(names.contains("secrets.list[0]"));
        assertFalse(names.contains("secrets.optional-list[0]"));
        assertFalse(names.contains("secrets.map.key"));
        assertFalse(names.contains("secrets.map-list.key[0]"));

        names = doUnlocked(() -> stream(config.getPropertyNames().spliterator(), false).collect(toSet()));
        assertTrue(names.contains("secrets.secret"));
        assertTrue(names.contains("secrets.optional"));
        assertTrue(names.contains("secrets.list[0]"));
        assertTrue(names.contains("secrets.optional-list[0]"));
        assertTrue(names.contains("secrets.map.key"));
        assertTrue(names.contains("secrets.map-list.key[0]"));

        assertEquals("MappingSecrets{}", mapping.toString());
    }

    @Test
    void profiles() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MappingSecrets.class)
                .withSources(config(
                        "%dev.secrets.secret", "secret",
                        "secrets.secret", "secret",
                        "secrets.optional", "secret",
                        "secrets.list[0]", "secret",
                        "secrets.optional-list[0]", "secret",
                        "secrets.map.key", "secret",
                        "secrets.map-list.key[0]", "secret"))
                .withSecretKeys()
                .build();

        Set<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(names.isEmpty());

        assertThrows(SecurityException.class, () -> config.getConfigValue("%dev.secrets.secret"));
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.secret"));

        assertEquals("secret", doUnlocked(() -> config.getConfigValue("%dev.secrets.secret").getValue()));
    }

    @ConfigMapping(prefix = "secrets")
    interface MappingSecrets {
        Secret<String> secret();

        Optional<Secret<String>> optional();

        List<Secret<String>> list();

        Optional<List<Secret<String>>> optionalList();

        Map<String, Secret<String>> map();

        Map<String, List<Secret<String>>> mapList();

        @Override
        String toString();
    }

    @Test
    void convertWith() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(ConvertWithSecrets.class)
                .withSources(config(
                        "secrets.secret", "secret",
                        "secrets.optional", "secret",
                        "secrets.list[0]", "secret",
                        "secrets.optional-list[0]", "secret",
                        "secrets.map.key", "secret",
                        "secrets.map-list.key[0]", "secret"))
                .withSecretKeys()
                .build();

        ConvertWithSecrets mapping = config.getConfigMapping(ConvertWithSecrets.class);
        assertEquals("redacted", mapping.secret().get());
        assertTrue(mapping.optional().isPresent());
        assertEquals("redacted", mapping.optional().get().get());
        assertEquals("redacted", mapping.list().get(0).get());
        assertTrue(mapping.optionalList().isPresent());
        assertEquals("redacted", mapping.optionalList().get().get(0).get());
        assertEquals("redacted", mapping.map().get("key").get());
        assertEquals("redacted", mapping.mapList().get("key").get(0).get());
    }

    @ConfigMapping(prefix = "secrets")
    interface ConvertWithSecrets {
        @WithConverter(RedactedConverter.class)
        Secret<String> secret();

        Optional<@WithConverter(RedactedConverter.class) Secret<String>> optional();

        List<@WithConverter(RedactedConverter.class) Secret<String>> list();

        Optional<List<@WithConverter(RedactedConverter.class) Secret<String>>> optionalList();

        Map<String, @WithConverter(RedactedConverter.class) Secret<String>> map();

        Map<String, List<@WithConverter(RedactedConverter.class) Secret<String>>> mapList();

        class RedactedConverter implements Converter<String> {
            @Override
            public String convert(String value) throws IllegalArgumentException, NullPointerException {
                return "redacted";
            }
        }
    }
}
