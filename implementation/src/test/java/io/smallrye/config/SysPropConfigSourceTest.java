package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SysPropConfigSourceTest {
    @BeforeEach
    void setUp() {
        System.setProperty("config_ordinal", "1000");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config_ordinal");
    }

    @Test
    void ordinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(new SysPropConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertTrue(configSource instanceof SysPropConfigSource);
        assertEquals(configSource.getOrdinal(), 1000);
    }
}
