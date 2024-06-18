package io.smallrye.config.test.source;

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
class YamlPropertiesTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("my:\n  yaml: 1234\n"), "META-INF/microprofile-config.yaml")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    void yaml() {
        assertEquals("1234", config.getValue("my.yaml", String.class));
    }
}
