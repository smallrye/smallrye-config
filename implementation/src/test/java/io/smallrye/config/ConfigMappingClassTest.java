package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigMappingClassTest {
    @Test
    void toClass() {
        final Class<?> klass = ConfigMappingClass.toInterface(ServerClass.class);
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(klass)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        final ServerClass server = config.getConfigMapping(ServerClass.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void privateFields() {
        final Class<?> klass = ConfigMappingClass.toInterface(ServerPrivate.class);
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(klass)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        final ServerPrivate server = config.getConfigMapping(ServerPrivate.class);
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

    static class ServerPrivate {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
