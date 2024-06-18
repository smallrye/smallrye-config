package io.smallrye.config.test.source;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.StreamSupport;

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

import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SysPropConfigSource;

@ExtendWith(ArquillianExtension.class)
class SmallRyeConfigTest {
    @Deployment
    static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("my.prop=1234\n"), "META-INF/microprofile-config.properties")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    void config() {
        assertEquals("1234", config.getValue("my.prop", String.class));
    }

    @Test
    void sources() {
        List<ConfigSource> sources = StreamSupport.stream(config.getConfigSources().spliterator(), false).collect(toList());

        assertEquals(4, sources.size());
        assertTrue(sources.get(0) instanceof SysPropConfigSource);
        assertTrue(sources.get(1) instanceof EnvConfigSource);
        assertTrue(sources.get(2) instanceof PropertiesConfigSource);
        assertTrue(sources.get(3) instanceof DefaultValuesConfigSource);
    }
}
