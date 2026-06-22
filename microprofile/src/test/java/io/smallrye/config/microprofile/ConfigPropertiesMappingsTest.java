package io.smallrye.config.microprofile;

import static io.smallrye.config.ConfigMappings.registerConfigClasses;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;

class ConfigPropertiesMappingsTest {
    @Test
    void registerProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(Map.of(
                        "server.host", "localhost", "server.port", "8080",
                        "server.reasons.200", "OK", "server.reasons.201", "Created",
                        "server.versions.v1", "1.The version 1.2.3",
                        "server.versions.v2", "2.The version 2.0.0",
                        "server.numbers.one", "1", "server.numbers.two", "2", "server.numbers.three", "3"), "test"))
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        registerConfigClasses(config, singleton(configClass(ServerClass.class, "server")), false);
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
    void validateProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withConverter(Version.class, 100, new VersionConverter())
                .withSources(new PropertiesConfigSource(Map.of(
                        "server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"), "test"))
                .build();

        registerConfigClasses(config, singleton(configClass(ServerClass.class, "server")), false);
        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateWithBuilderOrConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(Map.of(
                        "server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"), "test"))
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
                .withSources(new PropertiesConfigSource(Map.of(
                        "server.host", "localhost", "server.port", "8080", "server.unmapped", "unmapped"), "test"))
                .withMapping(ServerClass.class)
                .withConverter(Version.class, 100, new VersionConverter())
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);

        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void validateConfigPropertiesAnnotationOnInterface() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> registerConfigClasses(config, singleton(configClass(ServerPropertiesInterface.class)), true));
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));

        exception = assertThrows(IllegalStateException.class,
                () -> new SmallRyeConfigBuilder().withMapping(ServerPropertiesInterface.class).build());
        assertTrue(exception.getMessage()
                .startsWith("SRCFG00044: The @ConfigProperties annotation can only be placed in classes"));
    }

    @Test
    void interfaceAndClass() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withMapping(ConfigMappingNamingKebab.class)
                .withMapping(ConfigPropertiesNamingVerbatim.class)
                .withSources(new PropertiesConfigSource(Map.of("server.theHost", "localhost"), "test"))
                .withSources(new PropertiesConfigSource(Map.of(
                        "server.the-host", "localhost", "server.form.login-page", "login"), "test"))
                .build();

        ConfigPropertiesNamingVerbatim configProperties = config.getConfigMapping(ConfigPropertiesNamingVerbatim.class);
        assertEquals("localhost", configProperties.theHost);

        ConfigMappingNamingKebab configMapping = config.getConfigMapping(ConfigMappingNamingKebab.class);
        assertEquals("localhost", configMapping.theHost());
        assertEquals("login", configMapping.form().get("form").loginPage());
    }

    @Test
    void configPropertyAnnotation() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(ServerMPConfig20.class)
                .withDefaultValue("name", "localhost")
                .build();
        ServerMPConfig20 server = config.getConfigMapping(ServerMPConfig20.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    @Test
    void noArgsConstructorConfigProperties() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingLoader.ensureLoaded(ServerProperties.class));
    }

    @ConfigProperties(prefix = "server")
    public static class ServerClass {
        public String host;
        public int port;
        public Map<Integer, String> reasons;
        public Map<String, Version> versions;
        public Map<String, Integer> numbers;
    }

    @ConfigProperties(prefix = "server")
    public interface ServerPropertiesInterface {
        String host();
    }

    @ConfigProperties(prefix = "server")
    public static class ConfigPropertiesNamingVerbatim {
        public String theHost;
    }

    @ConfigMapping(prefix = "server")
    interface ConfigMappingNamingKebab {
        String theHost();

        @WithParentName
        Map<String, Form> form();

        interface Form {
            String loginPage();
        }
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {
        public String host;
        public int port;
    }

    @ConfigProperties
    public static class ServerMPConfig20 {
        @ConfigProperty(name = "name")
        public String host;
        @ConfigProperty(defaultValue = "8080")
        public int port;
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
