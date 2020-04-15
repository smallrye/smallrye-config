package io.smallrye.config.inject;

import io.smallrye.config.KeyValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class ConfigInjectionTestConfigFactory extends SmallRyeConfigFactory {
    @Override
    public SmallRyeConfig getConfigFor(
            final SmallRyeConfigProviderResolver configProviderResolver, final ClassLoader classLoader) {
        return configProviderResolver.getBuilder().forClassLoader(classLoader)
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678"))
                .withSecretKeys("secret")
                .build();
    }
}
