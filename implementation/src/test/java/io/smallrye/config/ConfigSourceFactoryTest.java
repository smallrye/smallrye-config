package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;

public class ConfigSourceFactoryTest {
    @Test
    void mapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new CountConfigurableConfigSourceFactory())
                .withDefaultValue("count.size", "10")
                .build();

        for (int i = 0; i < 10; i++) {
            assertEquals(i, config.getValue(i + "", int.class));
        }
    }

    @ConfigMapping(prefix = "count")
    interface Count {
        int size();
    }

    static class CountConfigurableConfigSourceFactory implements ConfigurableConfigSourceFactory<Count> {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context, final Count config) {
            Map<String, String> properties = new HashMap<>();
            for (int i = 0; i < config.size(); i++) {
                properties.put(i + "", i + "");
            }
            return Collections.singleton(new PropertiesConfigSource(properties, "", 100));
        }
    }

    @Test
    void expression() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new ExpressionConfigSourceFactory())
                .withSources(config("expression.value", "12${DEFAULT:}"))
                .withSources(new EnvConfigSource(Map.of("DEFAULT", "34"), 100))
                .build();

        assertEquals("1234", config.getRawValue("factory.expression"));
    }

    @ConfigMapping(prefix = "expression")
    interface Expression {
        @WithDefault("${DEFAULT:}")
        String value();
    }

    static class ExpressionConfigSourceFactory implements ConfigSourceFactory {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withMapping(Expression.class)
                    .build();

            Expression mapping = config.getConfigMapping(Expression.class);
            assertEquals("1234", mapping.value());

            return List.of(new PropertiesConfigSource(Map.of("factory.expression", mapping.value()), "", 100));
        }
    }
}
