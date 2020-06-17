package io.smallrye.config.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.config.KeyValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigMappingTest {
    @Test
    void configMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void configMappingUnmapped() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();
        // TODO - Most likely we want to do a warning here, but just to try out the feature.
        assertThrows(Exception.class, () -> config.getConfigProperties(Configs.class, "server"));
    }

    @Test
    void configMappingIgnorePropertiesInUnregisteredRoots() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "client.name", "konoha"))
                .build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void configMappingIgnoreProperties() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    interface Configs {
        String host();

        int port();
    }
}
