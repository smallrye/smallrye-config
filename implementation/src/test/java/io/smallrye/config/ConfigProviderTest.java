package io.smallrye.config;

import static io.smallrye.config.SecuritySupport.getContextClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

public class ConfigProviderTest {
    @Test
    void sameConfig() {
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver
                .instance();
        SmallRyeConfigFactory configFactory = configProviderResolver.getFactoryFor(getContextClassLoader(), false);
        Config config = configFactory.getConfigFor(configProviderResolver, getContextClassLoader());
        configProviderResolver.registerConfig(config, getContextClassLoader());

        assertEquals(config, ConfigProvider.getConfig(getContextClassLoader()));
    }
}
