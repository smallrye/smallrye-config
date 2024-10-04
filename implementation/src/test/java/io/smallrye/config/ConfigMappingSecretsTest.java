package io.smallrye.config;

import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.KeyValuesConfigSource.config;
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

import org.junit.jupiter.api.Test;

class ConfigMappingSecretsTest {
    @Test
    void secrets() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withConverter(Secret.class, 100, new Secret.SecretConverter(STRING_CONVERTER))
                .withMapping(MappingSecrets.class)
                .withSources(config(
                        "secrets.secret", "secret",
                        "secrets.list[0]", "secret",
                        "secrets.map.key", "secret",
                        "secrets.optional", "secret"))
                .withSecretKeys()
                .build();

        MappingSecrets mapping = config.getConfigMapping(MappingSecrets.class);
        assertEquals("secret", mapping.secret().get());
        assertEquals("secret", mapping.list().get(0).get());
        assertEquals("secret", mapping.map().get("key").get());
        assertTrue(mapping.optional().isPresent());
        assertEquals("secret", mapping.optional().get().get());

        assertThrows(SecurityException.class, () -> config.getRawValue("secrets.secret"));
        assertThrows(SecurityException.class, () -> config.getRawValue("secrets.list[0]"));
        assertThrows(SecurityException.class, () -> config.getRawValue("secrets.map.key"));
        assertThrows(SecurityException.class, () -> config.getRawValue("secrets.optional"));

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertFalse(properties.contains("secrets.secret"));
        assertFalse(properties.contains("secrets.secrets[0]"));
        assertFalse(properties.contains("secrets.secret-map.key"));
        assertFalse(properties.contains("secrets.optional"));
    }

    @ConfigMapping(prefix = "secrets")
    interface MappingSecrets {
        Secret<String> secret();

        List<Secret<String>> list();

        Map<String, Secret<String>> map();

        Optional<Secret<String>> optional();
    }
}
