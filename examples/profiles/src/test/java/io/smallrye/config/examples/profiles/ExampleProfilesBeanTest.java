package io.smallrye.config.examples.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;

@ExtendWith(WeldJunit5Extension.class)
public class ExampleProfilesBeanTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ExampleProfilesBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    private ExampleProfilesBean bean;

    @Test
    public void profiles() {
        assertEquals("production", bean.getMyProperty());
    }
}
