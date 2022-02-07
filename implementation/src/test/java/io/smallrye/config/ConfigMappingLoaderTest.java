package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.getConfigurationInterface;
import static io.smallrye.config.ConfigMappingLoader.getImplementationClass;
import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;

class ConfigMappingLoaderTest {
    @Test
    void multipleLoads() {
        ConfigMappingLoader.getImplementationClass(Server.class);
        ConfigMappingLoader.getImplementationClass(Server.class);

        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080"))
                .withMapping(Server.class)
                .build();

        Server server = config.getConfigMapping(Server.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void loadManually() {
        List<ConfigMappingMetadata> configMappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerManual.class);
        configMappingsMetadata.forEach(
                mappingMetadata -> ConfigMappingLoader.loadClass(ServerManual.class, mappingMetadata));
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
        ConfigMappingLoader.getImplementationClass(ServerManual.class);
    }

    @Test
    void discoverNested() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMappingInterface(ServerNested.class);
        List<ConfigMappingInterface> nested = mapping.getNested();
        assertEquals(4, nested.size());
        List<Class<?>> types = nested.stream().map(ConfigMappingInterface::getInterfaceType).collect(toList());
        assertTrue(types.contains(ServerNested.Environment.class));
        assertTrue(types.contains(ServerNested.Log.class));
        assertTrue(types.contains(ServerNested.Ssl.class));
        assertTrue(types.contains(ServerNested.App.class));
    }

    @Test
    void noArgsConstructor() throws Exception {
        assertTrue(getImplementationClass(Server.class).getDeclaredConstructor().newInstance() instanceof Server);
        assertTrue(getImplementationClass(ServerNested.class).getDeclaredConstructor().newInstance() instanceof ServerNested);
        assertThrows(IllegalArgumentException.class, () -> getImplementationClass(ServerProperties.class));
    }

    @ConfigMapping(prefix = "server")
    public interface Server {
        String host();

        int port();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {
        String host;

        int port;
    }

    interface ServerManual {
        String host();

        int port();
    }

    public interface ServerNested {
        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {
            String host();

            int port();
        }

        interface Log {
            boolean enabled();
        }

        interface Ssl {
            String certificate();
        }

        interface App {
            String name();
        }
    }

    @ConfigMapping
    interface OptionalCollection {
        @WithDefault("property")
        boolean property();

        Optional<List<OptionalCollectionGroup>> optional();
    }

    interface OptionalCollectionGroup {
        Optional<String> property();
    }

    /**
     * Because declared methods may return a different order, there was an issue where a bytecode POP was missing if a
     * collection group wrapped in an Optional was added first. This test manually set the method order so the issue is
     * 100% reproducible and not dependent on the result of java.lang.Class#getDeclaredMethods().
     */
    @Test
    void optionalCollectionGroup() throws Exception {
        Method[] methods = new Method[] {
                OptionalCollection.class.getDeclaredMethod("optional"),
                OptionalCollection.class.getDeclaredMethod("property")
        };
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(OptionalCollection.class,
                new ConfigMappingInterface[] {}, properties);

        loadClass(OptionalCollection.class, getConfigurationInterface(OptionalCollectionGroup.class));
        loadClass(OptionalCollection.class, configMappingInterface);

        Class<? extends ConfigMappingObject> implementationClass = getImplementationClass(OptionalCollection.class);
        // If the bytecode has an issue this will throw a VerifyError
        assertNotNull(implementationClass.getDeclaredConstructor(ConfigMappingContext.class));
    }

    @ConfigMapping
    interface OptionalCollectionPrimitive {
        @WithDefault("property")
        boolean property();

        Optional<List<String>> optional();
    }

    @Test
    void optionalCollectionPrimitive() throws Exception {
        Method[] methods = new Method[] {
                OptionalCollectionPrimitive.class.getDeclaredMethod("optional"),
                OptionalCollectionPrimitive.class.getDeclaredMethod("property")
        };
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(OptionalCollectionPrimitive.class,
                new ConfigMappingInterface[] {}, properties);

        loadClass(OptionalCollection.class, configMappingInterface);

        Class<? extends ConfigMappingObject> implementationClass = getImplementationClass(OptionalCollectionPrimitive.class);
        // If the bytecode has an issue this will throw a VerifyError
        assertNotNull(implementationClass.getDeclaredConstructor(ConfigMappingContext.class));
    }

    @ConfigMapping
    interface MappingCollection {
        @WithDefault("property")
        boolean property();

        Optional<List<MappingCollectionGroup>> collection();
    }

    interface MappingCollectionGroup {
        Optional<String> property();
    }

    @Test
    void collectionGroup() throws Exception {
        Method[] methods = new Method[] {
                MappingCollection.class.getDeclaredMethod("collection"),
                MappingCollection.class.getDeclaredMethod("property")
        };
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(MappingCollection.class,
                new ConfigMappingInterface[] {}, properties);

        loadClass(OptionalCollection.class, getConfigurationInterface(MappingCollectionGroup.class));
        loadClass(OptionalCollection.class, configMappingInterface);

        Class<? extends ConfigMappingObject> implementationClass = getImplementationClass(MappingCollection.class);
        // If the bytecode has an issue this will throw a VerifyError
        assertNotNull(implementationClass.getDeclaredConstructor(ConfigMappingContext.class));
    }

    interface ServerParent {
        String parent();

        ServerParentNested parentNested();

        interface ServerParentNested {

        }
    }

    interface ServerChild extends ServerParent {
        ServerChildNested childNested();

        interface ServerChildNested {

        }
    }

    @Test
    void parentNested() {
        List<ConfigMappingMetadata> mappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> mappings = mappingsMetadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(toList());
        assertTrue(mappings.contains(ServerChild.class));
        assertTrue(mappings.contains(ServerChild.ServerChildNested.class));
        assertTrue(mappings.contains(ServerParent.class));
        assertTrue(mappings.contains(ServerParent.ServerParentNested.class));
    }
}
