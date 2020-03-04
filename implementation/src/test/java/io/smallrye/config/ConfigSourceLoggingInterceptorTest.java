package io.smallrye.config;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

public class ConfigSourceLoggingInterceptorTest {
    @Test
    public void interceptor() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "1234");

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("1234", value);
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new LoggingConfigSourceInterceptor())
                .build();
    }

    private static class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
        private static final Logger LOG = Logger.getLogger("io.smallrye.config");

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            final ConfigValue configValue = context.proceed(name);

            final String key = configValue.getName();
            final String value = configValue.getValue();
            final String configSource = configValue.getConfigSourceName();

            LOG.infov("The key {0} was loaded from {1} with the value {2}", key, configSource, value);

            return configValue;
        }
    }
}
