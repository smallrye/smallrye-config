package io.smallrye.config.util.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigProducer;

/**
 * Testing the injection of a Config source name and the Config Source Map
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ExtendWith(WeldJunit5Extension.class)
public class ConfigSourceProviderTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class, ConfigSourceProvider.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    @Name("SysPropConfigSource")
    private ConfigSource systemPropertiesConfigSource;

    @Inject
    @Name("PropertiesConfigSource[source=memory]")
    private ConfigSource propertiesConfigSource;

    @Inject
    @ConfigSourceMap
    private Map<String, ConfigSource> configSourceMap;

    @Test
    public void testInjectionByName() {
        assertNotNull(systemPropertiesConfigSource);
        assertFalse(systemPropertiesConfigSource.getProperties().isEmpty());
    }

    @Test
    public void testInjectOfPropertiesFile() {
        assertNotNull(propertiesConfigSource);
        assertFalse(propertiesConfigSource.getProperties().isEmpty());
        Map<String, String> properties = propertiesConfigSource.getProperties();
        assertNotNull(properties);
        assertEquals(1, properties.size());
        assertEquals("testvalue", properties.get("testkey"));
    }

    @Test
    public void testInjectionOfMap() {
        assertNotNull(configSourceMap);
        assertFalse(configSourceMap.isEmpty());
    }
}
