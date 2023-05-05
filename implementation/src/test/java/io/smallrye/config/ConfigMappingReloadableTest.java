package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

class ConfigMappingReloadableTest {
    @Test
    void reloadMapping() {
        Map<String, String> properties = new HashMap<>();
        properties.put("reloadable.reload", "value");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withMapping(ReloadableMapping.class)
                .withSources(new PropertiesConfigSource(properties, "", 0))
                .build();

        ReloadableMapping mapping = config.getConfigMapping(ReloadableMapping.class);
        assertEquals("value", mapping.reload());

        properties.put("reloadable.reload", "reloaded");
        SmallRyeConfig reloadedConfig = new SmallRyeConfigBuilder()
                .withMapping(ReloadableMapping.class)
                .withSources(new ConfigSource() {
                    @Override
                    public Set<String> getPropertyNames() {
                        Set<String> properties = new HashSet<>();
                        config.getPropertyNames().forEach(properties::add);
                        return properties;
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return config.getRawValue(propertyName);
                    }

                    @Override
                    public String getName() {
                        return "Reloadable";
                    }
                }).build();

        ReloadableMapping reloadedMapping = reloadedConfig.getConfigMapping(ReloadableMapping.class);
        assertEquals("reloaded", reloadedMapping.reload());
    }

    @ConfigMapping(prefix = "reloadable")
    interface ReloadableMapping {
        String reload();
    }
}
