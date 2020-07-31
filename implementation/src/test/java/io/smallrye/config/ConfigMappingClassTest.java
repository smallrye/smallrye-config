package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;

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

    @Test
    void optionals() {
        final Class<?> klass = ConfigMappingClass.toInterface(ServerOptional.class);
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(klass)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        final ServerOptional server = config.getConfigMapping(ServerOptional.class);
        assertTrue(server.getHost().isPresent());
        assertEquals("localhost", server.getHost().get());
        assertTrue(server.getPort().isPresent());
        assertEquals(8080, server.getPort().getAsInt());
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

    static class ServerOptional {
        Optional<String> host;
        OptionalInt port;

        public Optional<String> getHost() {
            return host;
        }

        public OptionalInt getPort() {
            return port;
        }
    }
}
