package io.smallrye.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class ConfigMappingLoaderParallelTest {
    @Test
    void testParallelThreadOne() {
        loadTestClass();
    }

    @Test
    void testParallelThreadTwo() {
        loadTestClass();
    }

    @Test
    void testParallelThreadThree() {
        loadTestClass();
    }

    @Test
    void testParallelThreadFour() {
        loadTestClass();
    }

    private void loadTestClass() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoaderTest.Server.class);
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoaderTest.Server.class);

        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080"))
                .withMapping(ConfigMappingLoaderTest.Server.class)
                .build();

        ConfigMappingLoaderTest.Server server = config.getConfigMapping(ConfigMappingLoaderTest.Server.class);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }
}
