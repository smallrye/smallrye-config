package io.smallrye.config;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class MappingConfigSourceInterceptorTest {
    @Test
    public void relocateAndFallback() {
        final Config config = buildConfig("mp.jwt.token.header", "Authorization",
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

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(
                        new RelocateConfigSourceInterceptor(
                                new HashMap<String, String>() {
                                    {
                                        put("smallrye.jwt.token.header", "mp.jwt.token.header");
                                    }
                                }),
                        new FallbackConfigSourceInterceptor(
                                new HashMap<String, String>() {
                                    {
                                        put("smallrye.jwt.token.header", "mp.jwt.token.header");
                                        put("smallrye.jwt.token.cookie", "mp.jwt.token.cookie");
                                    }
                                }))
                .build();
    }
}
