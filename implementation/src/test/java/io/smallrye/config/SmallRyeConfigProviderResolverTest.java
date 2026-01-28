package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Test;

class SmallRyeConfigProviderResolverTest {
    @Test
    void get() {
        assertThrows(IllegalArgumentException.class, io.smallrye.config.Config::get);
        ConfigProviderResolver instance = SmallRyeConfigProviderResolver.instance();
        Config config = new SmallRyeConfigBuilder().build();
        instance.registerConfig(config, Thread.currentThread().getContextClassLoader());
        assertEquals(config, io.smallrye.config.Config.get());
        instance.releaseConfig(config);
    }

    @Test
    void getOrCreate() {
        assertNotNull(io.smallrye.config.Config.getOrCreate());
        ConfigProviderResolver instance = SmallRyeConfigProviderResolver.instance();
        Config config = instance.getConfig();
        assertEquals(config, io.smallrye.config.Config.get());
        instance.releaseConfig(config);
    }

    @Test
    void getByClassLoader() {
        ClassLoader classLoader = new ClassLoader() {
        };
        assertThrows(IllegalArgumentException.class, () -> io.smallrye.config.Config.get(classLoader));
        ConfigProviderResolver instance = SmallRyeConfigProviderResolver.instance();
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        instance.registerConfig(config, classLoader);
        assertEquals(config, io.smallrye.config.Config.get(classLoader));
        instance.releaseConfig(config);
    }

    @Test
    void getOrCreateByClassLoader() {
        ClassLoader classLoader = new ClassLoader() {
        };
        assertNotNull(io.smallrye.config.Config.getOrCreate(classLoader));
        ConfigProviderResolver instance = SmallRyeConfigProviderResolver.instance();
        Config config = instance.getConfig(classLoader);
        assertEquals(config, io.smallrye.config.Config.getOrCreate(classLoader));
        instance.releaseConfig(config);
    }
}
