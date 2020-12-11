package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.inject.InjectionTestConfigFactory.ConvertedValue;

@ExtendWith(WeldJunit5Extension.class)
class ConfigInjectionTest extends InjectionTest {
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
}
