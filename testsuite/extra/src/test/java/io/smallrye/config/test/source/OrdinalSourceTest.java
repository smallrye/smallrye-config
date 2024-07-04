package io.smallrye.config.test.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
class OrdinalSourceTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "ProviderTest.war")
                .addAsResource(new StringAsset("config_ordinal=1234"),
                        "META-INF/microprofile-config.properties")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    void ordinal() {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("microprofile-config.properties")) {
                assertEquals(1234, configSource.getOrdinal());
            }
        }
    }
}
