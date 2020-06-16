package io.smallrye.config.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.config.KeyValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigMappingTest {
    @Test
    void configMapping() {
        final SmallRyeConfig config = buildConfig("server.host", "localhost", "server.port", "8080");
        final ConfigsInterface configProperties = config.getConfigProperties(ConfigsInterface.class, "server");
        //assertEquals("localhost", configProperties.getHost());
        assertEquals("localhost", configProperties.host());
        //assertEquals(8080, configProperties.getPort());
        assertEquals(8080, configProperties.port());
    }

    interface ConfigsInterface {
        String host();

        int port();
    }

    private static SmallRyeConfig buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }
}
