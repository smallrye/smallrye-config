package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigMappingObjectLoaderTest {
    @Test
    void multipleLoads() {
        ConfigMappingObjectLoader.getImplementationClass(Server.class);
        ConfigMappingObjectLoader.getImplementationClass(Server.class);

        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080"))
                .withMapping(Server.class)
                .build();

        Server server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void loadManually() {
        ConfigMappingMetadata mappingMetadata = ConfigMappingObjectLoader.getConfigMappingMetadata(ServerManual.class);
        ConfigMappingObjectLoader.createMappingObjectClass(mappingMetadata.getClassName(), mappingMetadata.getClassBytes());
        ConfigMappingObjectLoader.getImplementationClass(ServerManual.class);
        ConfigMappingObjectLoader.getImplementationClass(ServerManual.class);
    }

    @ConfigMapping(prefix = "server")
    public interface Server {
        String host();

        int port();
    }

    interface ServerManual {
        String host();

        int port();
    }
}
