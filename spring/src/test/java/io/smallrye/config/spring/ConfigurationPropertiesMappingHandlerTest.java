package io.smallrye.config.spring;

import static io.smallrye.config.ConfigMappingLoader.getGeneratedConfigClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingHandler.ConfigMappingInterfaceHandler;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class ConfigurationPropertiesMappingHandlerTest {
    @Test
    void configurationProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerProperties.class)
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue("server.port", "8080")
                .build();

        ServerProperties server = config.getConfigMapping(ServerProperties.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void configurationPropertiesValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerPropertiesValue.class)
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue("server.port", "8080")
                .build();

        ServerPropertiesValue server = config.getConfigMapping(ServerPropertiesValue.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void configurationPropertiesNoPrefix() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NoPrefixProperties.class)
                .withDefaultValue("host", "localhost")
                .build();

        NoPrefixProperties properties = config.getConfigMapping(NoPrefixProperties.class);
        assertEquals("localhost", properties.host);
    }

    @Test
    void configurationPropertiesWithDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DefaultsProperties.class)
                .build();

        DefaultsProperties properties = config.getConfigMapping(DefaultsProperties.class);
        assertEquals("localhost", properties.host);
        assertEquals(8080, properties.port);
    }

    @Test
    void nameAnnotation() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NameProperties.class)
                .withDefaultValue("server.hostname", "localhost")
                .withDefaultValue("server.port", "8080")
                .build();

        NameProperties properties = config.getConfigMapping(NameProperties.class);
        assertEquals("localhost", properties.host);
        assertEquals(8080, properties.port);
    }

    @Test
    void kebabCaseNaming() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(KebabCaseProperties.class)
                .withDefaultValue("app.max-attempts", "5")
                .withDefaultValue("app.backoff-millis", "1000")
                .build();

        KebabCaseProperties properties = config.getConfigMapping(KebabCaseProperties.class);
        assertEquals(5, properties.maxAttempts);
        assertEquals(1000L, properties.backoffMillis);
    }

    @Test
    void multipleConfigurationProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(AppProperties.class)
                .withMapping(ServerProperties.class)
                .withDefaultValue("app.name", "my-app")
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue("server.port", "8080")
                .build();

        AppProperties app = config.getConfigMapping(AppProperties.class);
        assertEquals("my-app", app.name);

        ServerProperties server = config.getConfigMapping(ServerProperties.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void optionalProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(OptionalProperties.class)
                .withDefaultValue("app.name", "my-app")
                .build();

        OptionalProperties properties = config.getConfigMapping(OptionalProperties.class);
        assertEquals("my-app", properties.name);
        assertEquals(Optional.empty(), properties.description);
    }

    @Test
    void listProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ListProperties.class)
                .withDefaultValue("app.names", "a,b,c")
                .build();

        ListProperties properties = config.getConfigMapping(ListProperties.class);
        assertEquals(List.of("a", "b", "c"), properties.names);
    }

    @Test
    void ignoreUnmappedProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerProperties.class)
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue("server.port", "8080")
                .withDefaultValue("server.unmapped", "value")
                .build();

        ServerProperties server = config.getConfigMapping(ServerProperties.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void nestedProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedAppProperties.class)
                .withDefaultValue("app.name", "my-app")
                .withDefaultValue("app.server.host", "localhost")
                .withDefaultValue("app.server.port", "8080")
                .build();

        NestedAppProperties app = config.getConfigMapping(NestedAppProperties.class);
        assertEquals("my-app", app.name);
        assertEquals("localhost", app.server.host);
        assertEquals(8080, app.server.port);
    }

    @Test
    void deeplyNestedProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DeepNestedProperties.class)
                .withDefaultValue("app.name", "my-app")
                .withDefaultValue("app.database.host", "db-host")
                .withDefaultValue("app.database.pool.min-size", "5")
                .withDefaultValue("app.database.pool.max-size", "20")
                .build();

        DeepNestedProperties app = config.getConfigMapping(DeepNestedProperties.class);
        assertEquals("my-app", app.name);
        assertEquals("db-host", app.database.host);
        assertEquals(5, app.database.pool.minSize);
        assertEquals(20, app.database.pool.maxSize);
    }

    @Test
    void listOfNestedObjects() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NodeListProperties.class)
                .withDefaultValue("cluster.nodes[0].name", "main")
                .withDefaultValue("cluster.nodes[0].base-url", "http://localhost:11434")
                .withDefaultValue("cluster.nodes[1].name", "gpu-server")
                .withDefaultValue("cluster.nodes[1].base-url", "http://gpu-host:11435")
                .build();

        NodeListProperties props = config.getConfigMapping(NodeListProperties.class);
        assertEquals(2, props.nodes.size());
        assertEquals("main", props.nodes.get(0).name);
        assertEquals("http://localhost:11434", props.nodes.get(0).baseUrl);
        assertEquals("gpu-server", props.nodes.get(1).name);
        assertEquals("http://gpu-host:11435", props.nodes.get(1).baseUrl);
    }

    @Test
    void mapOfNestedObjects() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ToolGroupProperties.class)
                .withDefaultValue("tools.groups.search.description", "Search tools")
                .withDefaultValue("tools.groups.search.provider", "embabel")
                .withDefaultValue("tools.groups.code.description", "Code tools")
                .withDefaultValue("tools.groups.code.provider", "custom")
                .build();

        ToolGroupProperties props = config.getConfigMapping(ToolGroupProperties.class);
        assertEquals(2, props.groups.size());
        assertEquals("Search tools", props.groups.get("search").description);
        assertEquals("embabel", props.groups.get("search").provider);
        assertEquals("Code tools", props.groups.get("code").description);
        assertEquals("custom", props.groups.get("code").provider);
    }

    @Test
    void allMappingMetadata() {
        Set<String> names = getGeneratedConfigClasses(ToolGroupProperties.class)
                .stream()
                .map(GeneratedConfigClass::getClassName)
                .collect(Collectors.toSet());

        String test = "io.smallrye.config.spring.ConfigurationPropertiesMappingHandlerTest";
        assertTrue(names.contains(test + "$ToolGroupProperties$$CMClass"));
        assertTrue(names.contains(test + "$ToolGroupProperties$$CMClass$$CMImpl"));
        assertTrue(names.contains(test + "$ToolGroupProperties$GroupConfig$$CMClass"));
        assertTrue(names.contains(test + "$ToolGroupProperties$GroupConfig$$CMClass$$CMImpl"));
    }

    @Test
    void bridgeHandlerAfterMapping() {
        new SmallRyeConfigBuilder()
                .withMapping(BridgeFromMapping.class)
                .withDefaultValue("bridge-mapping.host", "localhost")
                .build();

        Set<GeneratedConfigClass> generated = getGeneratedConfigClasses(BridgeFromMapping.class);
        GeneratedConfigClass configClass = generated.stream()
                .filter(g -> g.getParent().equals(BridgeFromMapping.class))
                .findFirst()
                .orElseThrow();
        GeneratedConfigClass bridge = generated.stream()
                .filter(g -> g.getParent().equals(configClass.getInterfaceType()))
                .findFirst()
                .orElseThrow();

        assertSame(ConfigMappingInterfaceHandler.CONFIG_MAPPING, bridge.getHandler());
        assertNotSame(configClass.getHandler(), bridge.getHandler());
    }

    @ConfigurationProperties(prefix = "bridge-mapping")
    public static class BridgeFromMapping {
        public String host;
    }

    @Test
    void bridgeHandlerAfterProperties() {
        ConfigMappings.getProperties(ConfigClass.configClass(BridgeFromProperties.class));

        Set<GeneratedConfigClass> generated = getGeneratedConfigClasses(BridgeFromProperties.class);
        GeneratedConfigClass configClass = generated.stream()
                .filter(g -> g.getParent().equals(BridgeFromProperties.class))
                .findFirst()
                .orElseThrow();
        GeneratedConfigClass bridge = generated.stream()
                .filter(g -> g.getParent().equals(configClass.getInterfaceType()))
                .findFirst()
                .orElseThrow();

        assertSame(ConfigMappingInterfaceHandler.CONFIG_MAPPING, bridge.getHandler());
        assertNotSame(configClass.getHandler(), bridge.getHandler());
    }

    @ConfigurationProperties(prefix = "bridge-properties")
    public static class BridgeFromProperties {
        public String host;
    }

    @Test
    void mapRequired() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapConfig.class)
                .withDefaultValue("config.spring.map.one", "one")
                .withDefaultValue("config.spring.map.two", "two")
                .withDefaultValue("config.spring.servers.one.host", "localhost")
                .withDefaultValue("config.spring.servers.one.port", "8080")
                .withDefaultValue("config.spring.required.one", "one")
                .build();

        MapConfig mapConfig = config.getConfigMapping(MapConfig.class);
    }

    @ConfigurationProperties("config.spring")
    public static final class MapConfig {
        private Map<String, String> map = new HashMap<>();
        private Map<String, Server> servers = new HashMap<>();

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public Map<String, Server> getServers() {
            return servers;
        }

        public void setServers(Map<String, Server> servers) {
            this.servers = servers;
        }
    }

    public static final class Server {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public Server setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return port;
        }

        public Server setPort(int port) {
            this.port = port;
            return this;
        }
    }

    @ConfigurationProperties(prefix = "server")
    public static class ServerProperties {
        public String host;
        public int port;
    }

    @ConfigurationProperties(value = "server")
    public static class ServerPropertiesValue {
        public String host;
        public int port;
    }

    @ConfigurationProperties
    public static class NoPrefixProperties {
        public String host;
    }

    @ConfigurationProperties(prefix = "app")
    public static class DefaultsProperties {
        public String host = "localhost";
        public int port = 8080;
    }

    @ConfigurationProperties(prefix = "server")
    public static class NameProperties {
        @Name("hostname")
        public String host;
        public int port;
    }

    @ConfigurationProperties(prefix = "app")
    public static class KebabCaseProperties {
        public int maxAttempts;
        public long backoffMillis;
    }

    @ConfigurationProperties(prefix = "app")
    public static class AppProperties {
        public String name;
    }

    @ConfigurationProperties(prefix = "app")
    public static class OptionalProperties {
        public String name;
        public Optional<String> description;
    }

    @ConfigurationProperties(prefix = "app")
    public static class ListProperties {
        public List<String> names;
    }

    @ConfigurationProperties(prefix = "app")
    public static class NestedAppProperties {
        public String name;
        public Server server;

        public static class Server {
            public String host;
            public int port;
        }
    }

    @ConfigurationProperties(prefix = "app")
    public static class DeepNestedProperties {
        public String name;
        public Database database;

        public static class Database {
            public String host;
            public Pool pool;

            public static class Pool {
                public int minSize;
                public int maxSize;
            }
        }
    }

    @ConfigurationProperties(prefix = "cluster")
    public static class NodeListProperties {
        public List<NodeConfig> nodes;

        public static class NodeConfig {
            public String name;
            public String baseUrl;
        }
    }

    @ConfigurationProperties(prefix = "tools")
    public static class ToolGroupProperties {
        public Map<String, GroupConfig> groups;

        public static class GroupConfig {
            public String description;
            public String provider;
        }
    }

    @Test
    void namingStrategyFromConfigMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(VerbatimProperties.class)
                .withDefaultValue("verbatim.maxAttempts", "5")
                .withDefaultValue("verbatim.backoffMillis", "1000")
                .build();

        VerbatimProperties properties = config.getConfigMapping(VerbatimProperties.class);
        assertEquals(5, properties.maxAttempts);
        assertEquals(1000L, properties.backoffMillis);

        // the class carries @ConfigMapping, but it is still handled by the Spring handler
        GeneratedConfigClass generated = getGeneratedConfigClasses(VerbatimProperties.class).stream()
                .filter(g -> g.getParent().equals(VerbatimProperties.class))
                .findFirst()
                .orElseThrow();
        assertInstanceOf(ConfigurationPropertiesMappingHandler.class, generated.getHandler());
    }

    @ConfigurationProperties(prefix = "verbatim")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class VerbatimProperties {
        public int maxAttempts;
        public long backoffMillis;
    }

    @Test
    void nestedInheritsEnclosingNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedNamingProperties.class)
                .withDefaultValue("nested-naming.appName", "my-app")
                .withDefaultValue("nested-naming.server.baseUrl", "http://localhost")
                .withDefaultValue("nested-naming.pool.max_size", "20")
                .build();

        NestedNamingProperties properties = config.getConfigMapping(NestedNamingProperties.class);
        assertEquals("my-app", properties.appName);
        assertEquals("http://localhost", properties.server.baseUrl);
        assertEquals(20, properties.pool.maxSize);
    }

    @ConfigurationProperties(prefix = "nested-naming")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class NestedNamingProperties {
        public String appName;
        public Server server;
        public Pool pool;

        // inherits VERBATIM from the enclosing class
        public static class Server {
            public String baseUrl;
        }

        // declares its own strategy
        @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
        public static class Pool {
            public int maxSize;
        }
    }

    @Test
    void ignoreUnknownFieldsFalse() {
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withMapping(StrictProperties.class)
                .withDefaultValue("strict.host", "localhost")
                .withDefaultValue("strict.unmapped", "value")
                .build());
        assertTrue(exception.getMessage().contains("strict.unmapped"));

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(StrictProperties.class)
                .withDefaultValue("strict.host", "localhost")
                .withDefaultValue("other.unmapped", "value")
                .build();
        assertEquals("localhost", config.getConfigMapping(StrictProperties.class).host);
    }

    @ConfigurationProperties(prefix = "strict", ignoreUnknownFields = false)
    public static class StrictProperties {
        public String host;
    }

    @Test
    void snakeCaseNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SnakeCaseProperties.class)
                .withDefaultValue("snake.max_attempts", "5")
                .withDefaultValue("snake.backoff_millis", "1000")
                .build();

        SnakeCaseProperties properties = config.getConfigMapping(SnakeCaseProperties.class);
        assertEquals(5, properties.maxAttempts);
        assertEquals(1000L, properties.backoffMillis);
    }

    @ConfigurationProperties(prefix = "snake")
    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    public static class SnakeCaseProperties {
        public int maxAttempts;
        public long backoffMillis;
    }

    @Test
    void nameAnnotationOverridesNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NameWithStrategyProperties.class)
                .withDefaultValue("name-strategy.hostName", "localhost")
                .withDefaultValue("name-strategy.listen_port", "8080")
                .build();

        NameWithStrategyProperties properties = config.getConfigMapping(NameWithStrategyProperties.class);
        assertEquals("localhost", properties.host);
        assertEquals(8080, properties.listenPort);
    }

    @ConfigurationProperties(prefix = "name-strategy")
    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    public static class NameWithStrategyProperties {
        // explicit name is used as is, the naming strategy does not apply
        @Name("hostName")
        public String host;
        public int listenPort;
    }

    @Test
    void deeplyNestedInheritsOutermostNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DeepNamingProperties.class)
                .withDefaultValue("deep-naming.appName", "my-app")
                .withDefaultValue("deep-naming.database.jdbcUrl", "jdbc:h2:mem:test")
                .withDefaultValue("deep-naming.database.pool.minSize", "5")
                .withDefaultValue("deep-naming.database.pool.maxSize", "20")
                .build();

        DeepNamingProperties properties = config.getConfigMapping(DeepNamingProperties.class);
        assertEquals("my-app", properties.appName);
        assertEquals("jdbc:h2:mem:test", properties.database.jdbcUrl);
        assertEquals(5, properties.database.pool.minSize);
        assertEquals(20, properties.database.pool.maxSize);
    }

    @ConfigurationProperties(prefix = "deep-naming")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class DeepNamingProperties {
        public String appName;
        public Database database;

        public static class Database {
            public String jdbcUrl;
            public Pool pool;

            // two levels down, still resolves to the outermost VERBATIM
            public static class Pool {
                public int minSize;
                public int maxSize;
            }
        }
    }

    @Test
    void collectionElementsInheritNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(CollectionNamingProperties.class)
                .withDefaultValue("collection-naming.nodes[0].nodeName", "main")
                .withDefaultValue("collection-naming.nodes[0].baseUrl", "http://localhost:11434")
                .withDefaultValue("collection-naming.groups.search.groupDescription", "Search tools")
                .build();

        CollectionNamingProperties properties = config.getConfigMapping(CollectionNamingProperties.class);
        assertEquals(1, properties.nodes.size());
        assertEquals("main", properties.nodes.get(0).nodeName);
        assertEquals("http://localhost:11434", properties.nodes.get(0).baseUrl);
        assertEquals(1, properties.groups.size());
        assertEquals("Search tools", properties.groups.get("search").groupDescription);
    }

    @ConfigurationProperties(prefix = "collection-naming")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class CollectionNamingProperties {
        public List<Node> nodes;
        public Map<String, Group> groups;

        public static class Node {
            public String nodeName;
            public String baseUrl;
        }

        public static class Group {
            public String groupDescription;
        }
    }

    @Test
    void siblingClassDoesNotInheritNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SiblingNamingProperties.class)
                .withDefaultValue("sibling-naming.appName", "my-app")
                // not enclosed by the annotated class, so it falls back to kebab-case
                .withDefaultValue("sibling-naming.endpoint.base-url", "http://localhost")
                // declares its own strategy
                .withDefaultValue("sibling-naming.snakeEndpoint.base_url", "http://snake")
                .build();

        SiblingNamingProperties properties = config.getConfigMapping(SiblingNamingProperties.class);
        assertEquals("my-app", properties.appName);
        assertEquals("http://localhost", properties.endpoint.baseUrl);
        assertEquals("http://snake", properties.snakeEndpoint.baseUrl);
    }

    @ConfigurationProperties(prefix = "sibling-naming")
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class SiblingNamingProperties {
        public String appName;
        public Endpoint endpoint;
        public SnakeEndpoint snakeEndpoint;
    }

    public static class Endpoint {
        public String baseUrl;
    }

    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    public static class SnakeEndpoint {
        public String baseUrl;
    }

    @Test
    void ignoreUnknownFieldsFalseUsesNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(StrictVerbatimProperties.class)
                .withDefaultValue("strict-verbatim.maxAttempts", "5")
                .build();
        assertEquals(5, config.getConfigMapping(StrictVerbatimProperties.class).maxAttempts);

        // the kebab-case name is not mapped under VERBATIM, so it is reported as unknown
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder()
                .withMapping(StrictVerbatimProperties.class)
                .withDefaultValue("strict-verbatim.maxAttempts", "5")
                .withDefaultValue("strict-verbatim.max-attempts", "5")
                .build());
        assertTrue(exception.getMessage().contains("strict-verbatim.max-attempts"));
    }

    @ConfigurationProperties(prefix = "strict-verbatim", ignoreUnknownFields = false)
    @ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
    public static class StrictVerbatimProperties {
        public int maxAttempts;
    }

    @Test
    void handlerResolvesNamingStrategyFromEnclosingClasses() {
        ConfigurationPropertiesMappingHandler handler = new ConfigurationPropertiesMappingHandler();

        assertEquals(NamingStrategy.KEBAB_CASE, handler.getNamingStrategy(ServerProperties.class));
        assertEquals(NamingStrategy.KEBAB_CASE, handler.getNamingStrategy(NestedAppProperties.Server.class));
        assertEquals(NamingStrategy.VERBATIM, handler.getNamingStrategy(VerbatimProperties.class));
        assertEquals(NamingStrategy.VERBATIM, handler.getNamingStrategy(DeepNamingProperties.Database.Pool.class));
        assertEquals(NamingStrategy.SNAKE_CASE, handler.getNamingStrategy(NestedNamingProperties.Pool.class));
        assertEquals(NamingStrategy.KEBAB_CASE, handler.getNamingStrategy(Endpoint.class));
        assertEquals(NamingStrategy.SNAKE_CASE, handler.getNamingStrategy(SnakeEndpoint.class));
    }
}
