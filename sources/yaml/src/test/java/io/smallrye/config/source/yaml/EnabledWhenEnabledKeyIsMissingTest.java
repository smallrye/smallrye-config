package io.smallrye.config.source.yaml;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Make sure that by default the config source is enabled.
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class EnabledWhenEnabledKeyIsMissingTest {

    @Inject
    Config config;

    @Deployment
    public static WebArchive createDeployment() {
        return DeployableUnit.create();
    }

    @Test
    public void testPropertyLoadsWhenNotExplicitlyEnabled() {
        Assert.assertEquals("test.property in application.properties is set to a-string-value",
                "a-string-value", config.getOptionalValue("test.property", String.class).get());
    }

}
