package io.smallrye.config.source.yaml;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

class YamlConfigMappingTest {
    @Test
    void yamlConfigMapping() {
        String yaml = "proxies:\n" +
                "  - type: mssql\n" +
                "    name: first\n" +
                "    input:\n" +
                "      pool:\n" +
                "        max_pool_size: 100\n" +
                "        expires_in_seconds: 60\n" +
                "      mssql:\n" +
                "        server: 192.168.1.2\n" +
                "        port: 1434\n" +
                "        user: 'test'\n" +
                "        password: 'test'\n" +
                "    read:\n" +
                "      pool:\n" +
                "        max_pool_size: 100\n" +
                "        expires_in_seconds: 60\n" +
                "      regex: '(?i)^\\s*select.*'\n" +
                "      mssql:\n" +
                "        server: 127.0.0.1\n" +
                "        port: 1433\n" +
                "        user: 'sa'\n" +
                "        password: 'Test!1234'\n" +
                "    write:\n" +
                "      pool:\n" +
                "        max_pool_size: 100\n" +
                "        expires_in_seconds: 60\n" +
                "      mssql:\n" +
                "        server: 127.0.0.1\n" +
                "        port: 1433\n" +
                "        user: 'sa'\n" +
                "        password: 'Test!1234'\n" +
                "  - type: mssql\n" +
                "    name: second\n" +
                "    input:\n" +
                "      mssql:\n" +
                "        server: 192.168.1.2\n" +
                "        port: 1435\n" +
                "        user: 'test'\n" +
                "        password: 'test'\n" +
                "      pool:\n" +
                "        max_pool_size: 0\n" +
                "        expires_in_seconds: 0\n" +
                "      encryption:\n" +
                "        level: 'SUPPORTED'\n" +
                "        keystore:\n" +
                "          location: 'my-location'\n" +
                "    read:\n" +
                "      regex: '(?i)^\\s*select.*'\n" +
                "      mssql:\n" +
                "        server: 127.0.0.1\n" +
                "        port: 1433\n" +
                "        user: 'sa'\n" +
                "        password: 'Test!1234'\n" +
                "    write:\n" +
                "      mssql:\n" +
                "        server: 127.0.0.1\n" +
                "        port: 1433\n" +
                "        user: 'sa'\n" +
                "        password: 'Test!1234'\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Proxies.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        Proxies proxies = config.getConfigMapping(Proxies.class);

        assertFalse(proxies.allProxies().isEmpty());
        assertEquals(SQLProxyConfig.DatabaseType.mssql, proxies.allProxies().get(0).type());
        assertEquals("mssql", proxies.allProxies().get(0).typeAsString());
        assertEquals("first", proxies.allProxies().get(0).name());
        assertEquals(-1, proxies.allProxies().get(0).maxAsyncThreads());
        assertEquals(-1, proxies.allProxies().get(0).maxQueuedThreads());

        assertTrue(proxies.allProxies().get(0).input().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).input().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).input().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).input().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).input().mssql().isPresent());
        assertEquals("192.168.1.2", proxies.allProxies().get(0).input().mssql().get().server());
        assertEquals(1434, proxies.allProxies().get(0).input().mssql().get().port());
        assertEquals("test", proxies.allProxies().get(0).input().mssql().get().user());
        assertEquals("test", proxies.allProxies().get(0).input().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().timeout().isPresent());
        assertFalse(proxies.allProxies().get(0).input().regex().isPresent());

        assertTrue(proxies.allProxies().get(0).read().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).read().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).read().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).read().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).read().mssql().isPresent());
        assertEquals("127.0.0.1", proxies.allProxies().get(0).read().mssql().get().server());
        assertEquals(1433, proxies.allProxies().get(0).read().mssql().get().port());
        assertEquals("sa", proxies.allProxies().get(0).read().mssql().get().user());
        assertEquals("Test!1234", proxies.allProxies().get(0).read().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().timeout().isPresent());
        assertTrue(proxies.allProxies().get(0).read().regex().isPresent());
        assertEquals("(?i)^\\s*select.*", proxies.allProxies().get(0).read().regex().get());

        assertTrue(proxies.allProxies().get(0).write().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).write().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).write().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).write().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).write().mssql().isPresent());
        assertEquals("127.0.0.1", proxies.allProxies().get(0).write().mssql().get().server());
        assertEquals(1433, proxies.allProxies().get(0).write().mssql().get().port());
        assertEquals("sa", proxies.allProxies().get(0).write().mssql().get().user());
        assertEquals("Test!1234", proxies.allProxies().get(0).write().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().timeout().isPresent());
        assertFalse(proxies.allProxies().get(0).write().regex().isPresent());

        assertEquals(SQLProxyConfig.DatabaseType.mssql, proxies.allProxies().get(1).type());
        assertEquals("mssql", proxies.allProxies().get(1).typeAsString());
        assertEquals("second", proxies.allProxies().get(1).name());
        assertEquals(-1, proxies.allProxies().get(1).maxAsyncThreads());
        assertEquals(-1, proxies.allProxies().get(1).maxQueuedThreads());

        assertTrue(proxies.allProxies().get(1).input().pool().isPresent());
        assertEquals(0, proxies.allProxies().get(1).input().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(1).input().pool().get().maxPoolSize().isPresent());
        assertEquals(0, proxies.allProxies().get(1).input().pool().get().maxPoolSize().getAsInt());

        assertTrue(proxies.allProxies().get(1).input().encryption().isPresent());
        assertEquals("SUPPORTED", proxies.allProxies().get(1).input().encryption().get().levelAsString());
        assertEquals("TLS", proxies.allProxies().get(1).input().encryption().get().sslProtocol());
        assertTrue(proxies.allProxies().get(1).input().encryption().get().keystore().isPresent());
        assertEquals("my-location", proxies.allProxies().get(1).input().encryption().get().keystore().get().location());
        assertEquals("PCKS12", proxies.allProxies().get(1).input().encryption().get().keystore().get().format());
    }

    @Test
    void jsonConfigMapping() {
        String json = "{\n" +
                "  \"proxies\": [\n" +
                "    {\n" +
                "      \"type\": \"mssql\",\n" +
                "      \"name\": \"first\",\n" +
                "      \"input\": {\n" +
                "        \"pool\": {\n" +
                "          \"max_pool_size\": 100,\n" +
                "          \"expires_in_seconds\": 60\n" +
                "        },\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"192.168.1.2\",\n" +
                "          \"port\": 1434,\n" +
                "          \"user\": \"test\",\n" +
                "          \"password\": \"test\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"read\": {\n" +
                "        \"pool\": {\n" +
                "          \"max_pool_size\": 100,\n" +
                "          \"expires_in_seconds\": 60\n" +
                "        },\n" +
                "        \"regex\": \"(?i)^\\\\s*select.*\",\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"127.0.0.1\",\n" +
                "          \"port\": 1433,\n" +
                "          \"user\": \"sa\",\n" +
                "          \"password\": \"Test!1234\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"write\": {\n" +
                "        \"pool\": {\n" +
                "          \"max_pool_size\": 100,\n" +
                "          \"expires_in_seconds\": 60\n" +
                "        },\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"127.0.0.1\",\n" +
                "          \"port\": 1433,\n" +
                "          \"user\": \"sa\",\n" +
                "          \"password\": \"Test!1234\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"mssql\",\n" +
                "      \"name\": \"second\",\n" +
                "      \"input\": {\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"192.168.1.2\",\n" +
                "          \"port\": 1435,\n" +
                "          \"user\": \"test\",\n" +
                "          \"password\": \"test\"\n" +
                "        },\n" +
                "        \"pool\": {\n" +
                "          \"max_pool_size\": 0,\n" +
                "          \"expires_in_seconds\": 0\n" +
                "        },\n" +
                "        \"encryption\": {\n" +
                "          \"level\": \"SUPPORTED\",\n" +
                "          \"keystore\": {\n" +
                "            \"location\": \"my-location\"\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"read\": {\n" +
                "        \"regex\": \"(?i)^\\\\s*select.*\",\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"127.0.0.1\",\n" +
                "          \"port\": 1433,\n" +
                "          \"user\": \"sa\",\n" +
                "          \"password\": \"Test!1234\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"write\": {\n" +
                "        \"mssql\": {\n" +
                "          \"server\": \"127.0.0.1\",\n" +
                "          \"port\": 1433,\n" +
                "          \"user\": \"sa\",\n" +
                "          \"password\": \"Test!1234\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Proxies.class)
                .withSources(new YamlConfigSource("json", json))
                .build();

        Proxies proxies = config.getConfigMapping(Proxies.class);

        assertFalse(proxies.allProxies().isEmpty());
        assertEquals(SQLProxyConfig.DatabaseType.mssql, proxies.allProxies().get(0).type());
        assertEquals("mssql", proxies.allProxies().get(0).typeAsString());
        assertEquals("first", proxies.allProxies().get(0).name());
        assertEquals(-1, proxies.allProxies().get(0).maxAsyncThreads());
        assertEquals(-1, proxies.allProxies().get(0).maxQueuedThreads());

        assertTrue(proxies.allProxies().get(0).input().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).input().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).input().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).input().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).input().mssql().isPresent());
        assertEquals("192.168.1.2", proxies.allProxies().get(0).input().mssql().get().server());
        assertEquals(1434, proxies.allProxies().get(0).input().mssql().get().port());
        assertEquals("test", proxies.allProxies().get(0).input().mssql().get().user());
        assertEquals("test", proxies.allProxies().get(0).input().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).input().mssql().get().timeout().isPresent());
        assertFalse(proxies.allProxies().get(0).input().regex().isPresent());

        assertTrue(proxies.allProxies().get(0).read().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).read().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).read().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).read().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).read().mssql().isPresent());
        assertEquals("127.0.0.1", proxies.allProxies().get(0).read().mssql().get().server());
        assertEquals(1433, proxies.allProxies().get(0).read().mssql().get().port());
        assertEquals("sa", proxies.allProxies().get(0).read().mssql().get().user());
        assertEquals("Test!1234", proxies.allProxies().get(0).read().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).read().mssql().get().timeout().isPresent());
        assertTrue(proxies.allProxies().get(0).read().regex().isPresent());
        assertEquals("(?i)^\\s*select.*", proxies.allProxies().get(0).read().regex().get());

        assertTrue(proxies.allProxies().get(0).write().pool().isPresent());
        assertEquals(60, proxies.allProxies().get(0).write().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(0).write().pool().get().maxPoolSize().isPresent());
        assertEquals(100, proxies.allProxies().get(0).write().pool().get().maxPoolSize().getAsInt());
        assertTrue(proxies.allProxies().get(0).write().mssql().isPresent());
        assertEquals("127.0.0.1", proxies.allProxies().get(0).write().mssql().get().server());
        assertEquals(1433, proxies.allProxies().get(0).write().mssql().get().port());
        assertEquals("sa", proxies.allProxies().get(0).write().mssql().get().user());
        assertEquals("Test!1234", proxies.allProxies().get(0).write().mssql().get().password());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().hostName().isPresent());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().database().isPresent());
        assertFalse(proxies.allProxies().get(0).write().mssql().get().timeout().isPresent());
        assertFalse(proxies.allProxies().get(0).write().regex().isPresent());

        assertEquals(SQLProxyConfig.DatabaseType.mssql, proxies.allProxies().get(1).type());
        assertEquals("mssql", proxies.allProxies().get(1).typeAsString());
        assertEquals("second", proxies.allProxies().get(1).name());
        assertEquals(-1, proxies.allProxies().get(1).maxAsyncThreads());
        assertEquals(-1, proxies.allProxies().get(1).maxQueuedThreads());

        assertTrue(proxies.allProxies().get(1).input().pool().isPresent());
        assertEquals(0, proxies.allProxies().get(1).input().pool().get().expiresInSeconds());
        assertTrue(proxies.allProxies().get(1).input().pool().get().maxPoolSize().isPresent());
        assertEquals(0, proxies.allProxies().get(1).input().pool().get().maxPoolSize().getAsInt());

        assertTrue(proxies.allProxies().get(1).input().encryption().isPresent());
        assertEquals("SUPPORTED", proxies.allProxies().get(1).input().encryption().get().levelAsString());
        assertEquals("TLS", proxies.allProxies().get(1).input().encryption().get().sslProtocol());
        assertTrue(proxies.allProxies().get(1).input().encryption().get().keystore().isPresent());
        assertEquals("my-location", proxies.allProxies().get(1).input().encryption().get().keystore().get().location());
        assertEquals("PCKS12", proxies.allProxies().get(1).input().encryption().get().keystore().get().format());
    }

    @ConfigMapping(prefix = "proxies")
    public interface Proxies {
        @WithParentName
        List<SQLProxyConfig> allProxies();
    }

    public interface SQLProxyConfig {
        @WithName("name")
        String name();

        enum DatabaseType {
            mssql,
            oracle,
            plsql;
        }

        @WithName("type")
        @WithDefault("mssql")
        String typeAsString();

        default DatabaseType type() {
            return DatabaseType.valueOf(typeAsString());
        }

        @WithName("max_async_threads")
        @WithDefault("-1")
        int maxAsyncThreads();

        @WithName("max_queued_threads")
        @WithDefault("-1")
        int maxQueuedThreads();

        @WithName("input")
        ConnectionConfig input();

        @WithName("read")
        ConnectionConfig read();

        @WithName("write")
        ConnectionConfig write();
    }

    interface ConnectionConfig {
        interface PoolConfig {
            String DEFAULT_EXPIRE_IN_SECONDS = "600";

            @WithName("max_pool_size")
            OptionalInt maxPoolSize();

            @WithName("expires_in_seconds")
            @WithDefault(DEFAULT_EXPIRE_IN_SECONDS)
            int expiresInSeconds();
        }

        interface MSSQLConfig {
            String DEFAULT_PORT = "1433";

            @ConfigMapping(prefix = "timeout")
            interface TimeoutConfig {
                String DEFAULT_LOGIN_TIMEOUT = "15";
                String DEFAULT_QUERY_TIMEOUT = "600";

                @WithName("pre_login_in_seconds")
                @WithDefault(DEFAULT_LOGIN_TIMEOUT)
                int preLogin();

                @WithName("login_in_seconds")
                @WithDefault(DEFAULT_LOGIN_TIMEOUT)
                int login();

                @WithName("query_in_seconds")
                @WithDefault(DEFAULT_QUERY_TIMEOUT)
                int query();
            }

            @WithName("database")
            Optional<String> database();

            @WithName("host_name")
            Optional<String> hostName();

            @WithName("server")
            String server();

            @WithName("port")
            @WithDefault(DEFAULT_PORT)
            int port();

            @WithName("user")
            String user();

            @WithName("password")
            String password();

            @WithName("timeout")
            Optional<TimeoutConfig> timeout();
        }

        interface EncryptionConfig {
            @WithName("level")
            @WithDefault("NOT_SUPPORTED")
            String levelAsString();

            @WithName("ssl_protocol")
            @WithDefault("TLS")
            String sslProtocol();

            interface KeystoreConfig {
                @WithName("location")
                String location();

                @WithName("format")
                @WithDefault("PCKS12")
                String format();

                @WithName("password")
                Optional<String> password();
            }

            @WithName("keystore")
            Optional<EncryptionConfig.KeystoreConfig> keystore();

            @WithName("truststore")
            Optional<EncryptionConfig.KeystoreConfig> truststore();
        }

        @WithName("encryption")
        Optional<EncryptionConfig> encryption();

        @WithName("mssql")
        Optional<MSSQLConfig> mssql();

        @WithName("pool")
        Optional<PoolConfig> pool();

        @WithName("regex")
        Optional<String> regex();
    }

    @Test
    void yamlListMaps() {
        String yaml = "app:\n" +
                "  config:\n" +
                "    - name: Bob\n" +
                "      foo: thing\n" +
                "      bar: false\n" +
                "    - name: Tim\n" +
                "      baz: stuff\n" +
                "      qux: 3";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapConfig.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        MapConfig mapping = config.getConfigMapping(MapConfig.class);
        assertEquals("Bob", mapping.config().get(0).get("name"));
        assertEquals("thing", mapping.config().get(0).get("foo"));
        assertEquals("false", mapping.config().get(0).get("bar"));
        assertEquals("Tim", mapping.config().get(1).get("name"));
        assertEquals("stuff", mapping.config().get(1).get("baz"));
        assertEquals("3", mapping.config().get(1).get("qux"));
    }

    @ConfigMapping(prefix = "app")
    interface MapConfig {
        List<Map<String, String>> config();
    }

    @Test
    void yamlMapGroupMap() {
        String yaml = "parent:\n" +
                "  goodchildren:\n" +
                "    child1:\n" +
                "      name: John\n" +
                "      attributes:\n" +
                "        somekey: somevalue\n" +
                "        anotherkey: anothervalue\n" +
                "    child2:\n" +
                "      name: James\n" +
                "      attributes:\n" +
                "        something: isbroken\n" +
                "  badchildren:\n" +
                "    child3:\n" +
                "      name: BadJohn\n" +
                "      attributes:\n" +
                "        somekeybad: somevaluebad\n" +
                "        anotherkeybad: anothervaluebad";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Parent.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        Parent mapping = config.getConfigMapping(Parent.class);
        assertEquals(2, mapping.goodChildren().size());
        assertEquals("John", mapping.goodChildren().get("child1").name());
        assertEquals(2, mapping.goodChildren().get("child1").attributes().size());
        assertEquals("somevalue", mapping.goodChildren().get("child1").attributes().get("somekey"));
        assertEquals("anothervalue", mapping.goodChildren().get("child1").attributes().get("anotherkey"));

        assertEquals("James", mapping.goodChildren().get("child2").name());
        assertEquals(1, mapping.goodChildren().get("child2").attributes().size());
        assertEquals("isbroken", mapping.goodChildren().get("child2").attributes().get("something"));
        assertEquals(1, mapping.badChildren().size());
        assertEquals("BadJohn", mapping.badChildren().get("child3").name());
        assertEquals(2, mapping.badChildren().get("child3").attributes().size());
        assertEquals("somevaluebad", mapping.badChildren().get("child3").attributes().get("somekeybad"));
        assertEquals("anothervaluebad", mapping.badChildren().get("child3").attributes().get("anotherkeybad"));
    }

    @ConfigMapping(prefix = "parent")
    public interface Parent {
        @WithName("goodchildren")
        Map<String, GrandChild> goodChildren();

        @WithName("badchildren")
        Map<String, GrandChild> badChildren();

        interface GrandChild {
            @WithName("name")
            String name();

            @WithName("attributes")
            Map<String, String> attributes();
        }
    }

    @Test
    void yamlMapCollections() {
        String yaml = "some:\n" +
                "  prop:\n" +
                "    value:    \n" +
                "      - 0.9\n" +
                "      - 0.99";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapListDouble.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        MapListDouble mapping = config.getConfigMapping(MapListDouble.class);
        assertEquals(0.9d, mapping.prop().get("value").get(0));
        assertEquals(0.99d, mapping.prop().get("value").get(1));
    }

    @ConfigMapping(prefix = "some")
    public interface MapListDouble {
        Map<String, List<Double>> prop();
    }

    @Test
    void yamlMapListsGroup() {
        String yaml = "channelsuite:\n" +
                "  permissions:\n" +
                "    anonymous:\n" +
                "      - \"p1\"\n" +
                "    internal-call:\n" +
                "      - \"p2\"\n" +
                "      - \"p3\"\n" +
                "    roles:\n" +
                "      user:\n" +
                "        - \"p1\"\n" +
                "      administrator:\n" +
                "        - \"p2\"\n" +
                "        - \"p3\"\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(PermissionsConfig.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        PermissionsConfig configMapping = config.getConfigMapping(PermissionsConfig.class);

        assertEquals("p1", configMapping.anonymous().get(0));
        assertEquals("p2", configMapping.internalCall().get(0));
        assertEquals("p3", configMapping.internalCall().get(1));
        assertEquals("p1", configMapping.roles().get("user").get(0));
        assertEquals("p2", configMapping.roles().get("administrator").get(0));
        assertEquals("p3", configMapping.roles().get("administrator").get(1));
    }

    @ConfigMapping(prefix = "channelsuite.permissions")
    interface PermissionsConfig {
        List<String> anonymous();

        List<String> internalCall();

        Map<String, List<String>> roles();
    }

    @Test
    void yamlMapLists() {
        String yaml = "map:\n" +
                "  roles:\n" +
                "    hokage:\n" +
                "      -\n" +
                "        name: Senju Hashirama\n" +
                "        nature: Earth,Water\n" +
                "      -\n" +
                "        name: Senju Tobirama\n" +
                "        nature: Water\n" +
                "      -\n" +
                "        name: Sarotobi Hiruzen\n" +
                "        nature: Fire\n" +
                "      -\n" +
                "        name: Namikaze Minato\n" +
                "        nature: Fire,Wind,Lightning\n" +
                "      -\n" +
                "        name: Tsunade\n" +
                "        nature: Lightning\n" +
                "      -\n" +
                "        name: Hatake Kakashi\n" +
                "        nature: Lightning\n" +
                "      -\n" +
                "        name: Uzumaki Naruto\n" +
                "        nature: Wind\n" +
                "        jutsus:\n" +
                "          -\n" +
                "            name: Rasengan\n" +
                "            rank: A\n" +
                "          -\n" +
                "            name: Bijūdama\n" +
                "            rank: S\n" +
                "        teams:\n" +
                "          \"Team Kakashi\":\n" +
                "            - Hatake Kakashi\n" +
                "            - Uchiha Sasuke\n" +
                "            - Haruno Sakura\n" +
                "          \"Kazekage Rescue Team\":\n" +
                "            - Hatake Kakashi\n" +
                "            - Chiyo\n" +
                "            - Haruno Sakura\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsGroup.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        MapWithListsGroup mapping = config.getConfigMapping(MapWithListsGroup.class);

        MapWithListsGroup.User fourth = mapping.roles().get("hokage").get(3);
        assertEquals("Namikaze Minato", fourth.name());

        MapWithListsGroup.User seventh = mapping.roles().get("hokage").get(6);
        assertEquals("Uzumaki Naruto", seventh.name());
        assertEquals("Wind", seventh.nature().get(0));
        assertEquals("Rasengan", seventh.jutsus().get(0).name());
        assertEquals(MapWithListsGroup.User.Jutsu.Rank.A, seventh.jutsus().get(0).rank());
        assertEquals("Bijūdama", seventh.jutsus().get(1).name());
        assertEquals(MapWithListsGroup.User.Jutsu.Rank.S, seventh.jutsus().get(1).rank());
        assertEquals("Hatake Kakashi", seventh.teams().get("Team Kakashi").get(0));
        assertEquals("Uchiha Sasuke", seventh.teams().get("Team Kakashi").get(1));
        assertEquals("Haruno Sakura", seventh.teams().get("Team Kakashi").get(2));
        assertEquals("Hatake Kakashi", seventh.teams().get("Kazekage Rescue Team").get(0));
        assertEquals("Chiyo", seventh.teams().get("Kazekage Rescue Team").get(1));
        assertEquals("Haruno Sakura", seventh.teams().get("Kazekage Rescue Team").get(2));
    }

    @ConfigMapping(prefix = "map")
    interface MapWithListsGroup {
        Map<String, List<User>> roles();

        interface User {
            String name();

            List<String> nature();

            List<Jutsu> jutsus();

            Map<String, List<String>> teams();

            interface Jutsu {
                String name();

                Rank rank();

                enum Rank {
                    S,
                    A,
                    B,
                    C,
                    D;
                }
            }
        }
    }

    @Test
    void yamlNestedMaps() {
        String yaml = "eventStaff:\n"
                + "    phones:\n"
                + "        home: \"1\"\n"
                + "    coordinators:\n"
                + "        tim:\n"
                + "            contactInfo:\n"
                + "                address: \"a\"\n"
                + "                phones:\n"
                + "                    home: \"1\"\n"
                + "                    cell: \"2\"\n";

        YamlConfigSource yamlConfigSource = new YamlConfigSource("Yaml", yaml);

        assertEquals("a", yamlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.address"));
        assertEquals("1", yamlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.phones.home"));
        assertEquals("2", yamlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.phones.cell"));

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(EventStaff.class)
                .withSources(yamlConfigSource)
                .build();

        EventStaff eventStaff = config.getConfigMapping(EventStaff.class);

        assertEquals("1", eventStaff.phones().get("home"));
        assertEquals("a", eventStaff.coordinators().get("tim").contactInfo().address());
        assertEquals("1", eventStaff.coordinators().get("tim").contactInfo().phones().get("home"));
        assertEquals("2", eventStaff.coordinators().get("tim").contactInfo().phones().get("cell"));
    }

    @ConfigMapping(prefix = "eventStaff", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface EventStaff {
        Map<String, String> phones();

        Map<String, Coordinator> coordinators();

        interface Coordinator {
            ContactInfo contactInfo();

            interface ContactInfo {
                String address();

                Map<String, String> phones();
            }
        }
    }

    @Test
    void unnamedMapKeys() {
        String yaml = "unnamed:\n" +
                "    map: \n" +
                "        value: value\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(UnnamedMapKeys.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        UnnamedMapKeys mapping = config.getConfigMapping(UnnamedMapKeys.class);

        assertEquals("value", mapping.map().get(null).value());
    }

    @ConfigMapping(prefix = "unnamed")
    interface UnnamedMapKeys {
        @WithUnnamedKey
        Map<String, Nested> map();

        interface Nested {
            String value();
        }
    }

    @Test
    void unmapped() {
        String yaml = "http:\n" +
                "  server:\n" +
                "    name: server\n" +
                "    alias: server\n" +
                "    host: localhost\n" +
                "    port: 8080\n" +
                "    timeout: 60s\n" +
                "    io-threads: 200\n" +
                "    bytes: dummy\n" +
                "\n" +
                "    form:\n" +
                "      login-page: login.html\n" +
                "      error-page: error.html\n" +
                "      landing-page: index.html\n" +
                "      positions:\n" +
                "        - 10\n" +
                "        - 20\n" +
                "\n" +
                "    ssl:\n" +
                "      port: 8443\n" +
                "      certificate: certificate\n" +
                "\n" +
                "    cors:\n" +
                "      origins:\n" +
                "        - host: some-server\n" +
                "          port: 9000\n" +
                "        - host: another-server\n" +
                "          port: 8000\n" +
                "      methods:\n" +
                "        - GET\n" +
                "        - POST\n" +
                "\n" +
                "    log:\n" +
                "      period: P1D\n" +
                "      days: 10\n" +
                "\n" +
                "cloud:\n" +
                "  host: localhost\n" +
                "  port: 8080\n" +
                "  timeout: 60s\n" +
                "  io-threads: 200\n" +
                "\n" +
                "  form:\n" +
                "    login-page: login.html\n" +
                "    error-page: error.html\n" +
                "    landing-page: index.html\n" +
                "\n" +
                "  ssl:\n" +
                "    port: 8443\n" +
                "    certificate: certificate\n" +
                "\n" +
                "  cors:\n" +
                "    origins:\n" +
                "      - host: some-server\n" +
                "        port: 9000\n" +
                "      - host: localhost\n" +
                "        port: 1\n" +
                "    methods:\n" +
                "      - GET\n" +
                "      - POST\n" +
                "\n" +
                "  proxy:\n" +
                "    enable: true\n" +
                "    timeout: 20\n" +
                "\n" +
                "  log:\n" +
                "    period: P1D\n" +
                "    days: 20\n" +
                "\n" +
                "  info:\n" +
                "    name: Bond\n" +
                "    code: 007\n" +
                "    alias:\n" +
                "      - James\n" +
                "    admins:\n" +
                "      root:\n" +
                "        -\n" +
                "          username: root\n" +
                "        -\n" +
                "          username: super\n" +
                "    firewall:\n" +
                "      accepted:\n" +
                "        - 127.0.0.1\n" +
                "        - 8.8.8";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .withMapping(Server.class)
                .withMapping(Cloud.class)
                .build();

        Server server = config.getConfigMapping(Server.class);
        assertTrue(server.name().isPresent());
        assertEquals("server", server.name().get());
        assertTrue(server.alias().isPresent());
        assertEquals("server", server.alias().get());
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals("60s", server.timeout());
        assertEquals(200, server.threads());
        assertArrayEquals(new Server.ByteArrayConverter().convert("dummy"), server.bytes());
        assertEquals("login.html", server.form().get("form").loginPage());
        assertEquals("error.html", server.form().get("form").errorPage());
        assertEquals("index.html", server.form().get("form").landingPage());
        assertIterableEquals(List.of(10, 20), server.form().get("form").positions());
        assertTrue(server.ssl().isPresent());
        assertEquals(8443, server.ssl().get().port());
        assertEquals("certificate", server.ssl().get().certificate());
        assertIterableEquals(List.of("TLSv1.3", "TLSv1.2"), server.ssl().get().protocols());
        assertTrue(server.cors().isPresent());
        assertEquals("some-server", server.cors().get().origins().get(0).host());
        assertEquals(9000, server.cors().get().origins().get(0).port());
        assertEquals("another-server", server.cors().get().origins().get(1).host());
        assertEquals(8000, server.cors().get().origins().get(1).port());
        assertEquals("GET", server.cors().get().methods().get(0));
        assertEquals("POST", server.cors().get().methods().get(1));
        assertFalse(server.log().enabled());
        assertEquals(".log", server.log().suffix());
        assertTrue(server.log().rotate());
        assertEquals("P1D", server.log().period());
        assertEquals(10, server.log().days());
        assertEquals(Server.Log.Pattern.COMMON.name(), server.log().pattern());
        assertTrue(server.info().name().isEmpty());
        assertTrue(server.info().code().isEmpty());
        assertTrue(server.info().alias().isEmpty());
        assertTrue(server.info().admins().isEmpty());
        assertTrue(server.info().firewall().isEmpty());

        Cloud cloud = config.getConfigMapping(Cloud.class);
        assertTrue(cloud.server().ssl().isPresent());
        assertEquals(8443, cloud.server().ssl().get().port());
        assertEquals("certificate", cloud.server().ssl().get().certificate());
    }

    @ConfigMapping(prefix = "cloud")
    public interface Cloud {
        @WithParentName
        Server server();
    }

    public interface Named {
        Optional<String> name();
    }

    public interface Alias extends Named {
        Optional<String> alias();
    }

    @ConfigMapping(prefix = "http.server")
    public interface Server extends Alias {
        String host();

        int port();

        String timeout();

        @WithName("io-threads")
        int threads();

        @WithConverter(ByteArrayConverter.class)
        byte[] bytes();

        @WithParentName
        Map<String, Form> form();

        Optional<Ssl> ssl();

        Optional<Proxy> proxy();

        Optional<Cors> cors();

        Log log();

        Info info();

        interface Form {
            String loginPage();

            String errorPage();

            String landingPage();

            Optional<String> cookie();

            @WithDefault("1")
            List<Integer> positions();
        }

        interface Proxy {
            boolean enable();

            int timeout();
        }

        interface Log {
            @WithDefault("false")
            boolean enabled();

            @WithDefault(".log")
            String suffix();

            @WithDefault("true")
            boolean rotate();

            @WithDefault("COMMON")
            String pattern();

            String period();

            int days();

            enum Pattern {
                COMMON,
                SHORT,
                COMBINED,
                LONG;
            }
        }

        interface Cors {
            List<Origin> origins();

            List<String> methods();

            interface Origin {
                String host();

                int port();
            }
        }

        interface Info {
            Optional<String> name();

            OptionalInt code();

            Optional<List<String>> alias();

            Map<String, List<Admin>> admins();

            Map<String, List<String>> firewall();

            interface Admin {
                String username();
            }
        }

        class ByteArrayConverter implements Converter<byte[]> {
            @Override
            public byte[] convert(String value) throws IllegalArgumentException, NullPointerException {
                return value.getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    public interface Ssl {
        int port();

        String certificate();

        @WithDefault("TLSv1.3,TLSv1.2")
        List<String> protocols();
    }

    @Test
    void nestedMaps() {
        String yaml = "mapping-service:\n" +
                "  service1:\n" +
                "      id-type: \"DNI\"\n" +
                "      id-number: \"12345\"\n" +
                "  service2:\n" +
                "      id-type: \"DNI\"\n" +
                "      id-number: \"12345\"";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedMaps.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        NestedMaps mapping = config.getConfigMapping(NestedMaps.class);

        assertEquals("DNI", mapping.services().get("service1").get("id-type"));
        assertEquals("12345", mapping.services().get("service1").get("id-number"));
        assertEquals("DNI", mapping.services().get("service2").get("id-type"));
        assertEquals("12345", mapping.services().get("service2").get("id-number"));
    }

    @ConfigMapping(prefix = "mapping-service")
    interface NestedMaps {
        @WithParentName
        Map<String, Map<String, String>> services();
    }

    @Test
    void overrideIndexedWithHigher() {
        String yaml = "override:\n" +
                "  values:\n" +
                "    - one\n" +
                "    - two\n";

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(OverrideIndexedWithHigher.class)
                .withSources(new YamlConfigSource("yaml", yaml))
                .withSources(new PropertiesConfigSource(Map.of("override.values", "three,four"), "", 1000))
                .build();

        Set<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(properties.contains("override.values[0]"));
        assertTrue(properties.contains("override.values[1]"));
        assertTrue(properties.contains("override.values"));

        OverrideIndexedWithHigher mapping = config.getConfigMapping(OverrideIndexedWithHigher.class);
        assertEquals(2, mapping.values().size());
        assertTrue(mapping.values().contains("three"));
        assertTrue(mapping.values().contains("four"));
    }

    @ConfigMapping(prefix = "override")
    interface OverrideIndexedWithHigher {
        List<String> values();
    }
}
