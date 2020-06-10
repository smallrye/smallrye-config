package io.smallrye.config;

import static io.smallrye.config.ProfileConfigSourceInterceptor.SMALLRYE_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

public class MappingConfigSourceInterceptorTest {
    @Test
    public void relocateAndFallback() {
        final Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "mp.jwt.token.cookie", "Bearer");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
        assertEquals("Bearer", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    public void relocate() {
        final Config config = buildConfig(
                "smallrye.jwt.token.header", "Cookie",
                "mp.jwt.token.header", "Authorization");

        assertEquals("Authorization", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    public void fallback() {
        final Config config = buildConfig(
                "smallrye.jwt.token.cookie", "jwt",
                "mp.jwt.token.cookie", "Bearer");

        assertEquals("jwt", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    public void relocateWithProfile() {
        final Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "Cookie",
                SMALLRYE_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    public void relocateWithProfileAndExpression() {
        final Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_PROFILE, "prof");

        assertEquals("Cookie", config.getValue("smallrye.jwt.token.header", String.class));
    }

    @Test
    public void relocateWithProfileExpressionAndFallback() {
        final Config config = buildConfig(
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                "smallrye.jwt.token.cookie", "jwt",
                "%prof.smallrye.jwt.token.cookie", "Basic",
                SMALLRYE_PROFILE, "prof");

        assertEquals("Basic", config.getValue("smallrye.jwt.token.cookie", String.class));
    }

    @Test
    public void relocateIsSecret() {
        final Config config = buildConfig(
                Collections.singleton("mp.jwt.token.header"),
                "mp.jwt.token.header", "Authorization",
                "%prof.mp.jwt.token.header", "${token.header}",
                "token.header", "Cookie",
                SMALLRYE_PROFILE, "prof");

        assertThrows(SecurityException.class, () -> config.getValue("smallrye.jwt.token.header", String.class));
        assertThrows(SecurityException.class, () -> config.getValue("mp.jwt.token.header", String.class));
    }

    private static Config buildConfig(String... keyValues) {
        return buildConfig(Collections.emptySet(), keyValues);
    }

    private static Config buildConfig(Set<String> secretKeys, String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
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
