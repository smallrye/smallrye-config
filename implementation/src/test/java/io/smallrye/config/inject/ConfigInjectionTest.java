package io.smallrye.config.inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class ConfigInjectionTest extends InjectionTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigProducer.class, ConfigBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    private ConfigBean configBean;

    @Test
    public void inject() {
        assertEquals("1234", configBean.getMyProp());
        assertEquals("1234", configBean.getExpansion());
        assertEquals("12345678", configBean.getSecret());
        assertThrows(SecurityException.class, () -> configBean.getConfig().getValue("secret", String.class),
                "Not allowed to access secret key secret");

    }

    @ApplicationScoped
    public static class ConfigBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        private String myProp;
        @Inject
        @ConfigProperty(name = "expansion")
        private String expansion;
        @Inject
        @ConfigProperty(name = "secret")
        private String secret;
        @Inject
        private Config config;

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
    }
}
