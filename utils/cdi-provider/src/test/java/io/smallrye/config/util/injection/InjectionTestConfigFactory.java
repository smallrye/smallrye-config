package io.smallrye.config.util.injection;

import java.util.HashMap;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigFactory;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class InjectionTestConfigFactory extends SmallRyeConfigFactory {
    @Override
    public SmallRyeConfig getConfigFor(
            final SmallRyeConfigProviderResolver configProviderResolver, final ClassLoader classLoader) {
        return configProviderResolver.getBuilder().forClassLoader(classLoader)
                .addDefaultSources()
                .withSources(new PropertiesConfigSource(new HashMap<String, String>() {
                    {
                        put("testkey", "testvalue");
                    }
                }, "memory", 0))
                .addDefaultInterceptors()
                .build();
    }
}
