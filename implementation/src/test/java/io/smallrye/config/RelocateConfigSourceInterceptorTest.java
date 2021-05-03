package io.smallrye.config;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

class RelocateConfigSourceInterceptorTest {
    @Test
    void relocateAndFallback() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "mp.jwt.token.cookie", "Bearer");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        assertEquals("Bearer", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void relocate() {
        Config config = buildConfig(
                "smallrye.jwt.token.header", "Cookie",
                "mp.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void fallback() {
        Config config = buildConfig(
                "smallrye.jwt.token.cookie", "jwt",
                "mp.jwt.token.cookie", "Bearer");

        assertEquals("jwt", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void relocateWithProfile() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void relocateWithProfileAndExpression() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    void relocateWithProfileExpressionAndFallback() {
        Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                "smallrye.jwt.token.cookie", "jwt",
                "%prof.smallrye.jwt.token.cookie", "Basic",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertEquals("Basic", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    void relocateIsSecret() {
        Config config = buildConfig(
                Collections.singleton("mp.jwt.token.header"),
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_CONFIG_PROFILE, "prof");

        assertThrows(SecurityException.class, () -> config.getValue("smallrye.jwt.token.header", String.class));
        assertThrows(SecurityException.class, () -> config.getValue("mp.jwt.token.header", String.class));
    }

    @Test
    void relocatePropertyNames() {
        Config config = buildConfig("smallrye.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        List<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("smallrye.jwt.token.header"));
        assertTrue(names.contains("mp.jwt.token.header"));

        RelocateConfigSourceInterceptor relocateInterceptor = new RelocateConfigSourceInterceptor(
                s -> s.replaceAll("smallrye\\.jwt\\.token\\.header", "mp.jwt.token.header"));
        Iterator<ConfigValue> configValues = relocateInterceptor.iterateValues(new ConfigSourceInterceptorContext() {
            @Override
            public ConfigValue proceed(final String name) {
                return null;
            }

            @Override
            public Iterator<String> iterateNames() {
                return null;
            }

            @Override
            public Iterator<ConfigValue> iterateValues() {
                Set<ConfigValue> values = new HashSet<>();
                values.add(
                        ConfigValue.builder().withName("smallrye.jwt.token.header").withValue("Authorization").build());
                return values.iterator();
            }
        });

        Map<String, ConfigValue> values = new HashMap<>();
        while (configValues.hasNext()) {
            ConfigValue configValue = configValues.next();
            values.put(configValue.getName(), configValue);
        }

        assertEquals(2, values.size());
        assertEquals("Authorization", values.get("smallrye.jwt.token.header").getValue());
        assertEquals("Authorization", values.get("mp.jwt.token.header").getValue());
    }

    private static Config buildConfig(String... keyValues) {
        return buildConfig(Collections.emptySet(), keyValues);
    }

    private static Config buildConfig(Set<String> secretKeys, String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(
                        new RelocateConfigSourceInterceptor(
                                s -> s.replaceAll("smallrye\\.jwt\\.token\\.header", "mp.jwt.token.header")),
                        new FallbackConfigSourceInterceptor(
                                s -> s.replaceAll("smallrye\\.jwt", "mp.jwt")))
                .withInterceptors(new LoggingConfigSourceInterceptor())
                .withSecretKeys(secretKeys.toArray(new String[0]))
                .build();
    }
}
