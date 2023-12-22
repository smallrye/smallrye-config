package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigMappingClassTest {
    @Test
    void toClass() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerClass.class)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void privateFields() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerPrivate.class)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        ServerPrivate server = config.getConfigMapping(ServerPrivate.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void optionals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerOptional.class)
                .withDefaultValue("host", "localhost")
                .withDefaultValue("port", "8080")
                .build();

        ServerOptional server = config.getConfigMapping(ServerOptional.class);
        assertTrue(server.getHost().isPresent());
        assertEquals("localhost", server.getHost().get());
        assertTrue(server.getPort().isPresent());
        assertEquals(8080, server.getPort().getAsInt());
    }

    @Test
    void names() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerNames.class)
                .withDefaultValue("h", "localhost")
                .withDefaultValue("p", "8080")
                .build();
        ServerNames server = config.getConfigMapping(ServerNames.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void defaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerDefaults.class).build();
        ServerDefaults server = config.getConfigMapping(ServerDefaults.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void converters() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerConverters.class)
                .withConverters(new Converter[] { new ServerPortConverter() }).build();
        ServerConverters server = config.getConfigMapping(ServerConverters.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort().getPort());
    }

    @Test
    void initialized() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerInitialized.class)
                .withDefaultValue("host", "localhost")
                .build();
        ServerInitialized server = config.getConfigMapping(ServerInitialized.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void initializedDefault() {
        assertThrows(Exception.class, () -> new SmallRyeConfigBuilder().withMapping(ServerInitializedDefault.class).build());
    }

    @Test
    void mpConfig20() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerMPConfig20.class)
                .withDefaultValue("name", "localhost")
                .build();
        ServerMPConfig20 server = config.getConfigMapping(ServerMPConfig20.class);
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.getPort());
    }

    @Test
    void empty() {
        try {
            new SmallRyeConfigBuilder().withMapping(Empty.class).build();
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    void camelCase() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerCamelCase.class)
                .withDefaultValue("theHost", "localhost")
                .withDefaultValue("thePort", "8080")
                .build();

        ServerCamelCase server = config.getConfigMapping(ServerCamelCase.class);
        assertEquals("localhost", server.getTheHost());
        assertEquals(8080, server.getThePort());
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

        public ServerPort(String port) {
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

    static class ServerInitializedDefault {
        private int port;

        public int getPort() {
            return port;
        }
    }

    static class ServerMPConfig20 {
        @ConfigProperty(name = "name")
        private String host;
        @ConfigProperty(defaultValue = "8080")
        private int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    static class Empty {

    }

    static class ServerCamelCase {
        private String theHost;
        private int thePort;

        public String getTheHost() {
            return theHost;
        }

        public int getThePort() {
            return thePort;
        }
    }
}
