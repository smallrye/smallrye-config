package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.Converter;
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

    @Test
    void names() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerNames.class)
                .withDefaultValue("h", "localhost")
                .withDefaultValue("p", "8080")
                .build();
        final ServerNames server = config.getConfigMapping(ServerNames.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void defaults() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerDefaults.class).build();
        final ServerDefaults server = config.getConfigMapping(ServerDefaults.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void converters() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerConverters.class)
                .withConverters(new Converter[] { new ServerPortConverter() }).build();
        final ServerConverters server = config.getConfigMapping(ServerConverters.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort().getPort());
    }

    @Test
    void initialized() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerInitialized.class)
                .withDefaultValue("host", "localhost")
                //.withDefaultValue("port", "8080")
                .build();
        final ServerInitialized server = config.getConfigMapping(ServerInitialized.class);
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

    static class ServerNames {
        @WithName("h")
        private String host;
        @WithName("p")
        private int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    static class ServerDefaults {
        @WithDefault("localhost")
        private String host;
        @WithDefault("8080")
        private int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    static class ServerConverters {
        @WithDefault("localhost")
        private String host;
        @WithDefault("8080")
        @WithConverter(ServerPortConverter.class)
        private ServerPort port;

        public String getHost() {
            return host;
        }

        public ServerPort getPort() {
            return port;
        }
    }

    static class ServerPort {
        private int port;

        public ServerPort(final String port) {
            this.port = Integer.parseInt(port);
        }

        public int getPort() {
            return port;
        }
    }

    static class ServerPortConverter implements Converter<ServerPort> {
        @Override
        public ServerPort convert(final String value) {
            return new ServerPort(value);
        }
    }

    static class ServerInitialized {
        private String host;
        private int port = 8080;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
