package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;

public class ConfigMappingsTest {
    @Test
    void registerMapping() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .build();

        registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server")));
        Server server = config.getConfigMapping(Server.class);

        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void registerProperties() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .build();

        registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validate() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .build();

        assertThrows(ConfigValidationException.class,
                () -> registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server"))),
                "server.unmapped does not map to any root");

        registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @ConfigMapping(prefix = "server")
    interface Server {
        String host();

        int port();
    }

    @ConfigProperties(prefix = "server")
    static class ServerClass {
        String host;
        int port;
    }
}
