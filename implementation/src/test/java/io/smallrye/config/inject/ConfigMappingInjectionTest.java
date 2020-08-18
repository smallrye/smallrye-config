package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ExtendWith(WeldJunit5Extension.class)
public class ConfigMappingInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator
            .from(ConfigExtension.class, ConfigMappingInjectionTest.class, Server.class, Client.class)
            .inject(this)
            .build();

    @Inject
    Server server;
    @Inject
    Client client;
    @Inject
    @ConfigMapping(prefix = "cloud")
    Server cloud;

    @Test
    void configMapping() {
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void discoveredMapping() {
        assertNotNull(client);
        assertEquals("localhost", client.host());
        assertEquals(8080, client.port());
    }

    @Test
    void overridePrefix() {
        assertNotNull(cloud);
        assertEquals("cloud", cloud.host());
        assertEquals(9090, cloud.port());
    }

    @Test
    void select() {
        Server server = CDI.current().select(Server.class).get();
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @ConfigMapping(prefix = "client")
    public interface Server {
        String host();

        int port();
    }

    @ConfigMapping(prefix = "client")
    public interface Client {
        @WithDefault("localhost")
        String host();

        @WithDefault("8080")
        int port();
    }
}
