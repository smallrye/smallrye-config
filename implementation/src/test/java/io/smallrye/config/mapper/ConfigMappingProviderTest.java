package io.smallrye.config.mapper;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigMappingProviderTest {
    @Test
    void configMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080")).build();
        final Server configProperties = config.getConfigProperties(Server.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals("localhost", configProperties.getHost());
        assertEquals(8080, configProperties.port());
        assertEquals(8080, configProperties.getPort());
    }

    @Test
    void configMappingBuilder() throws Exception {
        final ConfigMappingProvider configMappingProvider = ConfigMappingProvider.builder().addRoot("server", Server.class)
                .addIgnored("server", "name").build();
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "server.name", "name")).build();

        final Server server = configMappingProvider.mapConfiguration(config).getConfigRoot("server", Server.class);
        assertEquals("localhost", server.host());
        assertEquals("localhost", server.getHost());
        assertEquals(8080, server.port());
        assertEquals(8080, server.getPort());
    }

    @Test
    void unmapped() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();
        // TODO - Most likely we want to do a warning here, but just to try out the feature.
        assertThrows(Exception.class, () -> config.getConfigProperties(Server.class, "server"));
    }

    @Test
    void ignorePropertiesInUnregisteredRoots() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                config("server.host", "localhost", "server.port", "8080", "client.name", "konoha"))
                .build();
        final Server configProperties = config.getConfigProperties(Server.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void ignoreProperties() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(config("server.host", "localhost", "server.port", "8080")).build();
        final Server configProperties = config.getConfigProperties(Server.class, "server");
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

        final ConfigMappingProvider.Result result = configMappingProvider.mapConfiguration(config);
        final SplitRootServerHostAndPort server = result.getConfigRoot("server", SplitRootServerHostAndPort.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        final SplitRootServerName name = result.getConfigRoot("server", SplitRootServerName.class);
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
        final SplitRootServerHostAndPort server = config.getConfigProperties(SplitRootServerHostAndPort.class, "server");
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
        final ServerSub server = config.getConfigProperties(ServerSub.class, "server");
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
        final SomeTypes types = config.getConfigProperties(SomeTypes.class);

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
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(Optionals.class)
                .build();
        final Optionals optionals = config.getConfigProperties(Optionals.class);

        assertTrue(optionals.server().isPresent());
        assertEquals("localhost", optionals.server().get().host());
        assertEquals(8080, optionals.server().get().port());

        assertTrue(optionals.optional().isPresent());
        assertEquals("optional", optionals.optional().get());
        assertTrue(optionals.optionalInt().isPresent());
        assertEquals(9, optionals.optionalInt().getAsInt());
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
        final CollectionTypes types = config.getConfigProperties(CollectionTypes.class);

        assertEquals(Stream.of("foo", "bar").collect(toList()), types.listStrings());
        assertEquals(Stream.of(1, 2, 3).collect(toList()), types.listInts());
    }

    @Test
    void maps() {
        final Map<String, String> typesConfig = new HashMap<String, String>() {
            {
                put("server.host", "localhost");
                put("server.port", "8080");
                put("another-server", "localhost");
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(typesConfig))
                .withMapping(Maps.class)
                .build();
        final Maps maps = config.getConfigProperties(Maps.class);

        assertEquals("localhost", maps.server().get("server").host());
        assertEquals("localhost", maps.anotherServer().get("another-server"));
    }

    @Test
    void defaults() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(Defaults.class)
                .build();
        final Defaults defaults = config.getConfigProperties(Defaults.class);

        assertEquals("foo", defaults.foo());
        assertEquals("bar", defaults.bar());
        assertEquals("foo", config.getRawValue("foo"));
    }

    @Test
    @Disabled
    void defaultsOnTheFly() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        final Defaults defaults = config.getConfigProperties(Defaults.class);

        assertEquals("foo", defaults.foo());
        assertEquals("bar", defaults.bar());
        assertEquals("foo", config.getRawValue("foo"));
    }

    @Test
    void converters() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("foo", "notbar"))
                .withMapping(Converters.class)
                .withConverter(String.class, 100, new FooBarConverter())
                .build();
        final Converters converters = config.getConfigProperties(Converters.class);

        assertEquals("bar", converters.foo());
        assertEquals("bar", config.getValue("foo", String.class));
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

        final ComplexSample sample = config.getConfigProperties(ComplexSample.class);
        assertEquals("localhost", sample.server().subHostAndPort().host());
        assertEquals(8080, sample.server().subHostAndPort().port());
        assertTrue(sample.client().isPresent());
        assertEquals("clienthost", sample.client().get().subHostAndPort().host());
        assertEquals(80, sample.client().get().subHostAndPort().port());
    }

    interface Server {
        String host();

        int port();

        String getHost();

        int getPort();
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
    }

    public interface CollectionTypes {
        @WithName("strings")
        List<String> listStrings();

        @WithName("ints")
        List<Integer> listInts();
    }

    public interface Maps {
        @WithParentName
        Map<String, Server> server();

        Map<String, String> anotherServer();
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
}
