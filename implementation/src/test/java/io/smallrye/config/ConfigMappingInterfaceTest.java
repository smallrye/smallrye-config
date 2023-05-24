package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterfaceTest.HyphenatedEnumMapping.HyphenatedEnum.VALUE_ONE;
import static io.smallrye.config.ConfigMappingInterfaceTest.MapKeyEnum.ClientId.NAF;
import static io.smallrye.config.ConfigMappingInterfaceTest.MapKeyEnum.ClientId.SOS_DAH;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
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
    void unknownConfigElement() {
        assertThrows(ConfigValidationException.class,
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
        assertThrows(ConfigValidationException.class,
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
    void splitRoots() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();

        ConfigMappings.mapConfiguration(
                config, ConfigMappingProvider.builder()
                        .addRoot("server", SplitRootServerHostAndPort.class)
                        .addRoot("server", SplitRootServerName.class));

        SplitRootServerHostAndPort server = config.getConfigMapping(SplitRootServerHostAndPort.class, "server");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        SplitRootServerName name = config.getConfigMapping(SplitRootServerName.class, "server");
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

                put("server.server.host", "localhost-server");
                put("server.server.port", "8080");

                put("server.group.server.host", "localhost-group");
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

        assertEquals("localhost-group", maps.group().get("server").host());
        assertEquals(8080, maps.group().get("server").port());

        assertEquals("localhost-server", maps.groupParentName().get("server").host());
        assertEquals(8080, maps.groupParentName().get("server").port());
    }

    @Test
    void mapsEmptyPrefix() {
        final Map<String, String> typesConfig = new HashMap<>() {
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

        List<String> propertyNames = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertTrue(propertyNames.contains("foo"));
        assertTrue(propertyNames.contains("bar"));
    }

    @Test
    void converters() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("foo", "notbar"))
                .withMapping(Converters.class)
                .build();
        final Converters converters = config.getConfigMapping(Converters.class);

        assertEquals("bar", converters.foo());
        assertTrue(converters.bprim());
        assertEquals('c', converters.cprim());
        assertEquals(-1, converters.iprim());
        assertEquals(-1, converters.sprim());
        assertEquals(-1L, converters.lprim());
        assertEquals(-1.0, converters.fprim());
        assertEquals(-1.0, converters.dprim());
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

        @WithConverter(BooleanConverter.class)
        boolean bprim();

        @WithConverter(CharacterConverter.class)
        char cprim();

        @WithConverter(IntegerConverter.class)
        int iprim();

        @WithConverter(ShortConverter.class)
        short sprim();

        @WithConverter(LongConverter.class)
        long lprim();

        @WithConverter(FloatConverter.class)
        float fprim();

        @WithConverter(DoubleConverter.class)
        double dprim();
    }

    public static class FooBarConverter implements Converter<String> {
        @Override
        public String convert(final String value) {
            return "bar";
        }
    }

    public static class BooleanConverter implements Converter<Boolean> {
        @Override
        public Boolean convert(String value) throws IllegalArgumentException, NullPointerException {
            return true;
        }
    }

    public static class CharacterConverter implements Converter<Character> {
        @Override
        public Character convert(String value) throws IllegalArgumentException, NullPointerException {
            return 'c';
        }
    }

    public static class IntegerConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) throws IllegalArgumentException, NullPointerException {
            return -1;
        }
    }

    public static class ShortConverter implements Converter<Short> {
        @Override
        public Short convert(String value) throws IllegalArgumentException, NullPointerException {
            return -1;
        }
    }

    public static class LongConverter implements Converter<Long> {
        @Override
        public Long convert(String value) throws IllegalArgumentException, NullPointerException {
            return -1L;
        }
    }

    public static class FloatConverter implements Converter<Float> {
        @Override
        public Float convert(String value) throws IllegalArgumentException, NullPointerException {
            return -1.0F;
        }
    }

    public static class DoubleConverter implements Converter<Double> {
        @Override
        public Double convert(String value) throws IllegalArgumentException, NullPointerException {
            return -1.0;
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

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, builder::build);
        assertEquals("server.name does not map to any root", exception.getProblem(0).getMessage());

        builder = new SmallRyeConfigBuilder()
                .withMapping(ServerPrefix.class, "server")
                .withMapping(ServerPrefix.class, "cloud.server")
                .withMapping(ServerNamePrefix.class, "server")
                .withSources(config("serverBoot", "server"))
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(config("cloud.serverBoot", "server"))
                .withSources(config("cloud.server.host", "localhost", "cloud.server.port", "8080"))
                .withSources(config("cloud.server.name", "localhost"));

        exception = assertThrows(ConfigValidationException.class, builder::build);
        assertEquals("cloud.server.name does not map to any root", exception.getProblem(0).getMessage());
    }

    @ConfigMapping(prefix = "mapping.server.env")
    public interface ServerMapEnv {
        @WithParentName
        Map<String, Info> info();

        String simpleDashedProperty();

        interface Info {
            String name();

            List<String> alias();
        }
    }

    @Test
    void mapEnv() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(new HashMap<String, String>() {
                    {
                        put("MAPPING_SERVER_ENV__LOCALHOST__NAME", "development");
                        put("MAPPING_SERVER_ENV__LOCALHOST__ALIAS", "dev");
                        put("MAPPING_SERVER_ENV_CLOUD_NAME", "production");
                        put("MAPPING_SERVER_ENV_CLOUD_ALIAS", "prod");
                        put("MAPPING_SERVER_ENV_SIMPLE_DASHED_PROPERTY", "5678");
                        put("mapping.server.env.simple.dashed.property", "5678");
                    }
                }, 300))
                .withSources(config("mapping.server.env.simple-dashed-property", "1234"))
                .withMapping(ServerMapEnv.class)
                .build();

        ServerMapEnv mapping = config.getConfigMapping(ServerMapEnv.class);

        assertEquals("development", mapping.info().get("localhost").name());
        assertEquals("dev", mapping.info().get("localhost").alias().get(0));
        assertEquals("production", mapping.info().get("cloud").name());
        assertEquals("prod", mapping.info().get("cloud").alias().get(0));
        assertEquals("5678", mapping.simpleDashedProperty());
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

    @ConfigMapping(prefix = "server")
    public interface ServerExpandDefaults {
        @WithDefault("localhost")
        String host();

        String url();
    }

    @Test
    void expandDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(ServerExpandDefaults.class, "server")
                .withSources(config("server.url", "http://${server.host}"))
                .build();

        ServerExpandDefaults mapping = config.getConfigMapping(ServerExpandDefaults.class);
        assertEquals("localhost", mapping.host());
        assertEquals("http://localhost", mapping.url());
    }

    interface ServerBase {
        String host();

        int port();

        Map<String, String> properties();

        Map<String, ServerAlias> otherAlias();

        ServerAlias alias();

        Optional<ServerAlias> optionalAlias();

        List<ServerAlias> aliases();
    }

    @ConfigMapping(prefix = "server")
    interface ServerOverrides extends ServerBase {
        @Override
        String host();

        @Override
        int port();

        @Override
        Map<String, String> properties();

        @Override
        Map<String, ServerAlias> otherAlias();

        @Override
        ServerAlias alias();

        @Override
        Optional<ServerAlias> optionalAlias();

        @Override
        List<ServerAlias> aliases();
    }

    interface ServerAlias {
        String name();
    }

    @Test
    void hierarchyOverrides() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ServerOverrides.class, "server")
                .withSources(config("server.host", "localhost", "server.port", "8080"))
                .withSources(config("server.properties.host", "localhost", "server.properties.port", "8080"))
                .withSources(config("server.other-alias.other.name", "other"))
                .withSources(config("server.alias.name", "server"))
                .withSources(config("server.optional-alias.name", "server"))
                .withSources(config("server.aliases[0].name", "server"))
                .withValidateUnknown(false)
                .build();

        ServerOverrides mapping = config.getConfigMapping(ServerOverrides.class, "server");
        assertEquals("localhost", mapping.host());
        assertEquals(8080, mapping.port());
        assertFalse(mapping.properties().isEmpty());
        assertEquals("localhost", mapping.properties().get("host"));
        assertEquals("8080", mapping.properties().get("port"));
        assertFalse(mapping.otherAlias().isEmpty());
        assertEquals("other", mapping.otherAlias().get("other").name());
        assertEquals("server", mapping.alias().name());
        assertTrue(mapping.optionalAlias().isPresent());
        assertEquals("server", mapping.optionalAlias().get().name());
        assertFalse(mapping.aliases().isEmpty());
        assertEquals("server", mapping.aliases().get(0).name());
    }

    @ConfigMapping(prefix = "maps")
    interface NestedGroupMaps {
        Map<String, User> users();

        interface User {
            int age();

            Map<String, String> items();

            Map<String, Address> addresses();

            interface Address {
                String location();

                Map<String, String> alias();
            }
        }
    }

    @Test
    void nestedGroupMaps() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedGroupMaps.class, "maps")
                .withSources(config("maps.users.naruto.age", "17",
                        "maps.users.naruto.items.attack1", "rasengan",
                        "maps.users.naruto.addresses.home.location", "Konoha",
                        "maps.users.naruto.addresses.home.alias.konoha", "Konohagakure"))
                .withSources(config("maps.users.sasuke.age", "18", "maps.users.sasuke.items.attack1", "chidori"))
                .build();

        NestedGroupMaps mapping = config.getConfigMapping(NestedGroupMaps.class);
        assertEquals(2, mapping.users().size());
        assertEquals(17, mapping.users().get("naruto").age());
        assertEquals("rasengan", mapping.users().get("naruto").items().get("attack1"));
        assertEquals("Konoha", mapping.users().get("naruto").addresses().get("home").location());
        assertEquals("Konohagakure", mapping.users().get("naruto").addresses().get("home").alias().get("konoha"));
        assertEquals(18, mapping.users().get("sasuke").age());
        assertEquals("chidori", mapping.users().get("sasuke").items().get("attack1"));
    }

    @ConfigMapping(prefix = "path")
    interface MappingPath {
        Path path();
    }

    @Test
    void path() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MappingPath.class, "path")
                .withSources(config("path.path", "path"))
                .withConverter(Path.class, 100, value -> value.isEmpty() ? null : Paths.get(value))
                .build();

        MappingPath mapping = config.getConfigMapping(MappingPath.class);
        assertEquals("path", mapping.path().toString());
    }

    @ConfigMapping(prefix = "map")
    public interface DottedKeyInMap {
        @WithParentName
        Map<String, String> map();
    }

    @Test
    void properties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DottedKeyInMap.class, "map")
                .withSources(config("map.key", "1234", "map.\"dotted.key\"", "5678"))
                .build();

        DottedKeyInMap mapping = config.getConfigMapping(DottedKeyInMap.class);
        assertEquals("1234", mapping.map().get("key"));
        assertEquals("5678", mapping.map().get("dotted.key"));
    }

    // From https://github.com/quarkusio/quarkus/issues/20728
    @ConfigMapping(prefix = "clients")
    public interface BugsConfiguration {
        @WithParentName
        Map<String, ClientConfiguration> clients();

        interface ClientConfiguration {
            MediumProperties medium();

            CreatedByProperties app();

            EnabledProperties callback();

            EnabledProperties task();
        }

        interface MediumProperties {
            boolean web();

            boolean app();
        }

        interface CreatedByProperties {
            String createdByApplication();
        }

        interface EnabledProperties {
            boolean enabled();
        }
    }

    @Test
    void mapWithMultipleGroupsAndSameMethodNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(BugsConfiguration.class, "clients")
                .withSources(config(
                        "clients.naruto.medium.web", "true",
                        "clients.naruto.medium.app", "true",
                        "clients.naruto.app.created-by-application", "app",
                        "clients.naruto.callback.enabled", "true",
                        "clients.naruto.task.enabled", "true"))
                .build();

        BugsConfiguration mapping = config.getConfigMapping(BugsConfiguration.class);

        assertTrue(mapping.clients().get("naruto").medium().web());
        assertTrue(mapping.clients().get("naruto").medium().app());
        assertEquals("app", mapping.clients().get("naruto").app().createdByApplication());
        assertTrue(mapping.clients().get("naruto").callback().enabled());
        assertTrue(mapping.clients().get("naruto").task().enabled());
    }

    @ConfigMapping(prefix = "defaults")
    public interface DefaultMethods {
        default String host() {
            return "localhost";
        }

        @WithDefault("8080")
        int port();
    }

    @Test
    void defaultMethods() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DefaultMethods.class, "defaults")
                .build();

        DefaultMethods mapping = config.getConfigMapping(DefaultMethods.class);
        assertEquals("localhost", mapping.host());
        assertEquals(8080, mapping.port());
    }

    @ConfigMapping(prefix = "defaults")
    public interface DefaultKotlinMethods {
        String host();

        @WithDefault("8080")
        int port();

        List<String> servers();

        Optional<String> optional();

        int anotherPort();

        final class DefaultImpls {
            public static String host(DefaultKotlinMethods config) {
                return "localhost";
            }

            public static int port() {
                throw new IllegalStateException();
            }

            public static List<String> servers(DefaultKotlinMethods config) {
                return singletonList("server");
            }

            public static Optional<String> optional(DefaultKotlinMethods config) {
                return Optional.of("optional");
            }

            public static int anotherPort(DefaultKotlinMethods config) {
                return 9;
            }
        }
    }

    @Test
    void defaultKotlinMethods() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DefaultKotlinMethods.class, "defaults")
                .build();

        DefaultKotlinMethods mapping = config.getConfigMapping(DefaultKotlinMethods.class);
        assertEquals("localhost", mapping.host());
        assertEquals(8080, mapping.port());
        assertEquals(singletonList("server"), mapping.servers());
        assertTrue(mapping.optional().isPresent());
        assertEquals("optional", mapping.optional().get());
        assertEquals(9, mapping.anotherPort());
    }

    @ConfigMapping(prefix = "clients", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    public interface MapKeyEnum {
        enum ClientId {
            SOS_DAH,
            NAF
        }

        @WithParentName
        Map<ClientId, ClientConfiguration> clients();

        interface ClientConfiguration {
            Optional<CreatedByProperties> app();

            CreatedByProperties web();

            MediumProperties medium();
        }

        interface CreatedByProperties {
            String createdByApplication();
        }

        interface MediumProperties {
            boolean web();

            @WithDefault("false")
            boolean app();
        }
    }

    @Test
    void mapKeyEnum() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapKeyEnum.class, "clients")
                .withSources(config(
                        "clients.SOS_DAH.web.createdByApplication", "RoadrunnerWeb",
                        "clients.SOS_DAH.app.createdByApplication", "Roadrunner",
                        "clients.SOS_DAH.medium.web", "true",
                        "clients.SOS_DAH.medium.app", "true",
                        "clients.NAF.web.createdByApplication", "RoadrunnerWebNAF",
                        "clients.NAF.medium.web", "true"))
                .build();

        MapKeyEnum mapping = config.getConfigMapping(MapKeyEnum.class);

        assertTrue(mapping.clients().get(SOS_DAH).app().isPresent());
        assertEquals("Roadrunner", mapping.clients().get(SOS_DAH).app().get().createdByApplication());
        assertEquals("RoadrunnerWeb", mapping.clients().get(SOS_DAH).web().createdByApplication());
        assertTrue(mapping.clients().get(SOS_DAH).medium().web());
        assertTrue(mapping.clients().get(SOS_DAH).medium().app());

        assertFalse(mapping.clients().get(NAF).app().isPresent());
        assertEquals("RoadrunnerWebNAF", mapping.clients().get(NAF).web().createdByApplication());
        assertTrue(mapping.clients().get(NAF).medium().web());
        assertFalse(mapping.clients().get(NAF).medium().app());
    }

    @ConfigMapping
    interface HyphenatedEnumMapping {
        HyphenatedEnum hyphenatedEnum();

        enum HyphenatedEnum {
            VALUE_ONE
        }
    }

    @Test
    void hyphenateEnum() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(HyphenatedEnumMapping.class)
                .withSources(config("hyphenated-enum", "value-one"))
                .build();

        HyphenatedEnumMapping mapping = config.getConfigMapping(HyphenatedEnumMapping.class);
        assertEquals(VALUE_ONE, mapping.hyphenatedEnum());
        assertEquals(VALUE_ONE, config.getValue("hyphenated-enum", HyphenatedEnumMapping.HyphenatedEnum.class));
    }

    @ConfigMapping
    interface ListConverter {
        @WithConverter(RangeConverter.class)
        Range range();

        @WithConverter(RangeConverter.class)
        List<Range> list();

        class Range {
            private final Integer min;
            private final Integer max;

            public Range(final Integer min, final Integer max) {
                this.min = min;
                this.max = max;
            }

            public Integer getMin() {
                return min;
            }

            public Integer getMax() {
                return max;
            }
        }

        class RangeConverter implements Converter<Range> {
            @Override
            public Range convert(final String value) throws IllegalArgumentException, NullPointerException {
                String[] split = value.split("-");
                return new Range(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
            }
        }
    }

    @Test
    void listElementConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ListConverter.class)
                .withSources(config("range", "1-10", "list", "1-10,2-20"))
                .build();

        ListConverter mapping = config.getConfigMapping(ListConverter.class);
        assertEquals(1, mapping.range().getMin());
        assertEquals(10, mapping.range().getMax());

        assertEquals(1, mapping.list().get(0).getMin());
        assertEquals(10, mapping.list().get(0).getMax());
        assertEquals(2, mapping.list().get(1).getMin());
        assertEquals(20, mapping.list().get(1).getMax());
    }

    @ConfigMapping(prefix = "my-app.rest-config.my-client")
    public interface MyRestClientConfig {
        @WithParentName
        Optional<RestClientConfig> client();

        interface RestClientConfig {
            URI baseUri();

            KeystoreConfig keystore();

            List<Endpoint> endpoints();

            interface KeystoreConfig {
                Optional<String> type();

                Path path();

                String password();
            }

            interface Endpoint {
                String path();

                List<String> methods();
            }
        }
    }

    @Test
    void envPropertiesWithoutDottedProperties() {
        Map<String, String> env = new HashMap<String, String>() {
            {
                put("MY_APP_REST_CONFIG_MY_CLIENT_BASE_URI", "http://localhost:8080");
                put("MY_APP_REST_CONFIG_MY_CLIENT_KEYSTORE_PATH", "config/keystores/my-keys.p12");
                put("MY_APP_REST_CONFIG_MY_CLIENT_KEYSTORE_PASSWORD", "p@ssw0rd");
                put("MY_APP_REST_CONFIG_MY_CLIENT_ENDPOINTS_0__PATH", "/hello");
                put("MY_APP_REST_CONFIG_MY_CLIENT_ENDPOINTS_0__METHODS_0_", "GET");
                put("MY_APP_REST_CONFIG_MY_CLIENT_ENDPOINTS_0__METHODS_1_", "POST");
            }
        };

        EnvConfigSource envConfigSource = new EnvConfigSource(env, 300);
        assertNotNull(envConfigSource.getValue("my-app.rest-config.my-client.base-uri"));
        assertNotNull(envConfigSource.getValue("my-app.rest-config.my-client.keystore.password"));

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MyRestClientConfig.class)
                .withSources(envConfigSource)
                .withConverter(Path.class, 100, (Converter<Path>) Paths::get)
                .build();

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertTrue(properties.contains("my-app.rest-config.my-client.base-uri"));
        assertTrue(properties.contains("my-app.rest-config.my-client.keystore.path"));
        assertTrue(properties.contains("my-app.rest-config.my-client.keystore.password"));
        assertTrue(properties.contains("my-app.rest-config.my-client.endpoints[0].path"));
        assertTrue(properties.contains("my-app.rest-config.my-client.endpoints[0].methods[0]"));
        assertTrue(properties.contains("my-app.rest-config.my-client.endpoints[0].methods[1]"));

        MyRestClientConfig mapping = config.getConfigMapping(MyRestClientConfig.class);
        assertTrue(mapping.client().isPresent());
        assertEquals(URI.create("http://localhost:8080"), mapping.client().get().baseUri());
        assertEquals(Paths.get("config/keystores/my-keys.p12"), mapping.client().get().keystore().path());
        assertEquals("p@ssw0rd", mapping.client().get().keystore().password());
        assertFalse(mapping.client().get().endpoints().isEmpty());
        assertEquals("/hello", mapping.client().get().endpoints().get(0).path());
        assertEquals("GET", mapping.client().get().endpoints().get(0).methods().get(0));
        assertEquals("POST", mapping.client().get().endpoints().get(0).methods().get(1));
    }

    @ConfigMapping(prefix = "optionals")
    interface NestedOptionals {
        Optional<First> first();

        interface First {
            Optional<Second> second();

            interface Second {
                String value();
            }
        }
    }

    @Test
    void nestedOptionals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedOptionals.class)
                .withSources(config("optionals.first.second.value", "value"))
                .build();

        NestedOptionals mapping = config.getConfigMapping(NestedOptionals.class);

        assertTrue(mapping.first().isPresent());
        assertTrue(mapping.first().get().second().isPresent());
        assertEquals("value", mapping.first().get().second().get().value());
    }

    @ConfigMapping(prefix = "optionals")
    interface NestedOptionalMap {
        Optional<First> first();

        interface First {
            Map<String, Second> second();
        }

        interface Second {
            Optional<String> value();
        }
    }

    @Test
    void nestedOptionalMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedOptionalMap.class)
                .withSources(config("optionals.first.second.key.value", "value"))
                .build();

        NestedOptionalMap mapping = config.getConfigMapping(NestedOptionalMap.class);

        assertTrue(mapping.first().isPresent());
        assertEquals(1, mapping.first().get().second().size());
        assertNotNull(mapping.first().get().second().get("key"));
        assertTrue(mapping.first().get().second().get("key").value().isPresent());
        assertEquals("value", mapping.first().get().second().get("key").value().get());
    }

    @ConfigMapping(prefix = "optionals")
    interface NestedOptionalsGroupMap {
        Optional<First> first();

        interface First {
            Optional<Second> second();
        }

        interface Second {
            Optional<Third> third();
        }

        interface Third {
            Map<String, String> properties();

            String value();
        }
    }

    @Test
    void nestedOptionalsGroupMap() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedOptionalsGroupMap.class)
                .withSources(config("optionals.first.second.third.properties.key", "value"))
                .withSources(config("optionals.first.second.third.value", "value"))
                .build();

        NestedOptionalsGroupMap mapping = config.getConfigMapping(NestedOptionalsGroupMap.class);

        assertTrue(mapping.first().isPresent());
        assertTrue(mapping.first().get().second().isPresent());
        assertTrue(mapping.first().get().second().get().third().isPresent());
        assertEquals("value", mapping.first().get().second().get().third().get().properties().get("key"));
        assertEquals("value", mapping.first().get().second().get().third().get().value());
    }

    @ConfigMapping(prefix = "optional-map")
    interface NestedOptionalMapGroup {
        Optional<Boolean> enable();

        Map<String, Map<String, MessageUtilConfiguration>> map();

        interface MessageUtilConfiguration {
            Optional<Boolean> enable();
        }
    }

    @Test
    void nestedOptionalAndMaps() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(NestedOptionalMapGroup.class)
                .withSources(config("optional-map.enable", "true"))
                .withSources(config("optional-map.map.filter.default.enable", "false",
                        "optional-map.map.filter.get-jokes-uni.enable", "true"))
                .withSources(config("optional-map.map.client.reaction-api.enable", "true",
                        "optional-map.map.client.setup-api.enable", "true"))
                .build();

        NestedOptionalMapGroup mapping = config.getConfigMapping(NestedOptionalMapGroup.class);
        assertTrue(mapping.enable().isPresent());
        assertTrue(mapping.enable().get());

        assertEquals(2, mapping.map().size());
        assertTrue(mapping.map().containsKey("filter"));
        assertTrue(mapping.map().get("filter").containsKey("default"));
        assertTrue(mapping.map().get("filter").containsKey("get-jokes-uni"));
        assertTrue(mapping.map().containsKey("client"));
        assertTrue(mapping.map().get("client").containsKey("reaction-api"));
        assertTrue(mapping.map().get("client").containsKey("setup-api"));
        assertTrue(mapping.map().get("filter").get("default").enable().isPresent());
        assertFalse(mapping.map().get("filter").get("default").enable().get());
        assertTrue(mapping.map().get("filter").get("get-jokes-uni").enable().isPresent());
        assertTrue(mapping.map().get("filter").get("get-jokes-uni").enable().get());
        assertTrue(mapping.map().get("client").get("reaction-api").enable().isPresent());
        assertTrue(mapping.map().get("client").get("reaction-api").enable().get());
        assertTrue(mapping.map().get("client").get("setup-api").enable().isPresent());
        assertTrue(mapping.map().get("client").get("setup-api").enable().get());
    }

    @ConfigMapping(prefix = "optional")
    interface OptionalExpressions {
        Optional<String> expression();

        OptionalInt expressionInt();
    }

    @Test
    void optionalExpressions() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(OptionalExpressions.class)
                .withSources(config("optional.expression", "${expression}"))
                .withSources(config("optional.expression-int", "${expression}"))
                .build();

        OptionalExpressions mapping = config.getConfigMapping(OptionalExpressions.class);

        assertFalse(mapping.expression().isPresent());
        assertFalse(mapping.expressionInt().isPresent());
    }

    @Test
    void defaultsBuilderAndMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(DefaultsBuilderAndMapping.class)
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue(SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false")
                .build();

        DefaultsBuilderAndMapping mapping = config.getConfigMapping(DefaultsBuilderAndMapping.class);

        assertEquals("localhost", config.getRawValue("server.host"));
        assertEquals(443, mapping.ssl().port());
        assertEquals(2, mapping.ssl().protocols().size());
    }

    @ConfigMapping(prefix = "server")
    interface DefaultsBuilderAndMapping {
        Ssl ssl();

        interface Ssl {
            @WithDefault("443")
            int port();

            @WithDefault("TLSv1.3,TLSv1.2")
            List<String> protocols();
        }
    }

    @Test
    void ignoreNestedUnknown() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(IgnoreNestedUnknown.class)
                .withSources(config("ignore.nested.value", "value", "ignore.nested.ignore", "ignore"))
                .withSources(config("ignore.nested.nested.value", "value", "ignore.nested.nested.ignore", "ignore"))
                .withSources(config("ignore.nested.optional.value", "value", "ignore.nested.optional.ignore", "ignore"))
                .withSources(config("ignore.nested.list[0].value", "value", "ignore.nested.list[0].ignore", "ignore"))
                .withSources(config("ignore.nested.map.key.value", "value", "ignore.nested.map.ignored.ignored", "ignore"))
                .withSources(config("ignore.nested.key.value", "parent", "ignore.nested.ignored.ignored", "ignore"))
                .withSources(config("ignore.nested.ignore.ignore", "ignore"))
                .withMappingIgnore("ignore.**")
                .build();

        IgnoreNestedUnknown mapping = config.getConfigMapping(IgnoreNestedUnknown.class);

        assertEquals("value", mapping.value());
        assertEquals("value", mapping.nested().value());
        assertTrue(mapping.optional().isPresent());
        assertEquals("value", mapping.optional().get().value());
        assertEquals("value", mapping.list().get(0).value());
        assertEquals("value", mapping.map().get("key").value());
        assertEquals("parent", mapping.mapParent().get("key").value());
    }

    @ConfigMapping(prefix = "ignore.nested")
    interface IgnoreNestedUnknown {
        String value();

        Nested nested();

        Optional<Nested> optional();

        List<Nested> list();

        Map<String, Nested> map();

        @WithParentName
        Map<String, Nested> mapParent();

        interface Nested {
            String value();
        }
    }

    @Test
    void ignorePartial() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(IgnorePartial.class)
                .withSources(config("path.key", "value"))
                .withSources(config("path.main.key", "value"))
                .withSources(config("path.ignore.key", ""))
                .withMappingIgnore("path.ignore.**")
                .build();

        IgnorePartial mapping = config.getConfigMapping(IgnorePartial.class);

        assertEquals("value", mapping.common().get("key"));
        assertEquals("value", mapping.main().get("key"));
    }

    @ConfigMapping(prefix = "path")
    interface IgnorePartial {
        @WithParentName
        Map<String, String> common();

        Map<String, String> main();
    }

    @Test
    void withNameDotted() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(WithNameDotted.class)
                .withSources(config("with.name.dotted.name", "value"))
                .withSources(config("with.name.nested.dotted.name", "value"))
                .withSources(config(
                        "with.name.map.key.name", "value",
                        "with.name.map.key.dotted.name", "value",
                        "with.name.map.key.dotted.another.name", "another",
                        "with.name.map.key.dotted.description", "value"))
                .withSources(config(
                        "with.name.nested.map.key.nested-key.name", "value",
                        "with.name.nested.map.key.nested-key.dotted.name", "value",
                        "with.name.nested.map.key.nested-key.dotted.another.name", "another",
                        "with.name.nested.map.key.nested-key.dotted.description", "value"))
                .build();

        WithNameDotted mapping = config.getConfigMapping(WithNameDotted.class);

        assertEquals("value", mapping.dottedName());
        assertEquals("value", mapping.nested().dottedName());
        assertEquals("default", mapping.nested().dotted().name());
        assertEquals("value", mapping.map().get("key").dottedName());
        assertEquals("value", mapping.map().get("key").name());
        assertEquals("another", mapping.map().get("key").dotted().name());
        assertEquals("value", mapping.map().get("key").dotted().description());
        assertEquals("value", mapping.nestedMap().get("key").get("nested-key").name());
        assertEquals("another", mapping.nestedMap().get("key").get("nested-key").dotted().name());
        assertEquals("value", mapping.nestedMap().get("key").get("nested-key").dotted().description());
    }

    @ConfigMapping(prefix = "with.name")
    interface WithNameDotted {
        @WithName("dotted.name")
        String dottedName();

        Nested nested();

        Map<String, Nested> map();

        @WithName("nested.map")
        Map<String, Map<String, Nested>> nestedMap();

        interface Nested {
            @WithName("dotted.name")
            String dottedName();

            @WithDefault("default")
            String name();

            Dotted dotted();

            interface Dotted {
                @WithName("another.name")
                @WithDefault("default")
                String name();

                @WithDefault("default")
                String description();
            }
        }
    }

    @Test
    void mapKeyLevels() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MapKeyLevels.class)
                .withSources(config("map.1.two.value", "value"))
                .withSources(config("map.one.2.3.value", "value"))
                .build();

        MapKeyLevels mapping = config.getConfigMapping(MapKeyLevels.class);

        assertEquals("value", mapping.two().get(1L).get("two").value());
        assertEquals("value", mapping.three().get("one").get(2).get(3L).value());
    }

    @ConfigMapping(prefix = "map")
    public interface MapKeyLevels {
        @WithParentName
        Map<Long, Map<String, Value>> two();

        @WithParentName
        Map<String, Map<Integer, Map<Long, Value>>> three();

        interface Value {
            String value();
        }
    }

    @Test
    void optionalWithConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(OptionalWithConverter.class)
                .withSources(config("optional.converter.value", "value"))
                .withSources(config("optional.converter.optional-value", "value"))
                .withSources(config("optional.converter.primitive", "1"))
                .withSources(config("optional.converter.wrapper-int", "1"))
                .withSources(config("optional.converter.primitive-array", "dummy"))
                .build();

        OptionalWithConverter mapping = config.getConfigMapping(OptionalWithConverter.class);

        assertEquals("value", mapping.value().value);
        assertTrue(mapping.optionalValue().isPresent());
        assertEquals("value", mapping.optionalValue().get().value);
        assertEquals(0, mapping.primitive());
        assertTrue(mapping.wrapperInt().isPresent());
        assertEquals(0, mapping.wrapperInt().get());
    }

    @ConfigMapping(prefix = "optional.converter")
    interface OptionalWithConverter {
        @WithConverter(ValueConverter.class)
        Value value();

        @WithConverter(ValueConverter.class)
        Optional<Value> optionalValue();

        @WithConverter(IntConverter.class)
        int primitive();

        @WithConverter(IntConverter.class)
        Optional<Integer> wrapperInt();

        @WithConverter(ByteArrayConverter.class)
        byte[] primitiveArray();
    }

    public static class ValueConverter implements Converter<Value> {
        @Override
        public Value convert(final String value) throws IllegalArgumentException, NullPointerException {
            return new Value(value);
        }
    }

    public static class IntConverter implements Converter<Integer> {
        @Override
        public Integer convert(final String value) throws IllegalArgumentException, NullPointerException {
            return 0;
        }
    }

    public static class ByteArrayConverter implements Converter<byte[]> {
        @Override
        public byte[] convert(String value) throws IllegalArgumentException, NullPointerException {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    static class Value {
        String value;

        Value(final String value) {
            this.value = value;
        }
    }

    @Test
    void expressionDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(ExpressionDefaults.class)
                .withDefaultValue("expression", "1234")
                .build();

        ExpressionDefaults mapping = config.getConfigMapping(ExpressionDefaults.class);

        assertEquals("1234", mapping.expression());
    }

    @ConfigMapping(prefix = "expression.defaults")
    interface ExpressionDefaults {
        @WithDefault("${expression}")
        String expression();
    }

    @Test
    void mapKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(MapKeys.class)
                .withSources(config(
                        "keys.map.one", "1",
                        "keys.map.one.two", "2",
                        "keys.map.one.two.three", "3",
                        "keys.map.\"one.two.three.four\"", "4"))
                .withSources(config(
                        "keys.list.one[0]", "1",
                        "keys.list.one.two[0]", "2",
                        "keys.list.one.two.three[0]", "3",
                        "keys.list.\"one.two.three.four\"[0]", "4"))
                .build();

        MapKeys mapping = config.getConfigMapping(MapKeys.class);

        assertEquals(4, mapping.map().size());
        assertEquals("1", mapping.map().get("one"));
        assertEquals("2", mapping.map().get("one.two"));
        assertEquals("3", mapping.map().get("one.two.three"));
        assertEquals("4", mapping.map().get("one.two.three.four"));

        assertEquals("1", mapping.list().get("one").get(0));
        assertEquals("2", mapping.list().get("one.two").get(0));
        assertEquals("3", mapping.list().get("one.two.three").get(0));
        assertEquals("4", mapping.list().get("one.two.three.four").get(0));
    }

    @ConfigMapping(prefix = "keys")
    interface MapKeys {
        Map<String, String> map();

        Map<String, List<String>> list();
    }

    @Test
    void defaultsPropertyNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(DefaultsPropertyNames.class)
                .build();

        DefaultsPropertyNames mapping = config.getConfigMapping(DefaultsPropertyNames.class);
        assertEquals("value", mapping.value());
        assertEquals("value", config.getRawValue("defaults.myProperty"));

        Set<String> properties = stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertTrue(properties.contains("defaults.myProperty"));
    }

    @ConfigMapping(prefix = "defaults")
    interface DefaultsPropertyNames {
        @WithDefault("value")
        @WithName("myProperty")
        String value();
    }

    @Test
    void unnamedMapKeys() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(UnnamedMapKeys.class)
                .withSources(config(
                        "unnamed.map.value", "unnamed",
                        "unnamed.map.one.value", "one",
                        "unnamed.map.two.value", "two",
                        "unnamed.map.\"3.three\".value", "three",
                        "unnamed.double-map.value", "unnamed",
                        "unnamed.double-map.one.two.value", "double",
                        "unnamed.triple-map.value", "unnamed",
                        "unnamed.triple-map.one.three.value", "unnamed-2-3",
                        "unnamed.triple-map.one.two.three.value", "triple",
                        "unnamed.map-list.value", "unnamed",
                        "unnamed.map-list.one[0].value", "one-0",
                        "unnamed.map-list.one[1].value", "one-1",
                        "unnamed.map-list.\"3.three\"[0].value", "3.three-0",
                        "unnamed.parent.value", "unnamed",
                        "unnamed.parent.one.value", "one"))
                .build();

        UnnamedMapKeys mapping = config.getConfigMapping(UnnamedMapKeys.class);
        assertEquals("unnamed", mapping.map().get(null).value());
        assertEquals("one", mapping.map().get("one").value());
        assertEquals("two", mapping.map().get("two").value());
        assertEquals("three", mapping.map().get("3.three").value());
        assertEquals("unnamed", mapping.doubleMap().get(null).get(null).value());
        assertEquals("double", mapping.doubleMap().get("one").get("two").value());
        assertEquals("unnamed", mapping.tripleMap().get("a").get("b").get("c").value());
        assertEquals("triple", mapping.tripleMap().get("one").get("two").get("three").value());
        assertEquals("unnamed", mapping.mapList().get(null).get(0).value());
        assertEquals("one-0", mapping.mapList().get("one").get(0).value());
        assertEquals("one-1", mapping.mapList().get("one").get(1).value());
        assertEquals("3.three-0", mapping.mapList().get("3.three").get(0).value());
        assertEquals("unnamed", mapping.parent().parent().get(null).value());
        assertEquals("one", mapping.parent().parent().get("one").value());
    }

    @ConfigMapping(prefix = "unnamed")
    interface UnnamedMapKeys {
        @WithUnnamedKey
        Map<String, Nested> map();

        Map<@WithUnnamedKey String, Map<@WithUnnamedKey String, Nested>> doubleMap();

        Map<@WithUnnamedKey("a") String, Map<@WithUnnamedKey("b") String, Map<@WithUnnamedKey("c") String, Nested>>> tripleMap();

        @WithUnnamedKey
        Map<String, List<Nested>> mapList();

        Parent parent();

        interface Nested {
            String value();
        }

        interface Parent {
            @WithParentName
            @WithUnnamedKey
            Map<String, Nested> parent();
        }
    }

    @Test
    void explicitUnnamedMapKeys() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(UnnamedExplicitMapKeys.class)
                .withSources(config(
                        "unnamed.map.value", "value",
                        "unnamed.map.unnamed.value", "explicit"));

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @ConfigMapping(prefix = "unnamed")
    interface UnnamedExplicitMapKeys {
        @WithUnnamedKey("unnamed")
        Map<String, Nested> map();

        interface Nested {
            String value();
        }
    }

    @Test
    void ambiguousMapping() {
        assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(AmbiguousMapping.class).build());
    }

    @ConfigMapping(prefix = "ambiguous")
    interface AmbiguousMapping {
        // match `ambiguous.value`
        String value();

        // match `ambiguous.value`
        // match `ambiguous.*.value`
        @WithParentName
        @WithUnnamedKey
        Map<String, Nested> nested();

        interface Nested {
            String value();
        }
    }

    @Test
    void withNameMultipleSegments() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(WithNameMultipleSegments.class)
                .withSources(config(
                        "my.property.rest.api.url", "http://localhost:8094",
                        "my.optional.rest.api.url", "http://localhost:8094",
                        "my.list[0].rest.api.url", "http://localhost:8094",
                        "my.map.key.rest.api.url", "http://localhost:8094",
                        "my.map-list.key[0].rest.api.url", "http://localhost:8094"))
                .build();

        WithNameMultipleSegments mapping = config.getConfigMapping(WithNameMultipleSegments.class);
        assertEquals("http://localhost:8094", mapping.property().apiUrl());
        assertTrue(mapping.optional().isPresent());
        assertEquals("http://localhost:8094", mapping.optional().get().apiUrl());
        assertEquals("http://localhost:8094", mapping.list().get(0).apiUrl());
        assertEquals("http://localhost:8094", mapping.map().get("key").apiUrl());
        assertEquals("http://localhost:8094", mapping.mapList().get("key").get(0).apiUrl());
    }

    @ConfigMapping(prefix = "my")
    public interface WithNameMultipleSegments {
        Property property();

        Optional<Property> optional();

        List<Property> list();

        Map<String, Property> map();

        Map<String, List<Property>> mapList();

        interface Property {
            @WithName("rest.api.url")
            String apiUrl();
        }
    }
}
