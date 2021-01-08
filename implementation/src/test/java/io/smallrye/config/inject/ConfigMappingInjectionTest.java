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
class ConfigMappingInjectionTest extends InjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator
            .from(ConfigExtension.class, ConfigMappingInjectionTest.class, Server.class, Client.class, ConfigMappingBean.class)
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
        assertEquals("localhost", server.theHost());
        assertEquals(8080, server.port());
    }

    @Test
    void discoveredMapping() {
        assertNotNull(client);
        assertEquals("client", client.host());
        assertEquals(80, client.port());
    }

    @Test
    void overridePrefix() {
        assertNotNull(cloud);
        assertEquals("cloud", cloud.theHost());
        assertEquals(9090, cloud.port());
    }

    @Test
    void select() {
        Server server = CDI.current().select(Server.class).get();
        assertNotNull(server);
        assertEquals("localhost", server.theHost());
        assertEquals(8080, server.port());
    }

    @ConfigMapping(prefix = "server")
    interface Server {
        String theHost();

        int port();
    }

    @ConfigMapping(prefix = "client")
    interface Client {
        @WithDefault("client")
        String host();

        @WithDefault("80")
        int port();
    }

    @Inject
    ConfigMappingBean configMappingBean;

    @Test
    void overridePrefixBean() {
        Server cloud = configMappingBean.getCloud();
        assertEquals("cloud", cloud.theHost());
        assertEquals(9090, cloud.port());

        Server client = configMappingBean.getClient();
        assertEquals("client", client.theHost());
        assertEquals(80, client.port());
    }

    public static class ConfigMappingBean {
        private final Server cloud;
        private Server client;

        @Inject
        public ConfigMappingBean(@ConfigMapping(prefix = "cloud") Server cloud) {
            this.cloud = cloud;
        }

        public Server getCloud() {
            return cloud;
        }

        public Server getClient() {
            return client;
        }

        @Inject
        public void setClient(@ConfigMapping(prefix = "client") final Server client) {
            this.client = client;
        }
    }
}
