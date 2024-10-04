package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment;
import io.smallrye.config.ConfigMappingCollectionsTest.ServerCollectionsSet.Environment.App;

public class ConfigMappingCollectionsTest {
    @ConfigMapping(prefix = "server")
    public interface ServerCollectionSimple {
        List<String> environments();
    }

    @Test
    void mappingCollectionsSimple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionSimple.class)
                .withSources(config("server.environments[0]", "dev",
                        "server.environments[1]", "qa",
                        "server.environments[2]", "prod"))
                .build();

        List<String> environments = config.getValues("server.environments", String.class);
        assertEquals(3, environments.size());
        assertEquals("dev", environments.get(0));
        assertEquals("qa", environments.get(1));
        assertEquals("prod", environments.get(2));

        ServerCollectionSimple configMapping = config.getConfigMapping(ServerCollectionSimple.class);
        assertEquals(3, configMapping.environments().size());
        assertEquals("dev", configMapping.environments().get(0));
        assertEquals("qa", configMapping.environments().get(1));
        assertEquals("prod", configMapping.environments().get(2));
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollections {
        List<Environment> environments();

        interface Environment {
            String name();

            List<App> apps();

            interface App {
                String name();

                List<String> services();

                Optional<List<String>> databases();
            }
        }
    }

    @Test
    void mappingCollections() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollections.class)
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollections server = config.getConfigMapping(ServerCollections.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("a", "b", "c").collect(toList()), server.environments().get(0).apps().get(0).services());
        assertTrue(server.environments().get(0).apps().get(0).databases().isPresent());
        assertEquals(Stream.of("pg", "h2").collect(toList()), server.environments().get(0).apps().get(0).databases().get());
        assertEquals(2, server.environments().get(0).apps().size());
        assertEquals("rest", server.environments().get(0).apps().get(0).name());
        assertEquals("batch", server.environments().get(0).apps().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).apps().size());
        assertEquals("web", server.environments().get(1).apps().get(0).name());
        assertEquals("rest", server.environments().get(1).apps().get(1).name());
        assertEquals("batch", server.environments().get(1).apps().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionName {
        @WithName("envs")
        List<Environment> environments();

        interface Environment {
            String name();

            @WithName("apps")
            List<App> applications();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingCollectionsWithName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionName.class)
                .withSources(config("server.envs[0].name", "dev",
                        "server.envs[0].apps[0].name", "rest",
                        "server.envs[0].apps[1].name", "batch",
                        "server.envs[1].name", "prod",
                        "server.envs[1].apps[0].name", "web",
                        "server.envs[1].apps[1].name", "rest",
                        "server.envs[1].apps[2].name", "batch"))
                .build();
        ServerCollectionName server = config.getConfigMapping(ServerCollectionName.class);

        assertEquals(2, server.environments().size());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(2, server.environments().get(0).applications().size());
        assertEquals("rest", server.environments().get(0).applications().get(0).name());
        assertEquals("batch", server.environments().get(0).applications().get(1).name());
        assertEquals("prod", server.environments().get(1).name());
        assertEquals(3, server.environments().get(1).applications().size());
        assertEquals("web", server.environments().get(1).applications().get(0).name());
        assertEquals("rest", server.environments().get(1).applications().get(1).name());
        assertEquals("batch", server.environments().get(1).applications().get(2).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsDefaults {
        @WithParentName
        List<Environment> environments();

        interface Environment {
            @WithDefault("-1")
            int id();

            String name();

            @WithDefault("web,rest")
            List<String> apps();

            @WithDefault("80,443")
            List<Integer> ports();
        }
    }

    @Test
    void mappingCollectionsWithDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsDefaults.class)
                .withSources(config("server[0].name", "dev"))
                .build();

        ServerCollectionsDefaults server = config.getConfigMapping(ServerCollectionsDefaults.class);

        assertEquals(1, server.environments().size());
        assertEquals(-1, server.environments().get(0).id());
        assertEquals("dev", server.environments().get(0).name());
        assertEquals(Stream.of("web", "rest").collect(toList()), server.environments().get(0).apps());

        assertEquals(2, server.environments().get(0).ports().size());
        assertEquals(Stream.of(80, 443).collect(toList()), server.environments().get(0).ports());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsOptionals {
        @WithName("env")
        List<Environment> environments();

        Optional<Environment> notRequired();

        @WithName("req")
        Optional<Environment> required();

        Optional<List<Environment>> notRequiredEnvs();

        @WithName("reqEnvs")
        Optional<List<Environment>> requiredEnvs();

        interface Environment {
            String name();

            Optional<List<String>> notRequiredServices();

            Optional<List<String>> requiredServices();

            Optional<List<String>> indexed();

            Optional<App> app();

            interface App {
                Optional<String> alias();
            }
        }
    }

    @Test
    void mappingCollectionsOptionals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsOptionals.class)
                .withSources(config(
                        "server.env[0].name", "dev",
                        "server.env[1].name", "prod",
                        "server.env[0].app.alias", "rest",
                        "server.req.name", "dev",
                        "server.req.required-services", "rest,db",
                        "server.req.indexed[0]", "rest",
                        "server.req.indexed[1]", "db",
                        "server.reqEnvs[0].name", "dev",
                        "server.reqEnvs[1].name", "prod"))
                .build();

        ServerCollectionsOptionals server = config.getConfigMapping(ServerCollectionsOptionals.class);

        assertFalse(server.environments().isEmpty());
        assertEquals("dev", server.environments().get(0).name());
        assertTrue(server.environments().get(0).app().isPresent());
        assertTrue(server.environments().get(0).app().get().alias().isPresent());
        assertEquals("rest", server.environments().get(0).app().get().alias().get());
        assertFalse(server.notRequired().isPresent());
        assertTrue(server.required().isPresent());
        assertEquals("dev", server.required().get().name());
        assertFalse(server.required().get().notRequiredServices().isPresent());
        assertTrue(server.required().get().requiredServices().isPresent());
        assertEquals("rest", server.required().get().requiredServices().get().get(0));
        assertEquals("db", server.required().get().requiredServices().get().get(1));
        assertTrue(server.required().get().indexed().isPresent());
        assertEquals("rest", server.required().get().indexed().get().get(0));
        assertEquals("db", server.required().get().indexed().get().get(1));

        assertFalse(server.notRequiredEnvs().isPresent());
        assertTrue(server.requiredEnvs().isPresent());
        assertEquals("dev", server.requiredEnvs().get().get(0).name());
        assertEquals("prod", server.requiredEnvs().get().get(1).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsConverters {
        List<Enviroment> envs();

        interface Enviroment {
            @WithConverter(HostConverter.class)
            List<Host> hosts();
        }
    }

    static class Host {
        final String name;

        Host(final String name) {
            this.name = name;
        }
    }

    static class HostConverter implements Converter<Host> {
        public HostConverter() {
        }

        @Override
        public Host convert(final String value) throws IllegalArgumentException, NullPointerException {
            return new Host(value);
        }
    }

    @Test
    void mappingCollectionsConverters() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsConverters.class)
                .withSources(config("server.envs[0].hosts[0]", "localhost",
                        "server.envs[0].hosts[1]", "127.0.0.1"))
                .build();

        ServerCollectionsConverters server = config.getConfigMapping(ServerCollectionsConverters.class);

        assertEquals(1, server.envs().size());
        assertEquals(2, server.envs().get(0).hosts().size());
        assertEquals("localhost", server.envs().get(0).hosts().get(0).name);
        assertEquals("127.0.0.1", server.envs().get(0).hosts().get(1).name);
    }

    @ConfigMapping(prefix = "server")
    public interface ServerCollectionsSet {
        Set<Environment> environments();

        interface Environment {
            String name();

            Set<App> apps();

            interface App {
                String name();

                Set<String> services();

                Optional<Set<String>> databases();
            }
        }
    }

    @Test
    void mappingCollectionsSet() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionsSet.class)
                .withSources(config(
                        "server.environments[0].name", "dev",
                        "server.environments[0].apps[0].name", "rest",
                        "server.environments[0].apps[0].services", "a,b,c",
                        "server.environments[0].apps[0].databases", "pg,h2",
                        "server.environments[0].apps[1].name", "batch",
                        "server.environments[0].apps[1].services", "a,b,c",
                        "server.environments[1].name", "prod",
                        "server.environments[1].apps[0].name", "web",
                        "server.environments[1].apps[0].services", "a,b,c",
                        "server.environments[1].apps[1].name", "rest",
                        "server.environments[1].apps[1].services", "a,b,c",
                        "server.environments[1].apps[2].name", "batch",
                        "server.environments[1].apps[2].services", "a,b,c"))
                .build();
        ServerCollectionsSet server = config.getConfigMapping(ServerCollectionsSet.class);

        assertEquals(2, server.environments().size());

        Optional<Environment> dev = server.environments().stream().filter(environment -> environment.name().equals("dev"))
                .findFirst();
        assertTrue(dev.isPresent());
        dev.ifPresent(environment -> {
            assertEquals(2, environment.apps().size());
            Optional<App> rest = environment.apps().stream().filter(app -> app.name().equals("rest")).findFirst();
            assertTrue(rest.isPresent());
            rest.ifPresent(app -> {
                assertTrue(Stream.of("a", "b", "c").collect(toSet()).containsAll(app.services()));
                assertTrue(app.databases().isPresent());
                assertTrue(Stream.of("pg", "h2").collect(toSet()).containsAll(app.databases().get()));
            });
        });
    }

    @ConfigMapping(prefix = "server")
    public interface ServerOptionalGroup {
        Optional<Environment> environment();

        interface Environment {
            List<String> services();

            List<App> apps();

            interface App {
                String name();
            }
        }
    }

    @Test
    void mappingOptionalGroupWithCollection() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class)
                .withSources(config(
                        "server.environment.services[0]", "rest",
                        "server.environment.services[1]", "batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        ServerOptionalGroup server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());

        config = new SmallRyeConfigBuilder()
                .withMapping(ServerOptionalGroup.class)
                .withSources(config(
                        "server.environment.services", "rest,batch",
                        "server.environment.apps[0].name", "a"))
                .build();

        server = config.getConfigMapping(ServerOptionalGroup.class);
        assertTrue(server.environment().isPresent());
        assertEquals("rest", server.environment().get().services().get(0));
        assertEquals("batch", server.environment().get().services().get(1));
        assertEquals("a", server.environment().get().apps().get(0).name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerSingleCollection {
        @WithParentName
        List<Server> origins();

        interface Server {
            String host();

            int port();
        }
    }

    @Test
    void mappingSingleCollection() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerSingleCollection.class)
                .withSources(config(
                        "server[0].host", "localhost",
                        "server[0].port", "8080",
                        "server[1].host", "my-server",
                        "server[1].port", "80"))
                .build();

        ServerSingleCollection server = config.getConfigMapping(ServerSingleCollection.class);
        assertEquals(2, server.origins().size());
        assertEquals("localhost", server.origins().get(0).host());
        assertEquals(8080, server.origins().get(0).port());
        assertEquals("my-server", server.origins().get(1).host());
        assertEquals(80, server.origins().get(1).port());
    }

    @Test
    void mappingCollectionProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerSingleCollection.class)
                .withSources(config(
                        "server[0].host", "localhost",
                        "server[0].port", "8080",
                        "server[1].host", "my-server",
                        "server[1].port", "80",
                        "%test.server[0].host", "localhost-test",
                        "%test.server[0].port", "8081"))
                .withProfile("test")
                .build();

        ServerSingleCollection mapping = config.getConfigMapping(ServerSingleCollection.class);
        assertEquals(2, mapping.origins().size());
        assertEquals("localhost-test", mapping.origins().get(0).host());
        assertEquals(8081, mapping.origins().get(0).port());
        assertEquals("my-server", mapping.origins().get(1).host());
        assertEquals(80, mapping.origins().get(1).port());
    }

    @ConfigMapping(prefix = "servers")
    public interface ServerCollectionMap {
        @WithParentName
        List<Map<String, String>> servers();

        List<Map<String, Server>> moreServers();

        Optional<List<Map<String, String>>> optional();

        Optional<List<Map<String, String>>> present();

        interface Server {
            String host();

            int port();
        }
    }

    @Test
    void mappingCollectionsMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerCollectionMap.class)
                .withSources(config(
                        "servers[0].localhost", "localhost",
                        "servers[1].konoha", "konoha",
                        "servers.more-servers[0].local.host", "localhost",
                        "servers.more-servers[0].local.port", "8080",
                        "servers.more-servers[1].kon.host", "konoha",
                        "servers.more-servers[1].kon.port", "80",
                        "servers.present[0].localhost", "localhost"))
                .build();

        ServerCollectionMap mapping = config.getConfigMapping(ServerCollectionMap.class);
        assertEquals("localhost", mapping.servers().get(0).get("localhost"));
        assertEquals("konoha", mapping.servers().get(1).get("konoha"));

        assertEquals("localhost", mapping.moreServers().get(0).get("local").host());
        assertEquals(8080, mapping.moreServers().get(0).get("local").port());
        assertEquals("konoha", mapping.moreServers().get(1).get("kon").host());
        assertEquals(80, mapping.moreServers().get(1).get("kon").port());

        assertFalse(mapping.optional().isPresent());

        assertTrue(mapping.present().isPresent());
        assertEquals("localhost", mapping.present().get().get(0).get("localhost"));
    }

    @ConfigMapping
    public interface MapKeyWithIndexedSyntax {
        Map<String, String> map();
    }

    @Test
    void mapKeyWithIndexedSyntax() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapKeyWithIndexedSyntax.class)
                .withSources(config("map.key", "value"))
                .withSources(config("map.key[x]", "value"))
                .withSources(config("map.key[0-9]", "value"))
                .build();

        MapKeyWithIndexedSyntax mapping = config.getConfigMapping(MapKeyWithIndexedSyntax.class);

        assertEquals("value", mapping.map().get("key"));
        assertEquals("value", mapping.map().get("key[x]"));
        assertEquals("value", mapping.map().get("key[0-9]"));
    }

    @ConfigMapping(prefix = "map")
    public interface MapWithCollections {
        Map<String, List<String>> roles();

        Map<String, Set<String>> alias();
    }

    @Test
    void mapWithColllections() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithCollections.class)
                .withSources(config("map.roles.user[0]", "p1"))
                .withSources(config("map.roles.admin[0]", "p2", "map.roles.admin[1]", "p3"))
                .withSources(config("map.alias.user[0]", "p1"))
                .withSources(config("map.alias.admin[0]", "p2", "map.alias.admin[1]", "p2"))
                .build();

        MapWithCollections mapping = config.getConfigMapping(MapWithCollections.class);

        assertEquals("p1", mapping.roles().get("user").get(0));
        assertEquals("p2", mapping.roles().get("admin").get(0));
        assertEquals("p3", mapping.roles().get("admin").get(1));

        assertEquals(1, mapping.alias().get("user").size());
        assertTrue(mapping.alias().get("user").contains("p1"));
        assertEquals(1, mapping.alias().get("admin").size());
        assertTrue(mapping.alias().get("admin").contains("p2"));
    }

    @ConfigMapping(prefix = "map")
    public interface MapWithListsGroup {
        Map<String, List<Role>> roles();

        interface Role {
            String name();

            List<String> aliases();

            Map<String, List<String>> permissions();
        }
    }

    @Test
    void mapWithListsGroup() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsGroup.class)
                .withSources(config(
                        "map.roles.user[0].name", "p1",
                        "map.roles.user[0].aliases[0]", "user-role-p1",
                        "map.roles.user[0].permissions.read", "read"))
                .withSources(config(
                        "map.roles.admin[0].name", "p2",
                        "map.roles.admin[0].aliases", "admin-role-p2,administrator-role-p2",
                        "map.roles.admin[0].permissions.read", "read",
                        "map.roles.admin[1].name", "p3",
                        "map.roles.admin[1].aliases", "admin-role-p3,administrator-role-p3",
                        "map.roles.admin[1].permissions.write[0]", "create",
                        "map.roles.admin[1].permissions.write[1]", "update"))
                .build();

        MapWithListsGroup mapping = config.getConfigMapping(MapWithListsGroup.class);

        assertEquals(1, mapping.roles().get("user").size());
        assertEquals("p1", mapping.roles().get("user").get(0).name());
        assertEquals("user-role-p1", mapping.roles().get("user").get(0).aliases().get(0));
        assertEquals("read", mapping.roles().get("user").get(0).permissions().get("read").get(0));
        assertEquals(2, mapping.roles().get("admin").size());
        assertEquals("p2", mapping.roles().get("admin").get(0).name());
        assertEquals("admin-role-p2", mapping.roles().get("admin").get(0).aliases().get(0));
        assertEquals("administrator-role-p2", mapping.roles().get("admin").get(0).aliases().get(1));
        assertEquals("read", mapping.roles().get("admin").get(0).permissions().get("read").get(0));
        assertEquals("p3", mapping.roles().get("admin").get(1).name());
        assertEquals("admin-role-p3", mapping.roles().get("admin").get(1).aliases().get(0));
        assertEquals("administrator-role-p3", mapping.roles().get("admin").get(1).aliases().get(1));
        assertEquals("create", mapping.roles().get("admin").get(1).permissions().get("write").get(0));
        assertEquals("update", mapping.roles().get("admin").get(1).permissions().get("write").get(1));
    }

    @ConfigMapping(prefix = "map")
    public interface MapWithListsAndParentName {
        Map<String, Roles> roles();

        Map<String, Aliases> aliases();

        interface Roles {
            @WithParentName
            List<String> roles();
        }

        interface Aliases {
            List<String> alias();
        }
    }

    @Test
    void mapWithListsAndParentName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsAndParentName.class)
                .withSources(config("map.roles.user", "p1"))
                .withSources(config("map.roles.admin", "p2,p3"))
                .build();

        MapWithListsAndParentName mapping = config.getConfigMapping(MapWithListsAndParentName.class);

        assertEquals("p1", mapping.roles().get("user").roles().get(0));
        assertEquals("p2", mapping.roles().get("admin").roles().get(0));
        assertEquals("p3", mapping.roles().get("admin").roles().get(1));

        config = new SmallRyeConfigBuilder()
                .withMapping(MapWithListsAndParentName.class)
                .withSources(config("map.roles.user[0]", "p1"))
                .withSources(config("map.roles.admin[0]", "p2", "map.roles.admin[1]", "p3"))
                .withSources(config("map.aliases.user.alias[0]", "username", "map.aliases.user.alias[1]", "login"))
                .withSources(config("map.aliases.admin.alias[0]", "administrator", "map.aliases.admin.alias[1]", "root"))
                .build();

        mapping = config.getConfigMapping(MapWithListsAndParentName.class);

        assertEquals("p1", mapping.roles().get("user").roles().get(0));
        assertEquals("p2", mapping.roles().get("admin").roles().get(0));
        assertEquals("p3", mapping.roles().get("admin").roles().get(1));
        assertEquals("username", mapping.aliases().get("user").alias().get(0));
        assertEquals("login", mapping.aliases().get("user").alias().get(1));
        assertEquals("administrator", mapping.aliases().get("admin").alias().get(0));
        assertEquals("root", mapping.aliases().get("admin").alias().get(1));
    }

    @ConfigMapping(prefix = "map")
    interface MapIndexedAndPlain {
        @WithParentName
        Map<String, List<String>> map();
    }

    @Test
    void mapIndexedAndPlain() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapIndexedAndPlain.class)
                .withSources(config(
                        "map.one[0]", "one", "map.one[1]", "1",
                        "map.two", "two,2"))
                .build();

        MapIndexedAndPlain mapping = config.getConfigMapping(MapIndexedAndPlain.class);

        assertEquals("one", mapping.map().get("one").get(0));
        assertEquals("1", mapping.map().get("one").get(1));
        assertEquals("two", mapping.map().get("two").get(0));
    }

    @Test
    void simpleMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SimpleMap.class)
                .withSources(config("map.one", "value"))
                .withSources(config("map.key-converter.key", "value"))
                .withSources(config("map.value-converter.one", "something"))
                .withSources(config("map.defaults.one", "value"))
                .withSources(config("map.defaults-value-converter.one", "something"))
                .build();

        SimpleMap mapping = config.getConfigMapping(SimpleMap.class);

        assertEquals("value", mapping.map().get("one"));
        assertNull(mapping.map().get("any"));
        assertEquals("value", mapping.keyConverter().get("one"));
        assertNull(mapping.keyConverter().get("any"));
        assertEquals("value", mapping.valueConverter().get("one"));
        assertNull(mapping.valueConverter().get("any"));
        assertEquals("value", mapping.defaults().get("one"));
        assertEquals("any", mapping.defaults().get("any"));
        assertEquals("value", mapping.defaultsValueConverter().get("one"));
        assertEquals("value", mapping.defaultsValueConverter().get("any"));
        assertTrue(mapping.defaultsOnly().isEmpty());
        assertEquals("any", mapping.defaultsOnly().get("any"));
    }

    @ConfigMapping(prefix = "map")
    interface SimpleMap {
        @WithParentName
        Map<String, String> map();

        Map<@WithConverter(KeyConverter.class) String, String> keyConverter();

        Map<String, @WithConverter(ValueConverter.class) String> valueConverter();

        @WithDefault("any")
        Map<String, String> defaults();

        @WithDefault("any")
        Map<String, @WithConverter(ValueConverter.class) String> defaultsValueConverter();

        @WithDefault("any")
        Map<String, String> defaultsOnly();

        class KeyConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "one";
            }
        }

        class ValueConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "value";
            }
        }
    }

    @Test
    void nestedMaps() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "nested.simple.one", "one"))
                .withSources(config(
                        "nested.map.one.two", "one-two",
                        "nested.map.1.2", "1-2",
                        "nested.map.one.two.three", "one-two-three",
                        "nested.map.\"1.one\".\"2.two\"", "1.one-2.two"))
                .withSources(config(
                        "nested.key-converter.\"1.one\".\"2.two\"", "1.one-2.two"))
                .withSources(config(
                        "nested.key-implicit.1.1", "1-1",
                        "nested.key-implicit.1.\"2\"", "1-2"))
                .withSources(config(
                        "nested.value-converter.one.one", "one-one",
                        "nested.value-converter.one.\"two\"", "one-two"))
                .withSources(config(
                        "nested.defaults.one.one", "one-one",
                        "nested.defaults.one.\"two\"", "one-two"))
                .withSources(config(
                        "nested.triple.one.two.one", "one-two-one",
                        "nested.triple.one.two.two", "one-two-two",
                        "nested.triple.one.two.three", "one-two-three",
                        "nested.triple.1.2.3", "1-2-3",
                        "nested.triple.\"1.one\".\"2.two\".\"3.three\"", "1.one-2.two-3.three"))
                .withMapping(NestedMaps.class)
                .build();

        NestedMaps mapping = config.getConfigMapping(NestedMaps.class);

        assertEquals("one", mapping.simple().get("one"));

        assertEquals("one-two", mapping.map().get("one").get("two"));
        assertEquals("1-2", mapping.map().get("1").get("2"));
        assertEquals("one-two-three", mapping.map().get("one").get("two.three"));
        assertEquals("1.one-2.two", mapping.map().get("1.one").get("2.two"));

        assertEquals("1.one-2.two", mapping.keyConverter().get("one").get("one"));

        assertEquals("1-1", mapping.keyImplicit().get(1).get(1));
        assertEquals("1-2", mapping.keyImplicit().get(1).get(2));

        assertEquals("value", mapping.valueConverter().get("one").get("one"));
        assertEquals("value", mapping.valueConverter().get("one").get("two"));

        assertEquals("one-one", mapping.defaults().get("one").get("one"));
        assertEquals("one-two", mapping.defaults().get("one").get("two"));
        assertEquals("any", mapping.defaults().get("one").get("three"));
        // TODO - Add defaults for middle maps?
        //assertEquals("any", mapping.defaults().get("any").get("any"));

        assertEquals("one-two-one", mapping.triple().get("one").get("two").get("one"));
        assertEquals("one-two-two", mapping.triple().get("one").get("two").get("two"));
        assertEquals("one-two-three", mapping.triple().get("one").get("two").get("three"));
        assertEquals("1-2-3", mapping.triple().get("1").get("2").get("3"));
        assertEquals("1.one-2.two-3.three", mapping.triple().get("1.one").get("2.two").get("3.three"));

        // TODO - Assert keys that do not complete the Map nesting levels
    }

    @ConfigMapping(prefix = "nested")
    public interface NestedMaps {
        Map<String, String> simple();

        Map<String, Map<String, String>> map();

        Map<@WithConverter(KeyConverter.class) String, Map<@WithConverter(KeyConverter.class) String, String>> keyConverter();

        Map<Integer, Map<Integer, String>> keyImplicit();

        Map<String, Map<String, @WithConverter(ValueConverter.class) String>> valueConverter();

        @WithDefault("any")
        Map<String, Map<String, String>> defaults();

        Map<String, Map<String, Map<String, String>>> triple();

        class KeyConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "one";
            }
        }

        class ValueConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "value";
            }
        }
    }

    @Test
    void simpleMapList() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SimpleMapList.class)
                .withSources(config("map.one[0]", "value"))
                .withSources(config("map.key-converter.key[0]", "value"))
                .withSources(config("map.value-converter.one[0]", "something"))
                .withSources(config("map.defaults.one[0]", "value"))
                .withSources(config("map.defaults-value-converter.one[0]", "something"))
                .build();

        SimpleMapList mapping = config.getConfigMapping(SimpleMapList.class);

        assertEquals("value", mapping.map().get("one").get(0));
        assertNull(mapping.map().get("any"));
        assertEquals("value", mapping.keyConverter().get("one").get(0));
        assertNull(mapping.keyConverter().get("any"));
        assertEquals("value", mapping.valueConverter().get("one").get(0));
        assertNull(mapping.valueConverter().get("any"));
        assertEquals("value", mapping.defaults().get("one").get(0));
        assertEquals("any", mapping.defaults().get("any").get(0));
        assertEquals("something", mapping.defaults().get("any").get(1));
        assertEquals("value", mapping.defaultsValueConverter().get("one").get(0));
        assertEquals("value", mapping.defaultsValueConverter().get("any").get(0));
        assertTrue(mapping.defaultsOnly().isEmpty());
        assertEquals("any", mapping.defaultsOnly().get("any").get(0));
        assertEquals("something", mapping.defaultsOnly().get("any").get(1));
    }

    @ConfigMapping(prefix = "map")
    interface SimpleMapList {
        @WithParentName
        Map<String, List<String>> map();

        Map<@WithConverter(KeyConverter.class) String, List<String>> keyConverter();

        Map<String, List<@WithConverter(ValueConverter.class) String>> valueConverter();

        @WithDefault("any,something")
        Map<String, List<String>> defaults();

        @WithDefault("any")
        Map<String, @WithConverter(ValueConverter.class) List<String>> defaultsValueConverter();

        @WithDefault("any,something")
        Map<String, List<String>> defaultsOnly();

        class KeyConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "one";
            }
        }

        class ValueConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "value";
            }
        }
    }

    @Test
    void nestedMapsLists() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "nested.simple.one[1]", "one[1]",
                        "nested.simple.one[0]", "one[0]"))
                .withSources(config(
                        "nested.map.one[1].two", "one[1]-two",
                        "nested.map.one[0].two", "one[0]-two"))
                .withSources(config(
                        "nested.key-converter.1[0].two", "one[0]-two"))
                .withSources(config(
                        "nested.mixed.one[0].one[0].one", "one[0].one[0].one",
                        "nested.mixed.one[0].one[0].two", "one[0].one[0].two"))
                .withMapping(NestedMapsLists.class)
                .build();

        NestedMapsLists mapping = config.getConfigMapping(NestedMapsLists.class);

        assertEquals("one[0]", mapping.simple().get("one").get(0));
        assertEquals("one[1]", mapping.simple().get("one").get(1));

        assertEquals("one[0]-two", mapping.map().get("one").get(0).get("two"));
        assertEquals("one[1]-two", mapping.map().get("one").get(1).get("two"));

        assertEquals("one[0]-two", mapping.keyConverter().get("one").get(0).get("two"));

        assertEquals("one[0].one[0].one", mapping.mixed().get("one").get(0).get("one").get(0).get("one"));
        assertEquals("one[0].one[0].two", mapping.mixed().get("one").get(0).get("one").get(0).get("two"));
    }

    @ConfigMapping(prefix = "nested")
    interface NestedMapsLists {
        Map<String, List<String>> simple();

        Map<String, List<Map<String, String>>> map();

        Map<@WithConverter(KeyConverter.class) String, List<Map<String, String>>> keyConverter();

        Map<String, List<Map<String, List<Map<String, String>>>>> mixed();

        class KeyConverter implements Converter<String> {
            @Override
            public String convert(final String value) throws IllegalArgumentException, NullPointerException {
                return "one";
            }
        }
    }

    @Test
    void withParentList() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "list[0].value", "value",
                        "list[0].optional.value", "value",
                        "list[1].value", "value"))
                .withMapping(WithParentList.class)
                .build();

        WithParentList mapping = config.getConfigMapping(WithParentList.class);

        assertEquals("value", mapping.list().get(0).value());
        assertTrue(mapping.list().get(0).optional().isPresent());
        assertEquals("value", mapping.list().get(0).optional().get().value());
        assertEquals("value", mapping.list().get(1).value());
    }

    @ConfigMapping(prefix = "list")
    interface WithParentList {
        @WithParentName
        List<Nested> list();

        interface Nested {
            String value();

            Optional<NestedOptional> optional();

            interface NestedOptional {
                String value();
            }
        }
    }

    @Test
    void ambiguousMapKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "ambiguous.map-key.another-nested.name", "name",
                        "ambiguous.map-key.another-nested.keys", "values"))
                .withMapping(AmbiguousMapKeys.class)
                .build();

        AmbiguousMapKeys mapping = config.getConfigMapping(AmbiguousMapKeys.class);
        assertEquals("name", mapping.nested().get("map-key").anotherNested().name());
        assertEquals("values", mapping.nested().get("map-key").anotherNested().names().get("keys"));
    }

    @ConfigMapping(prefix = "ambiguous")
    interface AmbiguousMapKeys {
        @WithParentName
        Map<String, Nested> nested();

        interface Nested {
            AnotherNested anotherNested();

            interface AnotherNested {
                String name();

                @WithParentName
                Map<String, String> names();
            }
        }
    }

    @Test
    void overrideListDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "list.defaults.values[0]", "baz",
                        "list.defaults.list-nested[0].value", "value"))
                .withMapping(ListDefaults.class)
                .build();

        ListDefaults mapping = config.getConfigMapping(ListDefaults.class);
        assertEquals(1, mapping.values().size());
        assertEquals("baz", mapping.values().get(0));
        assertNull(config.getRawValue("list.defaults.values[9]"));
    }

    @ConfigMapping(prefix = "list.defaults")
    interface ListDefaults {
        @WithDefault("foo,bar")
        List<String> values();

        List<Nested> listNested();

        interface Nested {
            @WithDefault("value")
            String value();

            @WithDefault("one,two")
            List<String> list();
        }
    }

    @Test
    @Disabled(value = "@WithUnnamedKey not implemented for leaf Maps")
    void withUnnamedLeaf() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("unnamed.leaf", "unnamed")
                .withDefaultValue("unnamed.leaf.one", "one")
                .withMapping(WithUnnamedLeaf.class)
                .build();

        WithUnnamedLeaf mapping = config.getConfigMapping(WithUnnamedLeaf.class);
        assertEquals("unnamed", mapping.leaf().get(null));
        assertEquals("one", mapping.leaf().get("one"));
    }

    @ConfigMapping(prefix = "unnamed")
    interface WithUnnamedLeaf {
        //@WithUnnamedKey
        Map<String, String> leaf();
    }
}
