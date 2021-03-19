package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
                .withValidateUnknown(false) // TODO - We may want to remove this later.
                .withSources(config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678",
                        "mp.config.profile", "prof", "my.prop.profile", "1234", "%prof.my.prop.profile", "5678",
                        "bad.property.expression.prop", "${missing.prop}"))
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
                .withSources(config("client.the-host", "client"))
                .withSecretKeys("secret")
                .withSources(config("server.host", "localhost", "server.the-host", "localhost", "server.theHost", "localhost"))
                .withSources(config("cloud.host", "cloud", "cloud.the-host", "cloud", "cloud.theHost", "cloud"))
                .withSources(config("server.port", "8080", "cloud.port", "9090"))
                .withSources(config("server.hosts[0]", "localhost", "server.hosts[1]", "config"))
                .withSources(config("indexed.converted[0]", "in"))
                .withSources(config("indexed.override.defaults[0]", "e", "indexed.override.defaults[1]", "f"))
                .withSources(config("indexed.comma", "a,b,c", "indexed.comma[0]", "a", "indexed.comma[1]", "b"))
                .withSources(config("optionals.indexed[0]", "a", "optionals.indexed[1]", "b"))
                .withSources(config("supplier.indexed[0]", "a", "supplier.indexed[1]", "b"))
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

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConvertedValue that = (ConvertedValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
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
