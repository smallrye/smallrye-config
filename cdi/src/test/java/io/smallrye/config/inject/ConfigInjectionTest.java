package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@ExtendWith(WeldJunit5Extension.class)
class ConfigInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConfigBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    ConfigBean configBean;

    @Test
    void inject() {
        assertEquals("1234", configBean.getMyProp());
        assertEquals("1234", configBean.getExpansion());
        assertEquals("12345678", configBean.getSecret());
        assertEquals("5678", configBean.getMyPropProfile());
        assertThrows(SecurityException.class, () -> configBean.getConfig().getValue("secret", String.class),
                "Not allowed to access secret key secret");
    }

    @Test
    void injectConfigValue() {
        final ConfigValue configValue = configBean.getConfigValue();
        assertNotNull(configValue);
        assertEquals("my.prop", configValue.getName());
        assertEquals("1234", configValue.getValue());
        assertEquals("KeyValuesConfigSource", configValue.getConfigSourceName());
        assertEquals(100, configValue.getConfigSourceOrdinal());

        final ConfigValue configValueMissing = configBean.getConfigValueMissing();
        assertNotNull(configValueMissing);
        assertEquals("my.prop.missing", configValueMissing.getName());
        assertEquals("default", configValueMissing.getValue());
        assertNull(configValueMissing.getConfigSourceName());
    }

    @Test
    void optionals() {
        assertFalse(configBean.getUnknown().isPresent());
    }

    @Test
    void converters() {
        assertFalse(configBean.getConvertedValueOptional().isPresent());
    }

    @ApplicationScoped
    static class ConfigBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        String myProp;
        @Inject
        @ConfigProperty(name = "expansion")
        String expansion;
        @Inject
        @ConfigProperty(name = "secret")
        String secret;
        @Inject
        @ConfigProperty(name = "my.prop.profile")
        String myPropProfile;
        @Inject
        Config config;
        @Inject
        @ConfigProperty(name = "my.prop")
        ConfigValue configValue;
        @Inject
        @ConfigProperty(name = "my.prop.missing", defaultValue = "default")
        ConfigValue configValueMissing;
        @Inject
        @ConfigProperty(name = "unknown")
        Optional<String> unknown;
        @Inject
        @ConfigProperty(name = "converted")
        Optional<ConvertedValue> convertedValueOptional;

        String getMyProp() {
            return myProp;
        }

        String getExpansion() {
            return expansion;
        }

        String getSecret() {
            return secret;
        }

        String getMyPropProfile() {
            return myPropProfile;
        }

        Config getConfig() {
            return config;
        }

        ConfigValue getConfigValue() {
            return configValue;
        }

        ConfigValue getConfigValueMissing() {
            return configValueMissing;
        }

        Optional<String> getUnknown() {
            return unknown;
        }

        Optional<ConvertedValue> getConvertedValueOptional() {
            return convertedValueOptional;
        }
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678",
                        "mp.config.profile", "prof", "my.prop.profile", "1234", "%prof.my.prop.profile", "5678",
                        "bad.property.expression.prop", "${missing.prop}"))
                .withSecretKeys("secret")
                .withConverter(ConvertedValue.class, 100, new ConvertedValueConverter())
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    static class ConvertedValue {
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
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    static class ConvertedValueConverter implements Converter<ConvertedValue> {
        @Override
        public ConvertedValue convert(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new ConvertedValue("out");
        }
    }
}
