package io.smallrye.config;

import org.eclipse.microprofile.config.tck.ConfigPropertiesTest;

public class ConfigPropertiesTestFactory extends SmallRyeConfigFactory {
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
                .withValidateUnknown(false)
                .withMapping(ConfigPropertiesTest.BeanThree.class)
                .withMapping(ConfigPropertiesTest.BeanThree.class, "other")
                .build();
    }
}
