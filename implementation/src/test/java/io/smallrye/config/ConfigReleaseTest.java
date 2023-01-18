package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigReleaseTest {

    private static final Config CONFIG = new SmallRyeConfigBuilder()
            .withSources(config("server.host", "localhost", "server.port", "8080"))
            .build();

    private static final ClassLoader THREAD_LOADER = Thread.currentThread().getContextClassLoader();
    private static final ClassLoader PARENT_LOADER = Thread.currentThread().getContextClassLoader().getParent();

    @BeforeEach
    void beforeEach() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig(THREAD_LOADER));
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig(PARENT_LOADER));
        ConfigProviderResolver.instance().registerConfig(CONFIG, THREAD_LOADER);
        ConfigProviderResolver.instance().registerConfig(CONFIG, PARENT_LOADER);
    }

    @Test
    void releaseWithoutClassloader() {
        Config fromThreadLoader = ConfigProvider.getConfig(THREAD_LOADER);
        Config fromPlatformLoader = ConfigProvider.getConfig(PARENT_LOADER);

        assertEquals("localhost", fromThreadLoader.getValue("server.host", String.class));
        assertEquals("localhost", fromPlatformLoader.getValue("server.host", String.class));

        // this demonstrates the problems highlighted in these issues:
        // https://github.com/eclipse/microprofile-config/issues/136#issuecomment-535962313
        // https://github.com/eclipse/microprofile-config/issues/471
        // When we call release with one of the two class loaders, but they will both be released meaning
        // the current class loader is removing config from another classloader unexpectedly
        ConfigProviderResolver.instance().releaseConfig(fromThreadLoader);

        final Config fromThreadLoader2 = ConfigProvider.getConfig(THREAD_LOADER);
        final Config fromPlatformLoader2 = ConfigProvider.getConfig(PARENT_LOADER);

        assertThrows(NoSuchElementException.class, () -> fromThreadLoader2.getValue("server.host", String.class));
        assertThrows(NoSuchElementException.class, () -> fromPlatformLoader2.getValue("server.host", String.class));
    }

    @Test
    void releaseWithClassLoader() {
        Config fromThreadLoader = ConfigProvider.getConfig(THREAD_LOADER);
        Config fromPlatformLoader = ConfigProvider.getConfig(PARENT_LOADER);

        assertEquals("localhost", fromThreadLoader.getValue("server.host", String.class));
        assertEquals("localhost", fromPlatformLoader.getValue("server.host", String.class));

        // this demonstrates a solution to the problems highlighted in these issues:
        // https://github.com/eclipse/microprofile-config/issues/136#issuecomment-535962313
        // https://github.com/eclipse/microprofile-config/issues/471
        // We can explicitly release the config by class loader, leaving other class loaders untouched
        ((SmallRyeConfigProviderResolver) ConfigProviderResolver.instance()).releaseConfig(
                THREAD_LOADER);

        final Config fromThreadLoader2 = ConfigProvider.getConfig(THREAD_LOADER);
        final Config fromPlatformLoader2 = ConfigProvider.getConfig(PARENT_LOADER);

        assertThrows(NoSuchElementException.class, () -> fromThreadLoader2.getValue("server.host", String.class));
        assertEquals("localhost", fromPlatformLoader2.getValue("server.host", String.class));
    }
}
