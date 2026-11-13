package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class SecretKeysHandlerTest {
    @Test
    void notFound() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${missing::secret}")
                .build();

        assertThrows(NoSuchElementException.class, () -> config.getConfigValue("my.secret"));
    }

    @Test
    void handler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSecretKeysHandlers(new SecretKeysHandler() {
                    @Override
                    public String decode(final String secret) {
                        return "decoded";
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("decoded", secret.getValue());
        assertTrue(secret.isSecret());
    }

    @Test
    void handlerFactory() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValue("context.handler", "decoded")
                .addDefaultInterceptors()
                .withSecretKeyHandlerFactories(new SecretKeysHandlerFactory() {
                    @Override
                    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
                        return new SecretKeysHandler() {
                            @Override
                            public String decode(final String secret) {
                                return context.getValue("context.handler").getValue();
                            }

                            @Override
                            public String getName() {
                                return "handler";
                            }
                        };
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertEquals("decoded", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void discoveredHandler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertEquals("discovered", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void handlerHasPriorityOverDiscoveredHandler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDefaultInterceptors()
                .withSecretKeysHandlers(new SecretKeysHandler() {
                    @Override
                    public String decode(final String secret) {
                        return "decoded";
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertEquals("decoded", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void handlerFactoryHasPriorityOverDiscoveredHandler() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(serviceLoaderClassLoader())
                .addDefaultInterceptors()
                .withSecretKeyHandlerFactories(new SecretKeysHandlerFactory() {
                    @Override
                    public SecretKeysHandler getSecretKeysHandler(final ConfigSourceContext context) {
                        return new SecretKeysHandler() {
                            @Override
                            public String decode(final String secret) {
                                return "decoded";
                            }

                            @Override
                            public String getName() {
                                return "handler";
                            }
                        };
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${handler::secret}")
                .build();

        assertEquals("decoded", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void expression() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSecretKeysHandlers(new SecretKeysHandler() {
                    @Override
                    public String decode(final String secret) {
                        return "decoded";
                    }

                    @Override
                    public String getName() {
                        return "handler";
                    }
                })
                .withDefaultValue("my.secret", "${my.expression}")
                .withDefaultValue("my.expression", "${handler::secret}")
                .build();

        ConfigValue secret = config.getConfigValue("my.secret");
        assertEquals("decoded", secret.getValue());
        assertTrue(secret.isSecret());
    }

    public static class DiscoveredSecretKeysHandler implements SecretKeysHandler {
        @Override
        public String decode(final String secret) {
            return "discovered";
        }

        @Override
        public String getName() {
            return "handler";
        }
    }

    /**
     * A {@link ClassLoader} that registers {@link DiscoveredSecretKeysHandler} as a {@link SecretKeysHandler} service.
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
                                DiscoveredSecretKeysHandler.class.getName().getBytes(StandardCharsets.UTF_8));
                    }
                };
            }
        };

        try {
            URL url = new URL("secret-keys-handler", null, -1, "services", handler);
            return new ClassLoader(Thread.currentThread().getContextClassLoader()) {
                @Override
                public Enumeration<URL> getResources(final String name) {
                    if (name.equals("META-INF/services/" + SecretKeysHandler.class.getName())) {
                        return Collections.enumeration(List.of(url));
                    }
                    return Collections.emptyEnumeration();
                }

                @Override
                public URL getResource(final String name) {
                    if (name.equals("META-INF/services/" + SecretKeysHandler.class.getName())) {
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
