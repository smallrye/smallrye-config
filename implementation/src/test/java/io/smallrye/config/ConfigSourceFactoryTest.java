package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
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
}
