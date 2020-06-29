package io.smallrye.config.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.KeyValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class ConfigMappingTest {
    @Test
    void configMapping() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals("localhost", configProperties.getHost());
        assertEquals(8080, configProperties.port());
        assertEquals(8080, configProperties.getPort());
    }

    @Test
    void unmapped() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();
        // TODO - Most likely we want to do a warning here, but just to try out the feature.
        assertThrows(Exception.class, () -> config.getConfigProperties(Configs.class, "server"));
    }

    @Test
    void ignorePropertiesInUnregisteredRoots() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "client.name", "konoha"))
                .build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void ignoreProperties() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).build();
        final Configs configProperties = config.getConfigProperties(Configs.class, "server");
        assertEquals("localhost", configProperties.host());
        assertEquals(8080, configProperties.port());
    }

    @Test
    void splitRoots() throws Exception {
        final SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "server.name", "konoha"))
                .build();

        final ConfigMapping configMapping = ConfigMapping.builder()
                .addRoot("server", SplitRootServerHostAndPort.class)
                .addRoot("server", SplitRootServerName.class)
                .build();

        final ConfigMapping.Result result = configMapping.mapConfiguration(config);
        final SplitRootServerHostAndPort server = result.getConfigRoot("server", SplitRootServerHostAndPort.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());

        final SplitRootServerName name = result.getConfigRoot("server", SplitRootServerName.class);
        assertEquals("konoha", name.name());
    }

    @Test
    void splitRootsInConfig() {
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "server.name",
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
                .withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080", "server.name",
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
            }
        };

        final SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", typesConfig) {
                })
                .withMapping(SomeTypes.class)
                .build();
        final SomeTypes types = config.getConfigProperties(SomeTypes.class);

        assertEquals(9, types.intPrimitive());
        assertEquals(9, types.intWrapper());
        assertEquals(9999999999L, types.longPrimitive());
        assertEquals(9999999999L, types.longWrapper());
    }

    interface Configs {
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
    }
}
