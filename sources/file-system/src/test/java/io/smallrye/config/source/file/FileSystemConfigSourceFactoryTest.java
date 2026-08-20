package io.smallrye.config.source.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;

class FileSystemConfigSourceFactoryTest {

    @Test
    void testSingleLocation(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("key"), "value");

        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();
        Iterable<ConfigSource> configSources = factory
                .getConfigSources(newConfigSourceContext(tempDir.toUri().toString()));
        assertEquals(1, StreamSupport.stream(configSources.spliterator(), false).count());
    }

    @Test
    void testMultipleLocations(@TempDir Path tempDir) throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectory(dir1);
        Files.createDirectory(dir2);
        Files.writeString(dir1.resolve("key1"), "value1");
        Files.writeString(dir2.resolve("key2"), "value2");

        FileSystemConfigSourceFactory factory = new FileSystemConfigSourceFactory();
        Iterable<ConfigSource> configSources = factory.getConfigSources(
                newConfigSourceContext(dir1.toUri().toString() + "," + dir2.toUri().toString()));
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
        };
    }
}
