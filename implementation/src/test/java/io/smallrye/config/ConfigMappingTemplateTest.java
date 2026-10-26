package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

class ConfigMappingTemplateTest {
    @Test
    void populatesTemplate() {
        Server template = new Server();
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.ssl", "true"))
                .build();

        Server server = config.getConfigMapping(Server.class, "server");
        assertSame(template, server);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
        assertTrue(server.ssl);
    }

    @Test
    void keepsTemplateValuesWithoutConfiguration() {
        Server template = new Server();
        template.host = "template";
        template.port = 9000;
        template.ssl = true;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.host", "localhost"))
                .build();

        Server server = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", server.host);
        assertEquals(9000, server.port);
        assertTrue(server.ssl);
        // the template values are the defaults of the mapping
        assertTrue(config.getConfigValue("server.port").isDefault());
        assertEquals("9000", config.getConfigValue("server.port").getValue());
    }

    @Test
    void keepsTemplateValuesOverInitializerDefaults() {
        ServerDefaults template = new ServerDefaults();
        template.host = "template";
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerDefaults.class, "server", template)
                .build();

        ServerDefaults server = config.getConfigMapping(ServerDefaults.class, "server");
        assertEquals("template", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void builderDefaultValuesPopulateTemplate() {
        Server template = new Server();
        template.host = "template";
        template.port = 9000;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withDefaultValue("server.host", "default")
                .build();

        Server server = config.getConfigMapping(Server.class, "server");
        assertEquals("default", server.host);
        assertEquals(9000, server.port);

        // in any order
        config = new SmallRyeConfigBuilder()
                .withDefaultValue("server.host", "default")
                .withMapping(Server.class, "server", template)
                .build();
        assertEquals("default", config.getConfigMapping(Server.class, "server").host);
    }

    @Test
    void keepsFinalFieldsWithoutNoArgsConstructor() {
        ServerFinal template = new ServerFinal("template");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerFinal.class, "server", template)
                .withSources(config("server.id", "config", "server.host", "localhost"))
                .build();

        ServerFinal server = config.getConfigMapping(ServerFinal.class, "server");
        assertSame(template, server);
        assertEquals("template", server.id);
        assertEquals("localhost", server.host);
    }

