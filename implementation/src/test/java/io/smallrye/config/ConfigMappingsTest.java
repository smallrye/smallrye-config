package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.registerConfigProperties;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

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

        registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server")));
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

        registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
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
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .build();

        assertThrows(ConfigValidationException.class,
                () -> registerConfigMappings(config, singleton(configClassWithPrefix(Server.class, "server"))),
                "server.unmapped does not map to any root");

        registerConfigProperties(config, singleton(configClassWithPrefix(ServerClass.class, "server")));
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateWithBuilderOrConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"))
                .withMapping(ServerClass.class, "server")
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
                .withMapping(ServerClass.class, "server")
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateAnnotations() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registerConfigMappings(config, singleton(configClassWithPrefix(ServerMappingClass.class, "server"))));
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00043: The @ConfigMapping annotation can only be placed in interfaces"));

        exception = assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(ServerMappingClass.class, "server").build());
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00043: The @ConfigMapping annotation can only be placed in interfaces"));

        exception = assertThrows(IllegalStateException.class,
                () -> registerConfigMappings(config,
                        singleton(configClassWithPrefix(ServerPropertiesInterface.class, "server"))));
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));

        exception = assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(ServerPropertiesInterface.class, "server").build());
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));
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
}
