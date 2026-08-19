package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConfigMappingLoaderParallelTest {
    @Test
    void testInterfaceParallelOne() {
        loadInterface();
    }

    @Test
    void testInterfaceParallelTwo() {
        loadInterface();
    }

    @Test
    void testInterfaceParallelThree() {
        loadInterface();
    }

    @Test
    void testInterfaceParallelFour() {
        loadInterface();
    }

    private void loadInterface() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080"))
                .withMapping(ConfigMappingLoaderTest.Server.class)
                .build();

        ConfigMappingLoaderTest.Server server = config.getConfigMapping(ConfigMappingLoaderTest.Server.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void testClassParallelOne() {
        loadClass();
    }

    @Test
    void testClassParallelTwo() {
        loadClass();
    }

    @Test
    void testClassParallelThree() {
        loadClass();
    }

    @Test
    void testClassParallelFour() {
        loadClass();
    }

    private void loadClass() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("host", "localhost", "port", "8080"))
                .withMapping(ServerClass.class)
                .build();

        ServerClass server = config.getConfigMapping(ServerClass.class);
        assertEquals("localhost", server.host);
        assertEquals(8080, server.port);
    }

    static class ServerClass {
        String host;
        int port;
    }
}
