package io.smallrye.config.examples.mapping;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class MappingSmallRyeConfigFactory extends SmallRyeConfigFactory {
    @Override
    public SmallRyeConfig getConfigFor(
            final SmallRyeConfigProviderResolver configProviderResolver, final ClassLoader classLoader) {

        return configProviderResolver.getBuilder()
                .forClassLoader(classLoader)
                .addDefaultSources()
                .addDefaultInterceptors()
                .addDiscoveredSources()
                .addDiscoveredConverters()
                .addDiscoveredInterceptors()
                .withMapping(Server.class)
                .build();
    }
}
