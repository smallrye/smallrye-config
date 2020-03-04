package io.smallrye.config;

import javax.annotation.Priority;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

public class ConfigSourceInterceptorTest {
    @Test
    public void interceptor() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234", "override", "4567"))
                .withInterceptors(new LoggingConfigSourceInterceptor(),
                        new OverridingConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("4567", value);
    }

    @Test
    public void priority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("higher", value);
    }

    @Test
    public void serviceLoader() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .addDiscoveredInterceptors()
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("loader", value);
    }

    @Test
    public void serviceLoaderAndPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .addDiscoveredInterceptors()
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assert.assertEquals("higher", value);
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

    private static class OverridingConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return context.proceed("override");
        }
    }

    @Priority(300)
    private static class HighPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("higher").build();
        }
    }

    @Priority(200)
    private static class LowerPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("lower").build();
        }
    }

}
