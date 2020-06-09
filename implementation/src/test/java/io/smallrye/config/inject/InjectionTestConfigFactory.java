package io.smallrye.config.inject;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.KeyValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class InjectionTestConfigFactory extends SmallRyeConfigFactory {
    @Override
    public SmallRyeConfig getConfigFor(
            final SmallRyeConfigProviderResolver configProviderResolver, final ClassLoader classLoader) {
        return configProviderResolver.getBuilder().forClassLoader(classLoader)
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678"))
                .withSources(new ConfigSource() {
                    int counter = 1;

                    @Override
                    public Map<String, String> getProperties() {
                        return new HashMap<>();
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return "my.counter".equals(propertyName) ? "" + counter++ : null;
                    }

                    @Override
                    public String getName() {
                        return this.getClass().getName();
                    }
                })
                .withSources(KeyValuesConfigSource.config("optional.int.value", "1", "optional.long.value", "2",
                        "optional.double.value", "3.3"))
                .withSecretKeys("secret")
                .build();
    }
}
