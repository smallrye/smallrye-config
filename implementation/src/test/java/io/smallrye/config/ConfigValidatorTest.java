package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;

class ConfigValidatorTest {
    @Test
    void validator() {
        ConfigValidator validator = new RecordingConfigValidator();

        assertSame(validator, new SmallRyeConfigBuilder().withValidator(validator).getValidator());
    }

    @Test
    void noValidator() {
        assertSame(ConfigValidator.EMPTY, new SmallRyeConfigBuilder().getValidator());
    }

    @Test
    void discoveredValidator() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDiscoveredValidator();

        assertInstanceOf(RecordingConfigValidator.class, builder.getValidator());
    }

    @Test
    void noDiscoveredValidator() {
        assertSame(ConfigValidator.EMPTY, new SmallRyeConfigBuilder().addDiscoveredValidator().getValidator());
    }

    @Test
    void validatorHasPriorityOverDiscoveredValidator() {
        ConfigValidator validator = new RecordingConfigValidator();

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .withValidator(validator)
                .addDiscoveredValidator();
        assertSame(validator, builder.getValidator());

        SmallRyeConfigBuilder reverse = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDiscoveredValidator()
                .withValidator(validator);
        assertSame(validator, reverse.getValidator());
    }

    @Test
    void validatorFromCustomizerHasPriorityOverDiscoveredValidator() {
        ConfigValidator validator = new RecordingConfigValidator();

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDiscoveredValidator()
                .withCustomizers(builder -> builder.withValidator(validator))
                .build();

        assertSame(validator, config.getConfigValidator());
    }

    @Test
    void validatorIsNotDiscardedWhenNoValidatorIsDiscovered() {
        ConfigValidator validator = new RecordingConfigValidator();

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(validator)
                .addDiscoveredValidator()
                .withMapping(Server.class)
                .withSources(KeyValuesConfigSource.config("server.host", "localhost"))
                .build();

        assertEquals("localhost", config.getConfigMapping(Server.class).host());
        assertEquals(List.of(Server.class), ((RecordingConfigValidator) validator).validated);
    }

    @ConfigMapping(prefix = "server")
    interface Server {
        String host();
    }

    public static class RecordingConfigValidator implements ConfigValidator {
        final List<Class<?>> validated = new ArrayList<>();

        @Override
        public void validateMapping(final GeneratedConfigClass configClass, final Object configObject)
                throws ConfigValidationException {
            validated.add(configClass.getParent());
        }
    }

    /**
     * A {@link ClassLoader} that registers {@link RecordingConfigValidator} as a {@link ConfigValidator} service.
     */
    private static ClassLoader serviceLoaderClassLoader() {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(
                                RecordingConfigValidator.class.getName().getBytes(StandardCharsets.UTF_8));
                    }
                };
            }
        };

        try {
            URL url = new URL("validator", null, -1, "services", handler);
            return new ClassLoader(Thread.currentThread().getContextClassLoader()) {
                @Override
                public Enumeration<URL> getResources(final String name) {
                    if (name.equals("META-INF/services/" + ConfigValidator.class.getName())) {
                        return Collections.enumeration(List.of(url));
                    }
                    return Collections.emptyEnumeration();
                }

                @Override
                public URL getResource(final String name) {
                    if (name.equals("META-INF/services/" + ConfigValidator.class.getName())) {
                        return url;
                    }
                    return null;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
