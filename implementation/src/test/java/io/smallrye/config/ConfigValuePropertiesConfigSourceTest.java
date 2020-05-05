package io.smallrye.config;

import org.eclipse.microprofile.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ConfigValuePropertiesConfigSourceTest {
    @Test
    public void interceptor() throws Exception {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig();

        Assert.assertEquals("1", config.getValue("my.prop", String.class));
        Assert.assertEquals("20", config.getValue("my.prop.20", String.class));
    }

    private static Config buildConfig() throws Exception {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(new ConfigValuePropertiesConfigSource(
                        ConfigValuePropertiesConfigSourceTest.class.getResource("/config-values.properties")))
                .withInterceptors((ConfigSourceInterceptor) (context, name) -> {
                    ConfigValue configValue = context.proceed(name);
                    // Return the line number instead for asssert
                    if (configValue != null) {
                        configValue = configValue.withValue(configValue.getLineNumber() + "");
                    }

                    return configValue;
                })
                .build();
    }
}
