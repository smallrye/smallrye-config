package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

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
                .withSources(config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678",
                        "mp.config.profile", "prof", "my.prop.profile", "1234", "%prof.my.prop.profile", "5678"))
                .withSources(new ConfigSource() {
                    int counter = 1;

                    @Override
                    public Map<String, String> getProperties() {
                        return new HashMap<>();
                    }

                    @Override
                    public Set<String> getPropertyNames() {
                        return new HashSet<>();
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
                .withSources(config("optional.int.value", "1", "optional.long.value", "2", "optional.double.value", "3.3"))
                .withSources(
                        config("server.host", "localhost", "server.port", "8080", "cloud.host", "cloud", "cloud.port", "9090"))
                .withSecretKeys("secret")
                .withDefaultValue("server.host", "localhost")
                .withDefaultValue("cloud.host", "cloud")
                .withDefaultValue("server.port", "8080")
                .withDefaultValue("cloud.port", "9090")
                .withConverter(ConvertedValue.class, 100, new ConvertedValueConverter())
                .build();
    }

    public static class ConvertedValue {
        private final String value;

        public ConvertedValue(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ConvertedValueConverter implements Converter<ConvertedValue> {
        @Override
        public ConvertedValue convert(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new ConvertedValue("out");
        }
    }
}
