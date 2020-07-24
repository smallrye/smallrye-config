package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.InjectionTestConfigFactory.Server;

@ExtendWith(WeldJunit5Extension.class)
public class ConfigMappingInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, Server.class)
            .inject(this)
            .build();

    @Inject
    Server server;

    @Test
    void configMapping() {
        assertNotNull(server);
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }
}
