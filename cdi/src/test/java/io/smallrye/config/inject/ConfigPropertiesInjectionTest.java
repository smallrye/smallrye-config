package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@ExtendWith(WeldJunit5Extension.class)
class ConfigPropertiesInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator
            .from(ConfigExtension.class, ConfigPropertiesInjectionTest.class, Server.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperties
    Server server;
    @Inject
    @ConfigProperties(prefix = "cloud")
    Server serverCloud;

    @Inject
    Config config;

    @Test
    void configProperties() {
        assertNotNull(server);
        assertEquals("localhost", server.theHost);
        assertEquals(8080, server.port);
        assertEquals(1, server.array.length);
        assertEquals(1, server.list.size());
        assertEquals(1, server.set.size());
        assertFalse(server.optionalArray.isPresent());
        assertFalse(server.optionalList.isPresent());
        assertFalse(server.optionalSet.isPresent());

        assertNotNull(serverCloud);
        assertEquals("cloud", serverCloud.theHost);
        assertEquals(9090, serverCloud.port);
        assertEquals(2, serverCloud.array.length);
        assertEquals(2, serverCloud.list.size());
        assertEquals(2, serverCloud.set.size());
        assertTrue(serverCloud.optionalArray.isPresent());
        assertEquals(2, serverCloud.optionalArray.get().length);
        assertTrue(serverCloud.optionalList.isPresent());
        assertEquals(2, serverCloud.optionalList.get().size());
        assertTrue(serverCloud.optionalSet.isPresent());
        assertEquals(2, serverCloud.optionalSet.get().size());
    }

    @Test
    void select() {
        Server server = CDI.current().select(Server.class, ConfigProperties.Literal.of(UNCONFIGURED_PREFIX)).get();
        assertNotNull(server);
        assertEquals("localhost", server.theHost);
        assertEquals(8080, server.port);
        assertEquals(1, server.array.length);
        assertEquals(1, server.list.size());
        assertEquals(1, server.set.size());
        assertFalse(server.optionalArray.isPresent());
        assertFalse(server.optionalList.isPresent());
        assertFalse(server.optionalSet.isPresent());

        Server cloud = CDI.current().select(Server.class, ConfigProperties.Literal.of("cloud")).get();
        assertNotNull(cloud);
        assertEquals("cloud", cloud.theHost);
        assertEquals(9090, cloud.port);
        assertEquals(2, cloud.array.length);
        assertEquals(2, cloud.list.size());
        assertEquals(2, cloud.set.size());
        assertTrue(cloud.optionalArray.isPresent());
        assertEquals(2, cloud.optionalArray.get().length);
        assertTrue(cloud.optionalList.isPresent());
        assertEquals(2, cloud.optionalList.get().size());
        assertTrue(cloud.optionalSet.isPresent());
        assertEquals(2, cloud.optionalSet.get().size());
    }

    @Test
    void empty() {
        assertNull(config.getConfigValue("host").getValue());
        assertNull(config.getConfigValue("theHost").getValue());
        assertNull(config.getConfigValue("port").getValue());

        assertThrows(UnsatisfiedResolutionException.class,
                () -> CDI.current().select(Server.class, ConfigProperties.Literal.of("")).get());
    }

    @Dependent
    @ConfigProperties(prefix = "server")
    public static class Server {
        public String theHost;
        public int port;
        @ConfigProperty(defaultValue = "2")
        public Integer[] array;
        @ConfigProperty(defaultValue = "3")
        public List<Integer> list;
        @ConfigProperty(defaultValue = "4")
        public Set<Integer> set;
        public Optional<Integer[]> optionalArray;
        public Optional<List<Integer>> optionalList;
        public Optional<Set<Integer>> optionalSet;
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.theHost", "localhost", "server.port", "8080"))
                .withSources(config("cloud.theHost", "cloud", "cloud.port", "9090", "cloud.array", "2,3",
                        "cloud.list", "3,4", "cloud.set", "4,5", "cloud.optionalArray", "2,3",
                        "cloud.optionalList", "3,4", "cloud.optionalSet", "4,5"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }
}
