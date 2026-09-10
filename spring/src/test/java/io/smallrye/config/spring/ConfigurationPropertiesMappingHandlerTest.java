package io.smallrye.config.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
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
        Set<String> names = ConfigMappingLoader.getGeneratedConfigClasses(ToolGroupProperties.class)
                .stream()
                .map(GeneratedConfigClass::getClassName)
                .collect(Collectors.toSet());

        assertTrue(names.contains("io.smallrye.config.spring.ToolGroupProperties-1087945751I"));
        assertTrue(names.contains("io.smallrye.config.spring.ToolGroupProperties-1087945751I$$CMImpl"));
        assertTrue(names.contains("io.smallrye.config.spring.GroupConfig648864326I"));
        assertTrue(names.contains("io.smallrye.config.spring.GroupConfig648864326I$$CMImpl"));
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
}
