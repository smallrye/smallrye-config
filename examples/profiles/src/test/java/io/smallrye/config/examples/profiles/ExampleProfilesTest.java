package io.smallrye.config.examples.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ExampleProfilesTest {
    @AfterEach
    void tearDown() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    @Test
    public void profiles() {
        final String myProp = ExampleProfiles.getMyProp();
        assertEquals("production", myProp);
    }
}
