package io.smallrye.config.util.injection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class OptionalInjectionTest {
    @Deployment
    public static WebArchive createDeployment() {
        final File[] smallryeConfig = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("io.smallrye.config:smallrye-config")
                .withoutTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "ConfigSourceProviderTest.war")
                .addAsLibraries(smallryeConfig)
                .addAsResource(
                        new StringAsset(
                                "optional.int.value=1\n" +
                                        "optional.long.value=2\n" +
                                        "optional.double.value=3.3"),
                        "META-INF/microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    @ConfigProperty(name = "optional.int.value")
    private OptionalInt optionalInt;

    @Inject
    @ConfigProperty(name = "optional.long.value")
    private OptionalLong optionalLong;

    @Inject
    @ConfigProperty(name = "optional.double.value")
    private OptionalDouble optionalDouble;

    @Test
    public void optionalIntInjection() {
        assertTrue(optionalInt.isPresent());
        assertEquals(1, optionalInt.getAsInt());

        assertTrue(optionalLong.isPresent());
        assertEquals(2, optionalLong.getAsLong());

        assertTrue(optionalDouble.isPresent());
        assertEquals(3.3, optionalDouble.getAsDouble(), 0);
    }
}
