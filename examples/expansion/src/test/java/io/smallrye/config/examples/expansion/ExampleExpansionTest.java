package io.smallrye.config.examples.expansion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ExampleExpansionTest {
    @AfterEach
    void tearDown() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    @Test
    public void expand() {
        final String myProp = ExampleExpansion.getMyProp();
        assertEquals("expanded", myProp);
    }
}
