package io.smallrye.config.source.yaml;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Make sure that if the source is disabled that the property is not available
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class DisabledWhenEnabledKeyIsFalseTest {

    @Inject
    Config config;

    @Deployment
    public static WebArchive createDeployment() {
        return DeployableUnit.create("config-disabled.properties");
    }

    @Test(expected = NoSuchElementException.class)
    public void testPropertyFailsWhenExplicitlyDisabled() {
        config.getValue("test.property", String.class);
    }
}
