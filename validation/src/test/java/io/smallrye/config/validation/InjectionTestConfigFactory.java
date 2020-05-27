package io.smallrye.config.validation;

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
                .withValidator(new SmallRyeConfigValidator())
                .build();
    }
}