    @Test
    void requiresTemplateWithoutNoArgsConstructor() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(ServerFinal.class, "server")
                .withSources(config("server.id", "config", "server.host", "localhost"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(exception.getMessage().contains("SRCFG00063"));
    }

    @Test
    void requiresTemplateOfType() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> builder.withMapping(ConfigMappings.ConfigClass.configClass(Server.class, "server"), new Object()));
        assertTrue(exception.getMessage().contains("SRCFG00062"));
    }

    @Test
    void requiresTemplateOfClass() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> builder.withMapping(ServerInterface.class, "server", new ServerInterface() {
                    @Override
                    public String host() {
                        return "template";
                    }
                }));
        assertTrue(exception.getMessage().contains("SRCFG00064"));
    }

    @Test
    void keepsTemplateNestedGroupWithoutConfiguration() {
        ServerNested template = new ServerNested();
        Log log = new Log();
        log.level = "DEBUG";
        log.size = 10;
        template.log = log;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.host", "localhost"))
                .build();

        // a nested config class instance is replaced, but keeps the template values
        ServerNested server = config.getConfigMapping(ServerNested.class, "server");
        assertEquals("localhost", server.host);
        assertNotSame(log, server.log);
        assertEquals("DEBUG", server.log.level);
        assertEquals(10, server.log.size);
    }

    @Test
    void populatesTemplateNestedGroup() {
        ServerNested template = new ServerNested();
        template.log = new Log();
        template.log.level = "DEBUG";
        template.log.size = 10;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.host", "localhost", "server.log.level", "WARN"))
                .build();

        ServerNested server = config.getConfigMapping(ServerNested.class, "server");
        assertEquals("WARN", server.log.level);
        assertEquals(10, server.log.size);
    }

    @Test
    void createsTemplateNestedGroupWithConfiguration() {
        ServerNested template = new ServerNested();
        template.host = "template";
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.log.level", "WARN"))
                .build();

        ServerNested server = config.getConfigMapping(ServerNested.class, "server");
        assertEquals("WARN", server.log.level);
        // the nested group defaults still apply
        assertEquals(100, server.log.size);
    }

    @Test
    void requiresNestedGroupWithoutTemplateValue() {
        // a nested config class without a template instance has no default for its required values
        ServerNested template = new ServerNested();
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.host", "localhost"));
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, builder::build);
        assertTrue(exception.getMessage().contains("server.log.level"));
    }

    @Test
    void keepsFinalNestedGroup() {
        ServerFinalNested template = new ServerFinalNested();
        template.log.level = "DEBUG";
        template.log.size = 10;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerFinalNested.class, "server", template)
                .withSources(config("server.log.size", "20"))
                .build();

        // a final field is never populated
        ServerFinalNested server = config.getConfigMapping(ServerFinalNested.class, "server");
        assertSame(template.log, server.log);
        assertEquals("DEBUG", server.log.level);
        assertEquals(10, server.log.size);
    }

    @Test
    void populatesTemplateDeepNestedGroups() {
        Root template = new Root();
        template.server = new ServerNested();
        template.server.host = "template";
        template.server.log = new Log();
        template.server.log.level = "DEBUG";
        template.server.log.size = 10;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Root.class, "root", template)
                .withSources(config("root.server.log.level", "WARN"))
                .build();

        Root root = config.getConfigMapping(Root.class, "root");
        assertEquals("template", root.server.host);
        assertEquals("WARN", root.server.log.level);
        assertEquals(10, root.server.log.size);
    }

    @Test
    void populatesTemplatePrivateNestedGroup() {
        ServerPrivateNested template = new ServerPrivateNested();
        template.log.level = "DEBUG";
        template.log.size = 10;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerPrivateNested.class, "server", template)
                .withSources(config("server.log.size", "20"))
                .build();

        ServerPrivateNested server = config.getConfigMapping(ServerPrivateNested.class, "server");
        assertEquals("DEBUG", server.log.level);
        assertEquals(20, server.log.size);
    }

    @Test
    void keepsTemplateOptionalNestedGroup() {
        ServerOptionalNested template = new ServerOptionalNested();
        template.log = Optional.of(new Log());
        template.log.get().level = "DEBUG";
        template.log.get().size = 10;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalNested.class, "server", template)
                .withSources(config("server.log.size", "20"))
                .build();

        ServerOptionalNested server = config.getConfigMapping(ServerOptionalNested.class, "server");
        assertEquals("DEBUG", server.log.get().level);
        assertEquals(20, server.log.get().size);
    }

    @Test
    void keepsTemplateNestedGroupList() {
        ServerNestedList template = new ServerNestedList();
        Log log = new Log();
        log.level = "DEBUG";
        log.size = 10;
        template.logs = List.of(log);
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNestedList.class, "server", template)
                .withSources(config("server.logs[0].size", "20", "server.logs[1].level", "WARN"))
                .build();

        ServerNestedList server = config.getConfigMapping(ServerNestedList.class, "server");
        assertEquals(2, server.logs.size());
        assertEquals("DEBUG", server.logs.get(0).level);
        assertEquals(20, server.logs.get(0).size);
        assertEquals("WARN", server.logs.get(1).level);
        assertEquals(100, server.logs.get(1).size);
    }

    @Test
    void nestedGroupConversionProblems() {
        ServerNested template = new ServerNested();
        template.log = new Log();
        template.log.level = "DEBUG";
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.log.size", "not-a-number"));
        assertThrows(ConfigValidationException.class, builder::build);
    }

    @Test
    void containers() {
        ServerContainers template = new ServerContainers();
        template.aliases = List.of("template", "a,b");
        template.labels = Map.of("env", "template", "a.b", "dotted");
        template.description = Optional.of("template");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerContainers.class, "server", template)
                .build();

        ServerContainers server = config.getConfigMapping(ServerContainers.class, "server");
        assertEquals(List.of("template", "a,b"), server.aliases);
        assertEquals(Map.of("env", "template", "a.b", "dotted"), server.labels);
        assertEquals(Optional.of("template"), server.description);

        config = new SmallRyeConfigBuilder()
                .withMapping(ServerContainers.class, "server", template)
                .withSources(config("server.aliases", "a,b", "server.description", "config"))
                .build();
        server = config.getConfigMapping(ServerContainers.class, "server");
        assertEquals(List.of("a", "b"), server.aliases);
        assertEquals(Optional.of("config"), server.description);
    }

    @Test
    void keepsTemplateValuesWithConverter() {
        ServerConverter template = new ServerConverter();
        template.port = new ServerPort("9000");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerConverter.class, "server", template)
                .build();
        assertEquals(9000, config.getConfigMapping(ServerConverter.class, "server").port.port);

        config = new SmallRyeConfigBuilder()
                .withMapping(ServerConverter.class, "server", template)
                .withSources(config("server.port", "8080"))
                .build();
        assertEquals(8080, config.getConfigMapping(ServerConverter.class, "server").port.port);
    }

    @Test
    void conversionProblems() {
        Server template = new Server();
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.port", "not-a-number"));
        assertThrows(ConfigValidationException.class, builder::build);
    }

    @Test
    void unknownProperties() {
        Server template = new Server();
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.unknown", "value"));
        assertThrows(ConfigValidationException.class, builder::build);
    }

    @Test
    void doesNotChangeRegularMapping() {
        ServerDefaults template = new ServerDefaults();
        template.host = "template";
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerDefaults.class, "server", template)
                .withMapping(ServerDefaults.class, "other")
                .build();

        assertEquals("template", config.getConfigMapping(ServerDefaults.class, "server").host);
        ServerDefaults other = config.getConfigMapping(ServerDefaults.class, "other");
        assertFalse(other == template);
        assertEquals("localhost", other.host);
        assertEquals(8080, other.port);
    }

    @Test
    void dottedNames() {
        ServerDotted template = new ServerDotted();
        template.host = "template";
        template.port = 9000;
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerDotted.class, "server", template)
                .withSources(config("server.http.host", "localhost"))
                .build();

        ServerDotted server = config.getConfigMapping(ServerDotted.class, "server");
        assertEquals("localhost", server.host);
        assertEquals(9000, server.port);
    }

    @Test
    void envDashedNames() {
        ServerDashed template = new ServerDashed();
        template.hostName = "template";
        template.log = new LogDashed();
        template.log.maxSize = 10;
        template.log.level = "DEBUG";
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerDashed.class, "server", template)
                .withSources(new EnvConfigSource(Map.of("SERVER_HOST_NAME", "env", "SERVER_LOG_MAX_SIZE", "20"), 300))
                .build();

        ServerDashed server = config.getConfigMapping(ServerDashed.class, "server");
        assertEquals("env", server.hostName);
        assertEquals(20, server.log.maxSize);
        assertEquals("DEBUG", server.log.level);
    }

    @Test
    void expressionProblems() {
        Server template = new Server();
        template.host = "template";
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.host", "${undefined}"));
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, builder::build);
        assertTrue(exception.getProblem(0).getMessage().contains("SRCFG00011"));
    }

    @Test
    void nestedGroupExpressionProblems() {
        ServerNested template = new ServerNested();
        template.host = "template";
        template.log = new Log();
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(ServerNested.class, "server", template)
                .withSources(config("server.log.level", "${undefined}"));
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, builder::build);
        assertTrue(exception.getProblem(0).getMessage().contains("SRCFG00011"));
    }

    @Test
    void expressionWithDefault() {
        Server template = new Server();
        template.host = "template";
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.host", "${undefined:localhost}"))
                .build();

        assertEquals("localhost", config.getConfigMapping(Server.class, "server").host);
    }

    @Test
    void emptyValueProblems() {
        Server template = new Server();
        template.port = 9000;
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server", template)
                .withSources(config("server.port", ""));
        assertThrows(ConfigValidationException.class, builder::build);
    }

    @Test
    void mergesTemplateMap() {
        ServerMaps template = new ServerMaps();
        template.labels = Map.of("env", "template", "region", "template");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerMaps.class, "server", template)
                .withSources(config("server.labels.region", "eu", "server.labels.zone", "a"))
                .build();

        ServerMaps server = config.getConfigMapping(ServerMaps.class, "server");
        assertEquals(Map.of("env", "template", "region", "eu", "zone", "a"), server.labels);
    }

    @Test
    void createsTemplateMapWithConfiguration() {
        ServerMaps template = new ServerMaps();
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerMaps.class, "server", template)
                .withSources(config("server.labels.region", "eu"))
                .build();

        ServerMaps server = config.getConfigMapping(ServerMaps.class, "server");
        assertEquals(Map.of("region", "eu"), server.labels);
    }

    @Test
    void keepsFinalMaps() {
        ServerFinalMaps template = new ServerFinalMaps();
        template.labels.put("env", "template");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerFinalMaps.class, "server", template)
                .withSources(config("server.labels.region", "eu"))
                .build();

        // a final field is never populated
        ServerFinalMaps server = config.getConfigMapping(ServerFinalMaps.class, "server");
        assertSame(template.labels, server.labels);
        assertEquals(Map.of("env", "template"), server.labels);
    }

    @Test
    void mergesTemplateNestedGroupMap() {
        ServerNestedMap template = new ServerNestedMap();
        Log one = new Log();
        one.level = "DEBUG";
        one.size = 10;
        template.servers = new HashMap<>(Map.of("one", one));
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNestedMap.class, "server", template)
                .withSources(config("server.servers.one.size", "20", "server.servers.two.level", "WARN"))
                .build();

        ServerNestedMap server = config.getConfigMapping(ServerNestedMap.class, "server");
        assertEquals(2, server.servers.size());
        assertEquals("DEBUG", server.servers.get("one").level);
        assertEquals(20, server.servers.get("one").size);
        assertEquals("WARN", server.servers.get("two").level);
        assertEquals(100, server.servers.get("two").size);
    }

    @Test
    void mergesTemplateNestedGroupMaps() {
        ServerNestedMaps template = new ServerNestedMaps();
        template.maps = new ServerMaps();
        template.maps.labels = Map.of("env", "template");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerNestedMaps.class, "server", template)
                .withSources(config("server.maps.labels.region", "eu"))
                .build();

        ServerNestedMaps server = config.getConfigMapping(ServerNestedMaps.class, "server");
        assertEquals(Map.of("env", "template", "region", "eu"), server.maps.labels);
    }

    private static PropertiesConfigSource config(String... keyValues) {
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(keyValues[i], keyValues[i + 1]);
        }
        return new PropertiesConfigSource(properties, "test", 100);
    }

    public static class Server {
        public String host;
        public int port;
        private boolean ssl;
    }

    public static class ServerDefaults {
        public String host = "localhost";
        public int port = 8080;
    }

    public static class ServerFinal {
        public final String id;
        public String host;

        public ServerFinal(final String id) {
            this.id = id;
        }
    }

    public static class ServerNested {
        public String host;
        public Log log;
    }

    public static class Log {
        public String level;
        public int size = 100;
    }

    public static class ServerFinalNested {
        public final Log log = new Log();
    }

    public static class ServerPrivateNested {
        private Log log = new Log();
    }

    public static class ServerOptionalNested {
        public Optional<Log> log;
    }

    public static class ServerNestedList {
        public List<Log> logs;
    }

    public static class Root {
        public ServerNested server;
    }

    public static class ServerDotted {
        @WithName("http.host")
        public String host;
        @WithName("http.port")
        public int port;
    }

    public static class ServerDashed {
        @WithName("host-name")
        public String hostName;
        public LogDashed log;
    }

    public static class LogDashed {
        @WithName("max-size")
        public int maxSize;
        public String level;
    }

    public static class ServerMaps {
        public Map<String, String> labels;
    }

    public static class ServerFinalMaps {
        public final Map<String, String> labels = new HashMap<>();
    }

    public static class ServerNestedMap {
        public Map<String, Log> servers;
    }

    public static class ServerNestedMaps {
        private ServerMaps maps;
    }

    public static class ServerContainers {
        public List<String> aliases;
        public Map<String, String> labels;
        public Optional<String> description;
    }

    public static class ServerConverter {
        @WithConverter(ServerPortConverter.class)
        public ServerPort port;
    }

    public static class ServerPort {
        final int port;

        public ServerPort(final String port) {
            this.port = Integer.parseInt(port);
        }

        @Override
        public String toString() {
            return Integer.toString(port);
        }
    }

    public static class ServerPortConverter implements Converter<ServerPort> {
        @Override
        public ServerPort convert(final String value) {
            return new ServerPort(value);
        }
    }

    @ConfigMapping(prefix = "server")
    public interface ServerInterface {
        String host();
    }
}
