package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingInterface.Property;

public class ConfigMappingsTest {
    @Test
    void registerMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080",
                        "server.reasons.200", "OK", "server.reasons.201", "Created",
                        "server.versions.v1", "1.The version 1.2.3",
                        "server.versions.v2", "2.The version 2.0.0",
                        "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3"))
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        registerConfigMappings(config, singleton(configClass(Server.class, "server")));
        Server server = config.getConfigMapping(Server.class);

        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals(2, server.reasons().size());
        assertEquals("OK", server.reasons().get(200));
        assertEquals("Created", server.reasons().get(201));
        assertEquals(2, server.versions().size());
        assertEquals(new Version(1, "The version 1.2.3"), server.versions().get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), server.versions().get("v2"));
        assertEquals(3, server.numbers().size());
        assertEquals(1, server.numbers().get("one"));
        assertEquals(2, server.numbers().get("two"));
        assertEquals(3, server.numbers().get("three"));
    }

    @Test
    void registerProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080",
                        "server.reasons.200", "OK", "server.reasons.201", "Created",
                        "server.versions.v1", "1.The version 1.2.3",
                        "server.versions.v2", "2.The version 2.0.0",
                        "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3"))
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        registerConfigProperties(config, singleton(configClass(ServerClass.class, "server")));
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
        assertEquals(2, server.reasons.size());
        assertEquals("OK", server.reasons.get(200));
        assertEquals("Created", server.reasons.get(201));
        assertEquals(2, server.versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), server.versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), server.versions.get("v2"));
        assertEquals(3, server.numbers.size());
        assertEquals(1, server.numbers.get("one"));
        assertEquals(2, server.numbers.get("two"));
        assertEquals(3, server.numbers.get("three"));

        Map<String, Version> versions = config.getValues("server.versions", String.class, Version.class);
        assertEquals(2, versions.size());
        assertEquals(new Version(1, "The version 1.2.3"), versions.get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), versions.get("v2"));

        Optional<Map<String, Version>> versionsOptional = config.getOptionalValues("optional.versions", String.class,
                Version.class);
        assertFalse(versionsOptional.isPresent());
    }

    @Test
    void validate() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withConverter(Version.class, 100, new VersionConverter())
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .build();

        assertThrows(ConfigValidationException.class,
                () -> registerConfigMappings(config, singleton(configClass(Server.class, "server"))),
                "server.unmapped does not map to any root");

        registerConfigProperties(config, singleton(configClass(ServerClass.class, "server")));
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateWithBuilderOrConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .withMapping(ServerClass.class)
                .withConverter(Version.class, 100, new VersionConverter())
                .withValidateUnknown(true)
                .withDefaultValue(SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false")
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateDisableOnConfigProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .withMapping(ServerClass.class)
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateAnnotations() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registerConfigMappings(config, singleton(configClass(ServerMappingClass.class))));
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00043: The @ConfigMapping annotation can only be placed in interfaces"));

        exception = assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(ServerMappingClass.class).build());
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00043: The @ConfigMapping annotation can only be placed in interfaces"));

        exception = assertThrows(IllegalStateException.class,
                () -> registerConfigMappings(config, singleton(configClass(ServerPropertiesInterface.class))));
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));

        exception = assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(ServerPropertiesInterface.class).build());
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));
    }

    @ConfigMapping(prefix = "server")
    interface ToStringMapping {
        String host();

        int port();

        List<Alias> aliases();

        String toString();

        interface Alias {
            String name();

            String toString();
        }
    }

    @Test
    void generateToString() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ToStringMapping.class)
                .withSources(config(
                        "server.host", "localhost",
                        "server.port", "8080",
                        "server.aliases[0].name", "prod-1",
                        "server.aliases[1].name", "prod-2"))
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        ToStringMapping mapping = config.getConfigMapping(ToStringMapping.class);
        String toString = mapping.toString();
        assertTrue(toString.contains("ToStringMapping{"));
        assertTrue(toString.contains("host=localhost"));
        assertTrue(toString.contains("port=8080"));
        assertTrue(toString.contains("port=8080"));
        assertTrue(toString.contains("aliases=["));
        assertTrue(toString.contains("Alias{name=prod-1}"));
        assertTrue(toString.contains("Alias{name=prod-2}"));
    }

    @ConfigMapping(prefix = "app")
    interface SuperToStringMapping {
        List<Bar> foo();

        String toString();

        interface Bar extends Foo {
            String child();

            String toString();
        }

        interface Foo {
            String parent();

            String toString();
        }
    }

    @Test
    void superToString() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(SuperToStringMapping.class)
                .withSources(config(
                        "app.foo[0].parent", "parent",
                        "app.foo[0].child", "child"))
                .build();

        SuperToStringMapping mapping = config.getConfigMapping(SuperToStringMapping.class);

        assertEquals("parent", mapping.foo().get(0).parent());
        assertEquals("child", mapping.foo().get(0).child());

        String toString = mapping.toString();
        assertTrue(toString.contains("parent=parent"));
        assertTrue(toString.contains("child=child"));
    }

    @ConfigMapping(prefix = "server")
    interface Server {
        String host();

        int port();

        Map<Integer, String> reasons();

        Map<String, Version> versions();

        Map<String, Integer> numbers();
    }

    @ConfigProperties(prefix = "server")
    static class ServerClass {
        String host;
        int port;
        Map<Integer, String> reasons;
        Map<String, Version> versions;
        Map<String, Integer> numbers;
    }

    @ConfigMapping(prefix = "server")
    static class ServerMappingClass {
        String host;
    }

    @ConfigProperties(prefix = "server")
    interface ServerPropertiesInterface {
        String host();
    }

    static class Version {
        int id;
        String name;

        Version(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Version version = (Version) o;
            return id == version.id && Objects.equals(name, version.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    static class VersionConverter implements Converter<Version> {
        @Override
        public Version convert(String value) {
            return new Version(Integer.parseInt(value.substring(0, 1)), value.substring(2));
        }
    }

    @ConfigMapping(prefix = "mapped")
    interface MappedProperties {
        String value();

        Nested nested();

        List<Nested> collection();

        interface Nested {
            String value();
        }
    }

    @Test
    void properties() {
        ConfigMappings.ConfigClass configClass = configClass(MappedProperties.class);
        Map<String, Property> properties = ConfigMappings.getProperties(configClass);
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey("mapped.nested.value"));
        assertTrue(properties.containsKey("mapped.value"));
        assertTrue(properties.containsKey("mapped.collection[*].value"));
    }

    @Test
    void registerInstances() {
        ConfigSource source = config(
                "mapping.instance.value", "value",
                "mapping.instance.map.one", "value");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(source)
                .withMapping(MappingInstance.class)
                .build();

        MappingInstance mapping = config.getConfigMapping(MappingInstance.class);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.getMappingsBuilder().mappingInstance(configClass(MappingInstance.class, "mapping.instance"), mapping);
        SmallRyeConfig mappingConfig = builder
                .withSources(source)
                .build();

        MappingInstance mappingInstance = mappingConfig.getConfigMapping(MappingInstance.class);
        assertEquals("value", mappingInstance.value());
        assertEquals("default", mappingInstance.defaultValue());
        assertEquals("value", mappingInstance.map().get("one"));
        assertEquals("default", mappingConfig.getRawValue("mapping.instance.default-value"));
    }

    @ConfigMapping(prefix = "mapping.instance")
    interface MappingInstance {
        String value();

        @WithDefault("default")
        String defaultValue();

        Map<String, String> map();
    }
}
