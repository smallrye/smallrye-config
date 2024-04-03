package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertEquals("decoded", config.getRawValue("my.secret"));
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

        assertEquals("decoded", config.getRawValue("my.secret"));
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

        assertEquals("decoded", config.getRawValue("my.secret"));
    }
}
