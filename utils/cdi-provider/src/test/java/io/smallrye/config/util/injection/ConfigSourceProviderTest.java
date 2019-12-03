package io.smallrye.config.util.injection;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing the injection of a Config source name and the Config Source Map
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@RunWith(Arquillian.class)
public class ConfigSourceProviderTest {

    @Inject
    @Name("SysPropConfigSource")
    private ConfigSource systemPropertiesConfigSource;

    @Inject
    @Name("PropertiesConfigSource[source=archive:/WEB-INF/classes/META-INF/microprofile-config.properties]")
    private ConfigSource propertiesConfigSource;

    @Inject
    @ConfigSourceMap
    private Map<String, ConfigSource> configSourceMap;

    @Deployment
    public static WebArchive createDeployment() {
        final File[] smallryeConfig = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("io.smallrye.config:smallrye-config")
                .withoutTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "ConfigSourceProviderTest.war")
                .addPackage(ConfigSourceProvider.class.getPackage())
                .addAsLibraries(smallryeConfig)
                .addAsResource(ConfigSourceProviderTest.class.getClassLoader().getResource("test.properties"),
                        "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testInjectionByName() {
        Assert.assertNotNull(systemPropertiesConfigSource);
        Assert.assertFalse(systemPropertiesConfigSource.getProperties().isEmpty());
    }

    @Test
    public void testInjectOfPropertiesFile() {
        Assert.assertNotNull(propertiesConfigSource);
        Assert.assertFalse(propertiesConfigSource.getProperties().isEmpty());
        Map<String, String> properties = propertiesConfigSource.getProperties();
        Assert.assertNotNull(properties);
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("testvalue", properties.get("testkey"));
    }

    @Test
    public void testInjectionOfMap() {
        Assert.assertNotNull(configSourceMap);
        Assert.assertFalse(configSourceMap.isEmpty());
    }
}
