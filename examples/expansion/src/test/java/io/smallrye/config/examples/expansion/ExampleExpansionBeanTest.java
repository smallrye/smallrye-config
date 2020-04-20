package io.smallrye.config.examples.expansion;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.config.inject.ConfigExtension;

public class ExampleExpansionBeanTest {
    @Rule
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
