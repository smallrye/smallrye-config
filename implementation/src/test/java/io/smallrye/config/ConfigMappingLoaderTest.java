package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigMappingLoaderTest {
    @Test
    void multipleLoads() {
        ConfigMappingLoader.getImplementationClass(Server.class);
        ConfigMappingLoader.getImplementationClass(Server.class);

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
        List<ConfigMappingMetadata> configMappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerManual.class);
        configMappingsMetadata.forEach(
                mappingMetadata -> ConfigMappingLoader.loadClass(ServerManual.class.getClassLoader(),
                        mappingMetadata.getClassName(), mappingMetadata.getClassBytes()));
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
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
