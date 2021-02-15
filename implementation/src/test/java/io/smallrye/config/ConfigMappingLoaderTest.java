package io.smallrye.config;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                mappingMetadata -> ConfigMappingLoader.loadClass(ServerManual.class, mappingMetadata));
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
    }

    @Test
    void discoverNested() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMappingInterface(ServerNested.class);
        List<ConfigMappingInterface> nested = mapping.getNested();
        assertEquals(4, nested.size());
        List<Class<?>> types = nested.stream().map(ConfigMappingInterface::getInterfaceType).collect(toList());
        assertTrue(types.contains(ServerNested.Environment.class));
        assertTrue(types.contains(ServerNested.Log.class));
        assertTrue(types.contains(ServerNested.Ssl.class));
        assertTrue(types.contains(ServerNested.App.class));
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

    public interface ServerNested {
        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {
            String host();

            int port();
        }

        interface Log {
            boolean enabled();
        }

        interface Ssl {
            String certificate();
        }

        interface App {
            String name();
        }
    }
}
