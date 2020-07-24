package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigMappingClassTest {
    @Test
    void toClass() {
        final Class<?> klass = ConfigMappingClass.toInterface(ServerClass.class);
        ConfigMappingClass.toInterface(ServerClass.class);

        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(klass)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        final ServerClass server = config.getConfigMapping(ServerClass.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    static class ServerClass {
        String host;
        int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
