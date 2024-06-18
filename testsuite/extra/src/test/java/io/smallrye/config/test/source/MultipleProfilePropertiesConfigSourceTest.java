package io.smallrye.config.test.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
class MultipleProfilePropertiesConfigSourceTest {
    @Deployment
    static WebArchive deploy() {
        JavaArchive sourceOne = ShrinkWrap
                .create(JavaArchive.class)
                .addAsManifestResource(new StringAsset("smallrye.config.profile=prod"), "microprofile-config.properties")
                .addAsManifestResource(new StringAsset("my.prop.one=1234"), "microprofile-config-prod.properties")
                .as(JavaArchive.class);

        JavaArchive sourceTwo = ShrinkWrap
                .create(JavaArchive.class)
                .addAsManifestResource(new StringAsset("my.prop.two=1234"), "microprofile-config-prod.properties")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "sources.war")
                .addAsLibraries(sourceOne, sourceTwo);
    }

    @Inject
    Config config;

    @Test
    void multiple() {
        assertEquals("1234", config.getValue("my.prop.one", String.class));
        assertEquals("1234", config.getValue("my.prop.two", String.class));
    }
}
