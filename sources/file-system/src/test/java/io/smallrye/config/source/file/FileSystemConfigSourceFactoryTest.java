package io.smallrye.config.source.file;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;

class FileSystemConfigSourceFactoryTest {

    @Test
    void testSingleLocation() throws URISyntaxException {
        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();

        URL configDir1URL = this.getClass().getResource("configDir");
        Iterable<ConfigSource> configSources = factory
                .getConfigSources(newConfigSourceContext(configDir1URL.toURI().toString()));
        assertEquals(1, StreamSupport.stream(configSources.spliterator(), false).count());
    }

    @Test
    void testMultipleLocations() throws URISyntaxException {
        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();

        URL configDir1URL = this.getClass().getResource("configDir");
        URL configDir2URL = this.getClass().getResource("configDir2");
        Iterable<ConfigSource> configSources = factory.getConfigSources(
                newConfigSourceContext(configDir1URL.toURI().toString() + "," + configDir2URL.toURI().toString()));
        assertEquals(2, StreamSupport.stream(configSources.spliterator(), false).count());
    }

    @Test
    void testWildcardLocationUri() throws URISyntaxException {
        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();

        URL rootConfigDirURL = this.getClass().getResource(".");
        Iterable<ConfigSource> configSources = factory.getConfigSources(
                newConfigSourceContext(rootConfigDirURL.toURI() + "*/"));
        assertEquals(2, StreamSupport.stream(configSources.spliterator(), false).count());
    }

    @Test
    void testWildcardLocationPath() {
        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();

        String rootConfigDirPath = this.getClass().getResource(".").getPath();
        Iterable<ConfigSource> configSources = factory.getConfigSources(
                newConfigSourceContext(rootConfigDirPath + "*/"));
        assertEquals(2, StreamSupport.stream(configSources.spliterator(), false).count());
    }

    private ConfigSourceContext newConfigSourceContext(String value) {
        return new ConfigSourceContext() {

            @Override
            public Iterator<String> iterateNames() {
                return null;
            }

            @Override
            public ConfigValue getValue(String name) {
                return new ConfigValueBuilder().withValue(value).build();
            }

            @Override
            public List<String> getProfiles() {
                return null;
            }
        };
    }
}
