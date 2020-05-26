package io.smallrye.config.examples.profiles;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.config.inject.ConfigExtension;

public class ExampleProfilesBeanTest {
    @Rule
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
