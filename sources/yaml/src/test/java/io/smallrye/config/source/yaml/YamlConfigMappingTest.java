package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

class YamlConfigMappingTest {
    @Test
    void yamlConfigMapping() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Proxies.class, "proxies")
                .withSources(new YamlConfigSource(YamlConfigSourceTest.class.getResource("/example-mapping.yml")))
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
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapConfig.class, "app")
                .withSources(new YamlConfigSource("yaml",
                        "app:\n" +
                                "  config:\n" +
                                "    - name: Bob\n" +
                                "      foo: thing\n" +
                                "      bar: false\n" +
                                "    - name: Tim\n" +
                                "      baz: stuff\n" +
                                "      qux: 3"))
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
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Parent.class, "parent")
                .withSources(new YamlConfigSource("yaml",
                        "parent:\n" +
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
                                "        anotherkeybad: anothervaluebad "))
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
    void yamlMapCollections() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapListDouble.class)
                .withSources(new YamlConfigSource("yaml",
                        "some:\n" +
                                "  prop:\n" +
                                "    value:    \n" +
                                "      - 0.9\n" +
                                "      - 0.99"))
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
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(PermissionsConfig.class)
                .withSources(new YamlConfigSource("yaml",
                        "channelsuite:\n" +
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
                                "        - \"p3\"\n"))
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
    void yamlMapLists() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsGroup.class)
                .withSources(new YamlConfigSource(YamlConfigSourceTest.class.getResource("/example-map-list.yml")))
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
                .withMapping(EventStaff.class, "eventStaff")
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
}
