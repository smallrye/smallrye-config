package io.smallrye.config;

import static io.smallrye.config.ConfigMappingDefaultsTest.S3BuildTimeConfig.AsyncHttpClientBuildTimeConfig.AsyncClientType.NETTY;
import static io.smallrye.config.ConfigMappingDefaultsTest.S3BuildTimeConfig.SyncHttpClientBuildTimeConfig.SyncClientType.URL;
import static io.smallrye.config.ConfigMappings.getDefaults;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingDefaultsTest.DataSourcesJdbcBuildTimeConfig.DataSourceJdbcOuterNamedBuildTimeConfig;

public class ConfigMappingDefaultsTest {
    @Test
    void defaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "defaults.list-nested[0].value", "value",
                        "defaults.parent.list-nested[0].value", "value"))
                .withMapping(Defaults.class)
                .build();

        Defaults mapping = config.getConfigMapping(Defaults.class);

        assertEquals("value", mapping.value());
        assertEquals(10, mapping.primitive());
        assertTrue(mapping.optional().isPresent());
        assertEquals("value", mapping.optional().get());
        assertTrue(mapping.optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.list());
        assertEquals("value", mapping.map().get("default"));

        assertEquals("value", mapping.nested().value());
        assertEquals(10, mapping.nested().primitive());
        assertTrue(mapping.nested().optional().isPresent());
        assertEquals("value", mapping.nested().optional().get());
        assertTrue(mapping.nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.nested().list());
        assertEquals("value", mapping.nested().map().get("default"));

        assertTrue(mapping.optionalNested().isPresent());
        assertEquals("value", mapping.optionalNested().get().value());
        assertEquals(10, mapping.optionalNested().get().primitive());
        assertTrue(mapping.optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.optionalNested().get().optional().get());
        assertTrue(mapping.optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.optionalNested().get().list());
        assertEquals("value", mapping.optionalNested().get().map().get("default"));

        assertEquals("value", mapping.listNested().get(0).value());
        assertEquals(10, mapping.listNested().get(0).primitive());
        assertTrue(mapping.listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.listNested().get(0).optional().get());
        assertTrue(mapping.listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.listNested().get(0).list());
        assertEquals("value", mapping.listNested().get(0).map().get("default"));

        assertEquals("value", mapping.mapNested().get("default").value());
        assertEquals(10, mapping.mapNested().get("default").primitive());
        assertTrue(mapping.mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.mapNested().get("default").optional().get());
        assertTrue(mapping.mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.mapNested().get("default").list());
        assertEquals("value", mapping.mapNested().get("default").map().get("default"));

        assertEquals("value", mapping.parent().child().nested().value());
        assertEquals(10, mapping.parent().child().nested().primitive());
        assertTrue(mapping.parent().child().nested().optional().isPresent());
        assertEquals("value", mapping.parent().child().nested().optional().get());
        assertTrue(mapping.parent().child().nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().nested().list());
        assertEquals("value", mapping.parent().child().nested().map().get("default"));

        assertTrue(mapping.parent().child().optionalNested().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().value());
        assertEquals(10, mapping.parent().child().optionalNested().get().primitive());
        assertTrue(mapping.parent().child().optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().optional().get());
        assertTrue(mapping.parent().child().optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().optionalNested().get().list());
        assertEquals("value", mapping.parent().child().optionalNested().get().map().get("default"));

        assertEquals("value", mapping.parent().child().listNested().get(0).value());
        assertEquals(10, mapping.parent().child().listNested().get(0).primitive());
        assertTrue(mapping.parent().child().listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.parent().child().listNested().get(0).optional().get());
        assertTrue(mapping.parent().child().listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().listNested().get(0).list());
        assertEquals("value", mapping.parent().child().listNested().get(0).map().get("default"));

        assertEquals("value", mapping.parent().child().mapNested().get("default").value());
        assertEquals(10, mapping.parent().child().mapNested().get("default").primitive());
        assertTrue(mapping.parent().child().mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.parent().child().mapNested().get("default").optional().get());
        assertTrue(mapping.parent().child().mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().mapNested().get("default").list());
        assertEquals("value", mapping.parent().child().mapNested().get("default").map().get("default"));
    }

    @Test
    void emptyPrefix() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "list-nested[0].value", "value",
                        "parent.list-nested[0].value", "value"))
                .withMapping(Defaults.class, "")
                .build();

        Defaults mapping = config.getConfigMapping(Defaults.class, "");

        assertEquals("value", mapping.value());
        assertEquals(10, mapping.primitive());
        assertTrue(mapping.optional().isPresent());
        assertEquals("value", mapping.optional().get());
        assertTrue(mapping.optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.list());
        assertEquals("value", mapping.map().get("default"));

        assertEquals("value", mapping.nested().value());
        assertEquals(10, mapping.nested().primitive());
        assertTrue(mapping.nested().optional().isPresent());
        assertEquals("value", mapping.nested().optional().get());
        assertTrue(mapping.nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.nested().list());
        assertEquals("value", mapping.nested().map().get("default"));

        assertTrue(mapping.optionalNested().isPresent());
        assertEquals("value", mapping.optionalNested().get().value());
        assertEquals(10, mapping.optionalNested().get().primitive());
        assertTrue(mapping.optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.optionalNested().get().optional().get());
        assertTrue(mapping.optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.optionalNested().get().list());
        assertEquals("value", mapping.optionalNested().get().map().get("default"));

        assertEquals("value", mapping.listNested().get(0).value());
        assertEquals(10, mapping.listNested().get(0).primitive());
        assertTrue(mapping.listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.listNested().get(0).optional().get());
        assertTrue(mapping.listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.listNested().get(0).list());
        assertEquals("value", mapping.listNested().get(0).map().get("default"));

        assertEquals("value", mapping.mapNested().get("default").value());
        assertEquals(10, mapping.mapNested().get("default").primitive());
        assertTrue(mapping.mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.mapNested().get("default").optional().get());
        assertTrue(mapping.mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.mapNested().get("default").list());
        assertEquals("value", mapping.mapNested().get("default").map().get("default"));

        assertEquals("value", mapping.parent().child().nested().value());
        assertEquals(10, mapping.parent().child().nested().primitive());
        assertTrue(mapping.parent().child().nested().optional().isPresent());
        assertEquals("value", mapping.parent().child().nested().optional().get());
        assertTrue(mapping.parent().child().nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().nested().list());
        assertEquals("value", mapping.parent().child().nested().map().get("default"));

        assertTrue(mapping.parent().child().optionalNested().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().value());
        assertEquals(10, mapping.parent().child().optionalNested().get().primitive());
        assertTrue(mapping.parent().child().optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().optional().get());
        assertTrue(mapping.parent().child().optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().optionalNested().get().list());
        assertEquals("value", mapping.parent().child().optionalNested().get().map().get("default"));

        assertEquals("value", mapping.parent().child().listNested().get(0).value());
        assertEquals(10, mapping.parent().child().listNested().get(0).primitive());
        assertTrue(mapping.parent().child().listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.parent().child().listNested().get(0).optional().get());
        assertTrue(mapping.parent().child().listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().listNested().get(0).list());
        assertEquals("value", mapping.parent().child().listNested().get(0).map().get("default"));

        assertEquals("value", mapping.parent().child().mapNested().get("default").value());
        assertEquals(10, mapping.parent().child().mapNested().get("default").primitive());
        assertTrue(mapping.parent().child().mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.parent().child().mapNested().get("default").optional().get());
        assertTrue(mapping.parent().child().mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().mapNested().get("default").list());
        assertEquals("value", mapping.parent().child().mapNested().get("default").map().get("default"));
    }

    @ConfigMapping(prefix = "defaults")
    interface Defaults {
        @WithDefault("value")
        String value();

        @WithDefault("10")
        int primitive();

        @WithDefault("value")
        Optional<String> optional();

        @WithDefault("10")
        OptionalInt optionalPrimitive();

        @WithDefault("one,two")
        List<String> list();

        @WithDefault("value")
        Map<String, String> map();

        Nested nested();

        Optional<Nested> optionalNested();

        List<Nested> listNested();

        @WithDefaults
        Map<String, Nested> mapNested();

        Parent parent();

        interface Nested {
            @WithDefault("value")
            String value();

            @WithDefault("10")
            int primitive();

            @WithDefault("value")
            Optional<String> optional();

            @WithDefault("10")
            OptionalInt optionalPrimitive();

            @WithDefault("one,two")
            List<String> list();

            @WithDefault("value")
            Map<String, String> map();
        }

        interface Parent {
            @WithParentName
            Child child();
        }

        interface Child {
            Nested nested();

            Optional<Nested> optionalNested();

            List<Nested> listNested();

            @WithDefaults
            Map<String, Nested> mapNested();
        }
    }

    @Test
    void defaultsNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config(
                        "defaults.listNested[0].value", "value",
                        "defaults.parent.listNested[0].value", "value"))
                .withMapping(DefaultsWithNamingStrategy.class)
                .build();

        DefaultsWithNamingStrategy mapping = config.getConfigMapping(DefaultsWithNamingStrategy.class);

        assertEquals("value", mapping.value());
        assertEquals(10, mapping.primitive());
        assertTrue(mapping.optional().isPresent());
        assertEquals("value", mapping.optional().get());
        assertTrue(mapping.optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.list());
        assertEquals("value", mapping.map().get("default"));

        assertEquals("value", mapping.nested().value());
        assertEquals(10, mapping.nested().primitive());
        assertTrue(mapping.nested().optional().isPresent());
        assertEquals("value", mapping.nested().optional().get());
        assertTrue(mapping.nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.nested().list());
        assertEquals("value", mapping.nested().map().get("default"));

        assertTrue(mapping.optionalNested().isPresent());
        assertEquals("value", mapping.optionalNested().get().value());
        assertEquals(10, mapping.optionalNested().get().primitive());
        assertTrue(mapping.optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.optionalNested().get().optional().get());
        assertTrue(mapping.optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.optionalNested().get().list());
        assertEquals("value", mapping.optionalNested().get().map().get("default"));

        assertEquals("value", mapping.listNested().get(0).value());
        assertEquals(10, mapping.listNested().get(0).primitive());
        assertTrue(mapping.listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.listNested().get(0).optional().get());
        assertTrue(mapping.listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.listNested().get(0).list());
        assertEquals("value", mapping.listNested().get(0).map().get("default"));

        assertEquals("value", mapping.mapNested().get("default").value());
        assertEquals(10, mapping.mapNested().get("default").primitive());
        assertTrue(mapping.mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.mapNested().get("default").optional().get());
        assertTrue(mapping.mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.mapNested().get("default").list());
        assertEquals("value", mapping.mapNested().get("default").map().get("default"));

        assertEquals("value", mapping.parent().child().nested().value());
        assertEquals(10, mapping.parent().child().nested().primitive());
        assertTrue(mapping.parent().child().nested().optional().isPresent());
        assertEquals("value", mapping.parent().child().nested().optional().get());
        assertTrue(mapping.parent().child().nested().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().nested().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().nested().list());
        assertEquals("value", mapping.parent().child().nested().map().get("default"));

        assertTrue(mapping.parent().child().optionalNested().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().value());
        assertEquals(10, mapping.parent().child().optionalNested().get().primitive());
        assertTrue(mapping.parent().child().optionalNested().get().optional().isPresent());
        assertEquals("value", mapping.parent().child().optionalNested().get().optional().get());
        assertTrue(mapping.parent().child().optionalNested().get().optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().optionalNested().get().optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().optionalNested().get().list());
        assertEquals("value", mapping.parent().child().optionalNested().get().map().get("default"));

        assertEquals("value", mapping.parent().child().listNested().get(0).value());
        assertEquals(10, mapping.parent().child().listNested().get(0).primitive());
        assertTrue(mapping.parent().child().listNested().get(0).optional().isPresent());
        assertEquals("value", mapping.parent().child().listNested().get(0).optional().get());
        assertTrue(mapping.parent().child().listNested().get(0).optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().listNested().get(0).optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().listNested().get(0).list());
        assertEquals("value", mapping.parent().child().listNested().get(0).map().get("default"));

        assertEquals("value", mapping.parent().child().mapNested().get("default").value());
        assertEquals(10, mapping.parent().child().mapNested().get("default").primitive());
        assertTrue(mapping.parent().child().mapNested().get("default").optional().isPresent());
        assertEquals("value", mapping.parent().child().mapNested().get("default").optional().get());
        assertTrue(mapping.parent().child().mapNested().get("default").optionalPrimitive().isPresent());
        assertEquals(10, mapping.parent().child().mapNested().get("default").optionalPrimitive().getAsInt());
        assertIterableEquals(List.of("one", "two"), mapping.parent().child().mapNested().get("default").list());
        assertEquals("value", mapping.parent().child().mapNested().get("default").map().get("default"));
    }

    @ConfigMapping(prefix = "defaults", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    interface DefaultsWithNamingStrategy {
        @WithDefault("value")
        String value();

        @WithDefault("10")
        int primitive();

        @WithDefault("value")
        Optional<String> optional();

        @WithDefault("10")
        OptionalInt optionalPrimitive();

        @WithDefault("one,two")
        List<String> list();

        @WithDefault("value")
        Map<String, String> map();

        Nested nested();

        Optional<Nested> optionalNested();

        List<Nested> listNested();

        @WithDefaults
        Map<String, Nested> mapNested();

        Parent parent();

        interface Nested {
            @WithDefault("value")
            String value();

            @WithDefault("10")
            int primitive();

            @WithDefault("value")
            Optional<String> optional();

            @WithDefault("10")
            OptionalInt optionalPrimitive();

            @WithDefault("one,two")
            List<String> list();

            @WithDefault("value")
            Map<String, String> map();
        }

        interface Parent {
            @WithParentName
            Child child();
        }

        interface Child {
            Nested nested();

            Optional<Nested> optionalNested();

            List<Nested> listNested();

            @WithDefaults
            Map<String, Nested> mapNested();
        }
    }

    @Test
    void defaultsParentName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DefaultsParentName.class)
                .build();

        // TODO - Complete
    }

    @ConfigMapping(prefix = "defaults")
    interface DefaultsParentName {
        @WithParentName
        @WithDefault("value")
        String value();
    }

    @Test
    void moreDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(DataSourcesJdbcBuildTimeConfig.class)
                .withMapping(DataSourcesJdbcRuntimeConfig.class)
                .build();

        DataSourcesJdbcBuildTimeConfig mapping = config.getConfigMapping(DataSourcesJdbcBuildTimeConfig.class);
        DataSourceJdbcOuterNamedBuildTimeConfig mapDefault = mapping.dataSources().get("<default>");
        assertNotNull(mapDefault);
        assertTrue(mapDefault.jdbc().enabled());
        assertFalse(mapDefault.jdbc().tracing());
        assertFalse(mapDefault.jdbc().telemetry());
    }

    @ConfigMapping(prefix = "quarkus.datasource")
    public interface DataSourcesJdbcBuildTimeConfig {

        @WithParentName
        @WithDefaults
        @WithUnnamedKey("<default>")
        Map<String, DataSourceJdbcOuterNamedBuildTimeConfig> dataSources();

        interface DataSourceJdbcOuterNamedBuildTimeConfig {
            DataSourceJdbcBuildTimeConfig jdbc();
        }

        interface DataSourceJdbcBuildTimeConfig {
            @WithParentName
            @WithDefault("true")
            boolean enabled();

            Optional<String> driver();

            Optional<Boolean> enableMetrics();

            @WithDefault("false")
            boolean tracing();

            @WithDefault("false")
            boolean telemetry();
        }
    }

    @ConfigMapping(prefix = "quarkus.datasource")
    public interface DataSourcesJdbcRuntimeConfig {

        DataSourceJdbcRuntimeConfig jdbc();

        @WithParentName
        @WithDefaults
        Map<String, DataSourceJdbcOuterNamedRuntimeConfig> namedDataSources();

        interface DataSourceJdbcOuterNamedRuntimeConfig {
            DataSourceJdbcRuntimeConfig jdbc();
        }

        interface DataSourceJdbcRuntimeConfig {
            @WithDefault("0")
            int minSize();

            @WithDefault("20")
            int maxSize();

            @WithDefault("2M")
            String backgroundValidationInterval();

            @WithDefault("5S")
            Optional<String> acquisitionTimeout();

            @WithDefault("5M")
            String idleRemovalInterval();

            @WithDefault("false")
            boolean extendedLeakReport();

            @WithDefault("false")
            boolean flushOnClose();

            @WithDefault("true")
            boolean detectStatementLeaks();

            @WithDefault("true")
            boolean poolingEnabled();

            DataSourceJdbcTracingRuntimeConfig tracing();

            interface DataSourceJdbcTracingRuntimeConfig {
                @WithDefault("false")
                boolean traceWithActiveSpanOnly();
            }
        }
    }

    @Test
    void parentDefaults() {
        Map<String, String> defaults = getDefaults(configClassWithPrefix(ExtendsBase.class));
        assertEquals(2, defaults.size());
        assertEquals("default", defaults.get("base.base"));
        assertEquals("default", defaults.get("base.my-prop"));
    }

    public interface Base {
        @WithDefault("default")
        String base();
    }

    @ConfigMapping(prefix = "base")
    public interface ExtendsBase extends Base {
        @WithDefault("default")
        String myProp();
    }

    @Test
    void mapDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(MapDefaults.class)
                .build();

        MapDefaults mapping = config.getConfigMapping(MapDefaults.class);
        assertIterableEquals(List.of("foo", "bar"), mapping.map().get("any"));

        config = new SmallRyeConfigBuilder()
                .withSources(config("map.defaults.any", "one,two"))
                .withMapping(MapDefaults.class)
                .build();

        mapping = config.getConfigMapping(MapDefaults.class);
        assertIterableEquals(List.of("one", "two"), mapping.map().get("any"));

        config = new SmallRyeConfigBuilder()
                .withSources(config("map.defaults.any[0]", "one", "map.defaults.any[1]", "two"))
                .withMapping(MapDefaults.class)
                .build();

        mapping = config.getConfigMapping(MapDefaults.class);
        assertIterableEquals(List.of("one", "two"), mapping.map().get("any"));
    }

    @ConfigMapping(prefix = "map.defaults")
    interface MapDefaults {
        @WithParentName
        @WithDefault("foo,bar")
        Map<String, List<String>> map();
    }

    @Test
    void groupParentDefaults() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(S3BuildTimeConfig.class)
                .build();

        S3BuildTimeConfig mapping = config.getConfigMapping(S3BuildTimeConfig.class);
        assertEquals(URL, mapping.syncClient().type());
        assertEquals(NETTY, mapping.asyncClient().type());
        assertIterableEquals(Set.of("default"), mapping.devservices().buckets());
        assertFalse(mapping.devservices().shared());
        assertEquals("localstack", mapping.devservices().serviceName());
    }

    @ConfigMapping(prefix = "quarkus.s3")
    public interface S3BuildTimeConfig extends HasSdkBuildTimeConfig {
        SyncHttpClientBuildTimeConfig syncClient();

        AsyncHttpClientBuildTimeConfig asyncClient();

        S3DevServicesBuildTimeConfig devservices();

        interface SyncHttpClientBuildTimeConfig {
            @WithDefault(value = "url")
            SyncClientType type();

            enum SyncClientType {
                URL,
                APACHE
            }
        }

        interface AsyncHttpClientBuildTimeConfig {
            @WithDefault(value = "netty")
            AsyncClientType type();

            enum AsyncClientType {
                NETTY,
                AWS_CRT
            }
        }

        interface S3DevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {
            @WithDefault(value = "default")
            Set<String> buckets();
        }
    }

    public interface HasSdkBuildTimeConfig {
        @WithParentName
        SdkBuildTimeConfig sdk();
    }

    public interface SdkBuildTimeConfig {
        Optional<List<String>> interceptors();
    }

    public interface DevServicesBuildTimeConfig {
        Optional<Boolean> enabled();

        @WithDefault(value = "false")
        boolean shared();

        @WithDefault(value = "localstack")
        String serviceName();

        Map<String, String> containerProperties();
    }
}
