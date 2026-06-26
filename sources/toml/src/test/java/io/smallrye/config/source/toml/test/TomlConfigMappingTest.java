package io.smallrye.config.source.toml.test;

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
import io.smallrye.config.source.toml.TomlConfigSource;

class TomlConfigMappingTest {
    @Test
    void tomlConfigMapping() {
        String toml = """
                [[proxies]]
                type = "mssql"
                name = "first"

                [proxies.input.pool]
                max_pool_size = 100
                expires_in_seconds = 60

                [proxies.input.mssql]
                server = "192.168.1.2"
                port = 1434
                user = "test"
                password = "test"

                [proxies.read.pool]
                max_pool_size = 100
                expires_in_seconds = 60

                [proxies.read]
                regex = '(?i)^\\s*select.*'

                [proxies.read.mssql]
                server = "127.0.0.1"
                port = 1433
                user = "sa"
                password = "Test!1234"

                [proxies.write.pool]
                max_pool_size = 100
                expires_in_seconds = 60

                [proxies.write.mssql]
                server = "127.0.0.1"
                port = 1433
                user = "sa"
                password = "Test!1234"

                [[proxies]]
                type = "mssql"
                name = "second"

                [proxies.input.mssql]
                server = "192.168.1.2"
                port = 1435
                user = "test"
                password = "test"

                [proxies.input.pool]
                max_pool_size = 0
                expires_in_seconds = 0

                [proxies.input.encryption]
                level = "SUPPORTED"

                [proxies.input.encryption.keystore]
                location = "my-location"

                [proxies.read]
                regex = '(?i)^\\s*select.*'

                [proxies.read.mssql]
                server = "127.0.0.1"
                port = 1433
                user = "sa"
                password = "Test!1234"

                [proxies.write.mssql]
                server = "127.0.0.1"
                port = 1433
                user = "sa"
                password = "Test!1234"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Proxies.class)
                .withSources(new TomlConfigSource("toml", toml))
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

    interface SQLProxyConfig {
        @WithName("name")
        String name();

        enum DatabaseType {
            mssql,
            oracle,
            plsql
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
    void tomlListMaps() {
        String toml = """
                [[app.config]]
                name = "Bob"
                foo = "thing"
                bar = false

                [[app.config]]
                name = "Tim"
                baz = "stuff"
                qux = 3
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapConfig.class)
                .withSources(new TomlConfigSource("toml", toml))
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
    void tomlMapGroupMap() {
        String toml = """
                [parent.goodchildren.child1]
                name = "John"

                [parent.goodchildren.child1.attributes]
                somekey = "somevalue"
                anotherkey = "anothervalue"

                [parent.goodchildren.child2]
                name = "James"

                [parent.goodchildren.child2.attributes]
                something = "isbroken"

                [parent.badchildren.child3]
                name = "BadJohn"

                [parent.badchildren.child3.attributes]
                somekeybad = "somevaluebad"
                anotherkeybad = "anothervaluebad"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Parent.class)
                .withSources(new TomlConfigSource("toml", toml))
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
    void tomlMapCollections() {
        String toml = """
                [some.prop]
                value = [0.9, 0.99]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapListDouble.class)
                .withSources(new TomlConfigSource("toml", toml))
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
    void tomlMapListsGroup() {
        String toml = """
                [channelsuite.permissions]
                anonymous = ["p1"]
                internal-call = ["p2", "p3"]

                [channelsuite.permissions.roles]
                user = ["p1"]
                administrator = ["p2", "p3"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(PermissionsConfig.class)
                .withSources(new TomlConfigSource("toml", toml))
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
    void tomlMapLists() {
        String toml = """
                [[map.roles.hokage]]
                name = "Senju Hashirama"
                nature = "Earth,Water"

                [[map.roles.hokage]]
                name = "Senju Tobirama"
                nature = "Water"

                [[map.roles.hokage]]
                name = "Sarotobi Hiruzen"
                nature = "Fire"

                [[map.roles.hokage]]
                name = "Namikaze Minato"
                nature = "Fire,Wind,Lightning"

                [[map.roles.hokage]]
                name = "Tsunade"
                nature = "Lightning"

                [[map.roles.hokage]]
                name = "Hatake Kakashi"
                nature = "Lightning"

                [[map.roles.hokage]]
                name = "Uzumaki Naruto"
                nature = "Wind"

                [[map.roles.hokage.jutsus]]
                name = "Rasengan"
                rank = "A"

                [[map.roles.hokage.jutsus]]
                name = "Bijūdama"
                rank = "S"

                [map.roles.hokage.teams]
                "Team Kakashi" = ["Hatake Kakashi", "Uchiha Sasuke", "Haruno Sakura"]
                "Kazekage Rescue Team" = ["Hatake Kakashi", "Chiyo", "Haruno Sakura"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsGroup.class)
                .withSources(new TomlConfigSource("toml", toml))
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
                    D
                }
            }
        }
    }

    @Test
    void tomlNestedMaps() {
        String toml = """
                [eventStaff.phones]
                home = "1"

                [eventStaff.coordinators.tim.contactInfo]
                address = "a"

                [eventStaff.coordinators.tim.contactInfo.phones]
                home = "1"
                cell = "2"
                """;

        TomlConfigSource tomlConfigSource = new TomlConfigSource("toml", toml);

        assertEquals("a", tomlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.address"));
        assertEquals("1", tomlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.phones.home"));
        assertEquals("2", tomlConfigSource.getValue("eventStaff.coordinators.tim.contactInfo.phones.cell"));

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(EventStaff.class)
                .withSources(tomlConfigSource)
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
        String toml = """
                [unnamed.map]
                value = "value"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(UnnamedMapKeys.class)
                .withSources(new TomlConfigSource("toml", toml))
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
        String toml = """
                [http.server]
                name = "server"
                alias = "server"
                host = "localhost"
                port = 8080
                timeout = "60s"
                io-threads = 200
                bytes = "dummy"

                [http.server.form]
                login-page = "login.html"
                error-page = "error.html"
                landing-page = "index.html"
                positions = [10, 20]

                [http.server.ssl]
                port = 8443
                certificate = "certificate"

                [http.server.cors]
                methods = ["GET", "POST"]

                [[http.server.cors.origins]]
                host = "some-server"
                port = 9000

                [[http.server.cors.origins]]
                host = "another-server"
                port = 8000

                [http.server.log]
                period = "P1D"
                days = 10

                [cloud]
                host = "localhost"
                port = 8080
                timeout = "60s"
                io-threads = 200

                [cloud.form]
                login-page = "login.html"
                error-page = "error.html"
                landing-page = "index.html"

                [cloud.ssl]
                port = 8443
                certificate = "certificate"

                [cloud.cors]
                methods = ["GET", "POST"]

                [[cloud.cors.origins]]
                host = "some-server"
                port = 9000

                [[cloud.cors.origins]]
                host = "localhost"
                port = 1

                [cloud.proxy]
                enable = true
                timeout = 20

                [cloud.log]
                period = "P1D"
                days = 20

                [cloud.info]
                name = "Bond"
                code = 7
                alias = ["James"]

                [[cloud.info.admins.root]]
                username = "root"

                [[cloud.info.admins.root]]
                username = "super"

                [cloud.info.firewall]
                accepted = ["127.0.0.1", "8.8.8"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource("toml", toml))
                .withMapping(MappedServer.class)
                .withMapping(Cloud.class)
                .build();

        MappedServer server = config.getConfigMapping(MappedServer.class);
        assertTrue(server.name().isPresent());
        assertEquals("server", server.name().get());
        assertTrue(server.alias().isPresent());
        assertEquals("server", server.alias().get());
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals("60s", server.timeout());
        assertEquals(200, server.threads());
        assertArrayEquals(new MappedServer.ByteArrayConverter().convert("dummy"), server.bytes());
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
        assertEquals(MappedServer.Log.Pattern.COMMON.name(), server.log().pattern());
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
        MappedServer server();
    }

    public interface Named {
        Optional<String> name();
    }

    public interface Alias extends Named {
        Optional<String> alias();
    }

    @ConfigMapping(prefix = "http.server")
    public interface MappedServer extends Alias {
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
        String toml = """
                [mapping-service.service1]
                id-type = "DNI"
                id-number = "12345"

                [mapping-service.service2]
                id-type = "DNI"
                id-number = "12345"
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedMaps.class)
                .withSources(new TomlConfigSource("toml", toml))
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
        String toml = """
                [override]
                values = ["one", "two"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(OverrideIndexedWithHigher.class)
                .withSources(new TomlConfigSource("toml", toml))
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

    @Test
    void listWithUnnamed() {
        String toml = """
                sources = ["8.8.8.8", "8.8.4.4"]
                """;

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ListWithUnnamed.class)
                .withSources(new TomlConfigSource("toml", toml))
                .withValidateUnknown(false)
                .build();

        ListWithUnnamed mapping = config.getConfigMapping(ListWithUnnamed.class);
        assertEquals(2, mapping.sources().size());
        assertEquals("8.8.8.8", mapping.sources().get(0).unnamed().get());
        assertEquals("8.8.4.4", mapping.sources().get(1).unnamed().get());
    }

    @ConfigMapping(prefix = "sources")
    interface ListWithUnnamed {
        @WithParentName
        List<Source> sources();

        interface Source {
            @WithParentName
            Optional<String> unnamed();

            Optional<String> uri();

            Optional<String> type();
        }
    }
}
