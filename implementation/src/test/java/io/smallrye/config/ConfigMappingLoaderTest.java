package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
import static io.smallrye.config.ConfigMappingLoader.getGeneratedConfigClasses;
import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ConfigMappingLoaderTest {
    @Test
    void multipleLoads() {
        configClass(Server.class).implementation();
        configClass(Server.class).implementation();

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
        Set<GeneratedConfigClass> generatedClasses = getGeneratedConfigClasses(ServerManual.class);
        generatedClasses.forEach(ConfigMappingLoader::loadClass);
        assertNotNull(configClass(ServerManual.class).implementation());
    }

    @Test
    void discoverNested() {
        Set<GeneratedConfigClass> generatedClasses = getGeneratedConfigClasses(ServerNested.class);
        Set<Class<?>> types = generatedClasses.stream().map(GeneratedConfigClass::getInterfaceType).collect(toSet());
        assertTrue(types.contains(ServerNested.Environment.class));
        assertTrue(types.contains(ServerNested.Log.class));
        assertTrue(types.contains(ServerNested.Ssl.class));
        assertTrue(types.contains(ServerNested.App.class));
    }

    @Test
    void noArgsConstructor() throws Exception {
        assertInstanceOf(Server.class,
                configClass(Server.class).implementation().getImplementation().getDeclaredConstructor().newInstance());
        assertInstanceOf(ServerNested.class,
                configClass(ServerNested.class).implementation().getImplementation().getDeclaredConstructor().newInstance());
    }

    @ConfigMapping(prefix = "server")
    public interface Server {
        String host();

        int port();
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
        ConfigMappingHandler handler = ConfigMappingHandler.Handlers.find(OptionalCollection.class);
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(handler,
                OptionalCollection.class, methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(OptionalCollection.class, handler,
                new ConfigMappingInterface[] {}, properties);

        Set<GeneratedConfigClass> generatedClasses = new HashSet<>();
        generatedClasses.add(configMappingInterface);
        generatedClasses.add(ConfigMappingInterface.get(OptionalCollectionGroup.class, handler));
        for (GeneratedConfigClass generatedClass : generatedClasses) {
            assertNotNull(generatedClass);
            loadClass(generatedClass);
        }

        Class<?> implementationClass = configClass(OptionalCollection.class, handler).implementation().getImplementation();
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
        ConfigMappingHandler handler = ConfigMappingHandler.Handlers.find(OptionalCollectionPrimitive.class);
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(handler,
                OptionalCollectionPrimitive.class, methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(OptionalCollectionPrimitive.class,
                handler, new ConfigMappingInterface[] {}, properties);

        loadClass(configMappingInterface);

        Class<?> implementationClass = configClass(OptionalCollectionPrimitive.class, handler).implementation()
                .getImplementation();
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
        ConfigMappingHandler handler = ConfigMappingHandler.Handlers.find(MappingCollection.class);
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(handler,
                MappingCollection.class, methods, 0, 0);
        ConfigMappingInterface configMappingInterface = new ConfigMappingInterface(MappingCollection.class, handler,
                new ConfigMappingInterface[] {}, properties);

        Set<GeneratedConfigClass> generatedClasses = new HashSet<>();
        generatedClasses.add(configMappingInterface);
        generatedClasses.add(ConfigMappingInterface.get(MappingCollectionGroup.class, handler));
        for (GeneratedConfigClass generatedClass : generatedClasses) {
            assertNotNull(generatedClass);
            loadClass(generatedClass);
        }

        Class<?> implementationClass = configClass(MappingCollection.class, handler).implementation().getImplementation();
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
        Set<GeneratedConfigClass> generatedClasses = getGeneratedConfigClasses(ServerChild.class);
        Set<String> classNames = generatedClasses.stream().map(GeneratedConfigClass::getClassName).collect(toSet());
        assertEquals(4, classNames.size());
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$ServerChild$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$ServerChild$ServerChildNested$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$ServerParent$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$ServerParent$ServerParentNested$$CMImpl"));
    }

    interface MyConfig {
        GlobalConfig global();

        interface CommonConfig {
            Optional<String> commonTest();

            UserConfig user();
        }

        interface GlobalConfig extends CommonConfig {
            Optional<String> globalTest();
        }

        interface UserConfig {
            Optional<String> name();
        }
    }

    @Test
    void nestedParents() {
        Set<String> classNames = getGeneratedConfigClasses(MyConfig.class)
                .stream().map(GeneratedConfigClass::getClassName).collect(toSet());
        assertEquals(4, classNames.size());
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$MyConfig$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$MyConfig$CommonConfig$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$MyConfig$GlobalConfig$$CMImpl"));
        assertTrue(classNames.contains("io.smallrye.config.ConfigMappingLoaderTest$MyConfig$UserConfig$$CMImpl"));
    }
}
