package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

class ConfigMappingInterfaceTest {
    @Test
    void configMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080")).build();
        final Server configProperties = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void noConfigMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getConfigMapping(Server.class, "server"));
        assertEquals("SRCFG00027: Could not find a mapping for " + Server.class.getName(), exception.getMessage());
    }

    @Test
    void unregisteredConfigMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("host", "localhost", "port", "8080")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getConfigMapping(Server.class));
        assertEquals("SRCFG00027: Could not find a mapping for " + Server.class.getName(), exception.getMessage());
    }

    @Test
    void unregistedPrefix() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class)
                .withSources(config("host", "localhost", "port", "8080")).build();
        final NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> config.getConfigMapping(Server.class, "server"));
        assertEquals("SRCFG00028: Could not find a mapping for " + Server.class.getName() + " with prefix server",
                exception.getMessage());
    }

    @Test
    void noPrefix() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class)
                .withSources(config("host", "localhost", "port", "8080")).build();
        final Server configProperties = config.getConfigMapping(Server.class);
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void configMappingBuilder() throws Exception {
        final ConfigMappingProvider configMappingProvider = ConfigMappingProvider.builder().addRoot("server", Server.class)
                .addIgnored("server.name").build();
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "server.name", "name")).build();

        configMappingProvider.mapConfiguration(config);
        final Server server = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void unknownConfigElement() {
        assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(Server.class, "server").build());
    }

    @Test
    void ignorePropertiesInUnregisteredRoots() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080", "client.name", "konoha"))
                .build();
        final Server configProperties = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void ignoreSomeProperties() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server")
                .withMapping(Client.class, "client")
                .withMappingIgnore("client.**")
                .withSources(config("server.host", "localhost", "server.port", "8080", "client.host", "localhost",
                        "client.port", "8080", "client.name", "konoha"))
                .build();

        final Server server = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        final Client client = config.getConfigMapping(Client.class, "client");
        assertEquals("localhost", client.host());
        assertEquals(8080, client.port());
    }

    @Test
    void ignoreProperties() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withMapping(Server.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080")).build();
        final Server configProperties = config.getConfigMapping(Server.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void validateUnknown() {
        assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().addDefaultSources().withMapping(Server.class).build());

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withMapping(Server.class)
                .withMapping(Server.class, "server")
                .withValidateUnknown(false)
                .withSources(config("server.host", "localhost", "server.port", "8080", "host", "localhost", "port", "8080"))
                .build();

        final Server configProperties = config.getConfigMapping(Server.class);
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void splitRoots() throws Exception {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();

        final ConfigMappingProvider configMappingProvider = ConfigMappingProvider.builder()
                .addRoot("server", SplitRootServerHostAndPort.class)
                .addRoot("server", SplitRootServerName.class)
                .build();

        configMappingProvider.mapConfiguration(config);

        final SplitRootServerHostAndPort server = config.getConfigMapping(SplitRootServerHostAndPort.class, "server");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        final SplitRootServerName name = config.getConfigMapping(SplitRootServerName.class, "server");
        assertEquals("konoha", name.name());
    }

    @Test
    void splitRootsInConfig() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.name",
                        "konoha"))
                .withMapping(SplitRootServerHostAndPort.class, "server")
                .withMapping(SplitRootServerName.class, "server")
                .build();
        final SplitRootServerHostAndPort server = config.getConfigMapping(SplitRootServerHostAndPort.class, "server");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void subGroups() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.name",
                        "konoha"))
                .withMapping(ServerSub.class, "server")
                .build();
        final ServerSub server = config.getConfigMapping(ServerSub.class, "server");
        assertEquals("localhost", server.subHostAndPort().host());
        assertEquals(8080, server.subHostAndPort().port());
        assertEquals("konoha", server.subName().name());
    }

    @Test
    void types() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("int", "9");
                put("long", "9999999999");
                put("float", "99.9");
                put("double", "99.99");
                put("char", "c");
                put("boolean", "true");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(SomeTypes.class)
                .build();
        final SomeTypes types = config.getConfigMapping(SomeTypes.class);

        assertEquals(9, types.intPrimitive());
        assertEquals(9, types.intWrapper());
        assertEquals(9999999999L, types.longPrimitive());
        assertEquals(9999999999L, types.longWrapper());
        assertEquals(99.9f, types.floatPrimitive());
        assertEquals(99.9f, types.floatWrapper());
        assertEquals(99.99, types.doublePrimitive());
        assertEquals(99.99, types.doubleWrapper());
        assertEquals('c', types.charPrimitive());
        assertEquals('c', types.charWrapper());
        assertTrue(types.booleanPrimitive());
        assertTrue(types.booleanWrapper());
    }

    @Test
    void optionals() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("server.host", "localhost");
                put("server.port", "8080");
                put("optional", "optional");
                put("optional.int", "9");
                put("info.name", "server");
                put("info.login", "login");
                put("info.password", "password");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(Optionals.class)
                .build();
        final Optionals optionals = config.getConfigMapping(Optionals.class);

        assertTrue(optionals.server().isPresent());
        assertEquals("localhost", optionals.server().get().host());
        assertEquals(8080, optionals.server().get().port());

        assertTrue(optionals.optional().isPresent());
        assertEquals("optional", optionals.optional().get());
        assertTrue(optionals.optionalInt().isPresent());
        assertEquals(9, optionals.optionalInt().getAsInt());

        assertFalse(optionals.info().isEmpty());
        assertEquals("server", optionals.info().get("info").name());
        assertTrue(optionals.info().get("info").login().isPresent());
        assertEquals("login", optionals.info().get("info").login().get());
        assertTrue(optionals.info().get("info").password().isPresent());
        assertEquals("password", optionals.info().get("info").password().get());
    }

    @Test
    void collectionTypes() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("strings", "foo,bar");
                put("ints", "1,2,3");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(CollectionTypes.class)
                .build();
        final CollectionTypes types = config.getConfigMapping(CollectionTypes.class);

        assertEquals(Stream.of("foo", "bar").collect(toList()), types.listStrings());
        assertEquals(Stream.of(1, 2, 3).collect(toList()), types.listInts());
    }

    @Test
    void maps() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("server.host", "localhost");
                put("server.port", "8080");

                put("server.server.host", "localhost");
                put("server.server.port", "8080");

                put("server.group.server.host", "localhost");
                put("server.group.server.port", "8080");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(Maps.class)
                .build();
        final Maps maps = config.getConfigMapping(Maps.class);

        assertEquals("localhost", maps.server().get("host"));
        assertEquals(8080, Integer.valueOf(maps.server().get("port")));

        assertEquals("localhost", maps.group().get("server").host());
        assertEquals(8080, maps.group().get("server").port());

        assertEquals("localhost", maps.groupParentName().get("server").host());
        assertEquals(8080, maps.groupParentName().get("server").port());
    }

    @Test
    void mapsEmptyPrefix() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("host", "localhost");
                put("port", "8080");

                put("server.host", "localhost");
                put("server.port", "8080");

                put("group.server.host", "localhost");
                put("group.server.port", "8080");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(Maps.class, "")
                .build();
        final Maps maps = config.getConfigMapping(Maps.class, "");

        assertEquals("localhost", maps.server().get("host"));
        assertEquals(8080, Integer.valueOf(maps.server().get("port")));

        assertEquals("localhost", maps.group().get("server").host());
        assertEquals(8080, maps.group().get("server").port());

        assertEquals("localhost", maps.groupParentName().get("server").host());
        assertEquals(8080, maps.groupParentName().get("server").port());
    }

    @Test
    void defaults() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Defaults.class)
                .build();
        final Defaults defaults = config.getConfigMapping(Defaults.class);

        assertEquals("foo", defaults.foo());
        assertEquals("bar", defaults.bar());
        assertEquals("foo", config.getRawValue("foo"));

        final List<String> propertyNames = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(propertyNames.contains("foo"));
    }

    @Test
    void converters() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("foo", "notbar"))
                .withMapping(Converters.class)
                .build();
        final Converters converters = config.getConfigMapping(Converters.class);

        assertEquals("bar", converters.foo());
    }

    @Test
    void mix() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("server.host", "localhost");
                put("server.port", "8080");
                put("server.name", "server");
                put("client.host", "clienthost");
                put("client.port", "80");
                put("client.name", "client");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(ComplexSample.class)
                .build();

        final ComplexSample sample = config.getConfigMapping(ComplexSample.class);
        assertEquals("localhost", sample.server().subHostAndPort().host());
        assertEquals(8080, sample.server().subHostAndPort().port());
        assertTrue(sample.client().isPresent());
        assertEquals("clienthost", sample.client().get().subHostAndPort().host());
        assertEquals(80, sample.client().get().subHostAndPort().port());
    }

    @Test
    void noDynamicValues() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Server.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(new MapBackedConfigSource("test", new HashMap<>(), Integer.MAX_VALUE) {
                    private int counter = 1;

                    @Override
                    public String getValue(final String propertyName) {
                        return counter++ + "";
                    }
                }).build();

        final Server server = config.getConfigMapping(Server.class, "server");

        assertNotEquals(config.getRawValue("server.port"), config.getRawValue("server.port"));
        assertEquals(server.port(), server.port());
    }

    @Test
    void mapClass() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerClass.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080")).build();
        final ServerClass server = config.getConfigMapping(ServerClass.class, "server");
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

    @Test
    void configMappingAnnotation() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerAnnotated.class, "server")
                .withMapping(ServerAnnotated.class, "cloud")
                .withSources(
                        config("server.host", "localhost", "server.port", "8080", "cloud.host", "cloud", "cloud.port", "9090"))
                .build();

        final ServerAnnotated server = config.getConfigMapping(ServerAnnotated.class, "server");
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        final ServerAnnotated cloud = config.getConfigMapping(ServerAnnotated.class);
        assertNotNull(cloud);
        assertEquals("cloud", cloud.host());
        assertEquals(9090, cloud.port());

        final ServerAnnotated cloudNull = config.getConfigMapping(ServerAnnotated.class, null);
        assertNotNull(cloudNull);
        assertEquals("cloud", cloudNull.host());
        assertEquals(9090, cloudNull.port());
    }

    @Test
    void prefixFromAnnotation() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerAnnotated.class)
                .withSources(config("cloud.host", "cloud", "cloud.port", "9090"))
                .build();

        final ServerAnnotated cloud = config.getConfigMapping(ServerAnnotated.class);
        assertNotNull(cloud);
        assertEquals("cloud", cloud.host());
        assertEquals(9090, cloud.port());
    }

    @Test
    void superTypes() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerChild.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .build();

        final ServerChild server = config.getConfigMapping(ServerChild.class, "server");
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void configValue() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerConfigValue.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .build();

        final ServerConfigValue server = config.getConfigMapping(ServerConfigValue.class, "server");
        assertNotNull(server);
        assertEquals("localhost", server.host().getValue());
        assertEquals(8080, Integer.valueOf(server.port().getValue()));
    }

    @Test
    void empty() {
        try {
            new SmallRyeConfigBuilder().withMapping(Empty.class, "empty").build();
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    interface Server {
        String host();

        int port();
    }

    interface Client {
        String host();

        int port();
    }

    interface SplitRootServerHostAndPort {
        String host();

        int port();
    }

    interface SplitRootServerName {
        String name();
    }

    interface ServerSub {
        @WithParentName
        ServerSubHostAndPort subHostAndPort();

        @WithParentName
        ServerSubName subName();
    }

    interface ServerSubHostAndPort {
        String host();

        int port();
    }

    interface ServerSubName {
        String name();
    }

    public interface SomeTypes {
        @WithName("int")
        int intPrimitive();

        @WithName("int")
        Integer intWrapper();

        @WithName("long")
        long longPrimitive();

        @WithName("long")
        Long longWrapper();

        @WithName("float")
        float floatPrimitive();

        @WithName("float")
        Float floatWrapper();

        @WithName("double")
        double doublePrimitive();

        @WithName("double")
        Double doubleWrapper();

        @WithName("char")
        char charPrimitive();

        @WithName("char")
        Character charWrapper();

        @WithName("boolean")
        boolean booleanPrimitive();

        @WithName("boolean")
        Boolean booleanWrapper();
    }

    public interface Optionals {
        Optional<Server> server();

        Optional<String> optional();

        @WithName("optional.int")
        OptionalInt optionalInt();

        @WithParentName
        Map<String, Info> info();

        interface Info {
            String name();

            Optional<String> login();

            Optional<String> password();
        }
    }

    public interface CollectionTypes {
        @WithName("strings")
        List<String> listStrings();

        @WithName("ints")
        List<Integer> listInts();
    }

    @ConfigMapping(prefix = "server")
    public interface Maps {
        @WithParentName
        Map<String, String> server();

        Map<String, Server> group();

        @WithParentName
        Map<String, Server> groupParentName();
    }

    public interface Defaults {
        @WithDefault("foo")
        String foo();

        @WithDefault("bar")
        String bar();
    }

    public interface ComplexSample {
        ServerSub server();

        Optional<ServerSub> client();
    }

    public interface Converters {
        @WithConverter(FooBarConverter.class)
        String foo();
    }

    public static class FooBarConverter implements Converter<String> {
        @Override
        public String convert(final String value) {
            return "bar";
        }
    }

    @ConfigMapping(prefix = "cloud")
    public interface ServerAnnotated {
        String host();

        int port();
    }

    public interface ServerParent {
        String host();
    }

    public interface ServerChild extends ServerParent {
        int port();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerConfigValue {
        ConfigValue host();

        ConfigValue port();
    }

    @ConfigMapping(prefix = "empty")
    public interface Empty {

    }

    @ConfigMapping(prefix = "server")
    public interface MapsInGroup {
        Info info();

        interface Info {
            String name();

            Map<String, String> values();

            Map<String, Data> data();
        }

        interface Data {
            String name();
        }
    }

    @Test
    void mapsInGroup() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("server.info.name", "naruto");
                put("server.info.values.name", "naruto");
                put("server.info.data.first.name", "naruto");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(MapsInGroup.class)
                .build();

        final MapsInGroup mapping = config.getConfigMapping(MapsInGroup.class);
        assertNotNull(mapping);
        assertEquals("naruto", mapping.info().name());
        assertEquals("naruto", mapping.info().values().get("name"));
        assertEquals("naruto", mapping.info().data().get("first").name());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerPrefix {
        String host();

        int port();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerNamePrefix {
        String host();
    }

    @Test
    void prefixPropertyInRoot() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerPrefix.class, "server")
                .withMapping(ServerPrefix.class, "cloud.server")
                .withMapping(ServerNamePrefix.class, "server")
                .withSources(config("serverBoot", "server"))
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(config("cloud.serverBoot", "server"))
                .withSources(config("cloud.server.host", "localhost", "cloud.server.port", "8080"))
                .build();

        ServerPrefix server = config.getConfigMapping(ServerPrefix.class, "server");
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        ServerPrefix serverCloud = config.getConfigMapping(ServerPrefix.class, "cloud.server");
        assertNotNull(server);
        assertEquals("localhost", serverCloud.host());
        assertEquals(8080, serverCloud.port());

        ServerNamePrefix serverName = config.getConfigMapping(ServerNamePrefix.class, "server");
        assertNotNull(server);
        assertEquals("localhost", serverName.host());
    }

    @Test
    void prefixPropertyInRootUnknown() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withMapping(ServerPrefix.class, "server")
                .withSources(config("serverBoot", "server"))
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(config("server.name", "localhost"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(exception.getCause() instanceof ConfigValidationException);
        assertEquals("server.name does not map to any root",
                ((ConfigValidationException) exception.getCause()).getProblem(0).getMessage());

        builder = new SmallRyeConfigBuilder()
                .withMapping(ServerPrefix.class, "server")
                .withMapping(ServerPrefix.class, "cloud.server")
                .withMapping(ServerNamePrefix.class, "server")
                .withSources(config("serverBoot", "server"))
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(config("cloud.serverBoot", "server"))
                .withSources(config("cloud.server.host", "localhost", "cloud.server.port", "8080"))
                .withSources(config("cloud.server.name", "localhost"));

        exception = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(exception.getCause() instanceof ConfigValidationException);
        assertEquals("cloud.server.name does not map to any root",
                ((ConfigValidationException) exception.getCause()).getProblem(0).getMessage());
    }

    @ConfigMapping(prefix = "mapping.server.env")
    public interface ServerMapEnv {
        @WithParentName
        Map<String, Info> info();

        interface Info {
            String name();

            List<String> alias();
        }
    }

    @Test
    void mapEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource())
                .withMapping(ServerMapEnv.class)
                .build();

        ServerMapEnv mapping = config.getConfigMapping(ServerMapEnv.class);

        assertEquals("development", mapping.info().get("localhost").name());
        assertEquals("dev", mapping.info().get("localhost").alias().get(0));
        assertEquals("production", mapping.info().get("cloud").name());
        assertEquals("prod", mapping.info().get("cloud").alias().get(0));
    }

    @ConfigMapping(prefix = "server")
    public interface ServerOptionalWithName {
        @WithName("a_server")
        Optional<Server> aServer();

        interface Server {
            @WithName("a_host")
            String host();

            @WithName("a_port")
            int port();
        }
    }

    @Test
    void optionalWithName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource())
                .withMapping(ServerOptionalWithName.class)
                .withSources(config("server.a_server.a_host", "localhost", "server.a_server.a_port", "8080"))
                .build();

        ServerOptionalWithName mapping = config.getConfigMapping(ServerOptionalWithName.class);
        assertTrue(mapping.aServer().isPresent());
        assertEquals("localhost", mapping.aServer().get().host());
        assertEquals(8080, mapping.aServer().get().port());
    }

    @ConfigMapping(prefix = "server")
    public interface ServerHierarchy extends Server {
        @WithParentName
        Named named();

        Optional<Alias> alias();

        interface Named {
            String name();
        }

        interface Alias extends Named {
            String alias();
        }
    }

    @Test
    void hierarchy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerHierarchy.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080",
                        "server.name", "server", "server.alias.name", "alias", "server.alias.alias", "alias"))
                .build();

        ServerHierarchy mapping = config.getConfigMapping(ServerHierarchy.class);
        assertEquals("localhost", mapping.host());
        assertEquals(8080, mapping.port());
        assertEquals("server", mapping.named().name());
        assertTrue(mapping.alias().isPresent());
        assertEquals("alias", mapping.alias().get().alias());
        assertEquals("alias", mapping.alias().get().name());
    }
}
