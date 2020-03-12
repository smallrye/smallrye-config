package io.smallrye.config;

import org.eclipse.microprofile.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ConfigSourceProfileInterceptorTest {
    @Test
    public void interceptor() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234", "%prod.my.prop", "prod");

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("prod", value);
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new ProfileConfigSourceInterceptor())
                .build();
    }

    private static class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
        private final String profile = "prod";

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            ConfigValue configValue = context.proceed("%" + profile + "." + name);
            if (configValue == null) {
                configValue = context.proceed(name);
            }
            return configValue;
        }
    }
}
