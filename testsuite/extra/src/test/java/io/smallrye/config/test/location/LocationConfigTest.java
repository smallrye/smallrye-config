package io.smallrye.config.test.location;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class LocationConfigTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "LocationConfigTest.war")
                .addAsResource(new StringAsset("smallrye.config.locations=config.properties,config.yml"),
                        "META-INF/microprofile-config.properties")
                .addAsResource(new StringAsset("my.prop=1234"), "config.properties")
                .addAsResource(new StringAsset("my:\n" +
                        "  yml: 1234\n"), "config.yml")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    void locationConfig() {
        assertEquals("config.properties,config.yml", config.getValue("smallrye.config.locations", String.class));
        assertEquals("1234", config.getValue("my.prop", String.class));
        assertEquals("1234", config.getValue("my.yml", String.class));
    }
}
