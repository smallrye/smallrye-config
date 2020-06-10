package io.smallrye.config.examples.expansion;

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
public class ExampleExpansionBeanTest {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ExampleExpansionBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    private ExampleExpansionBean bean;

    @Test
    public void expand() {
        assertEquals("expanded", bean.getMyProperty());
    }
}
