package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class YamlConfigSourceLoaderTest {
    @Test
    void applicationYaml(@TempDir Path tempDir) throws Exception {
        String yaml = "my:\n" +
                "  prop: 1234\n";
        File file = tempDir.resolve("application.yaml").toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(yaml.getBytes());
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(new URLClassLoader(new URL[] { tempDir.toUri().toURL() }))
                .addDiscoveredSources()
                .build();

        assertTrue(config.getConfigSources(YamlConfigSource.class).iterator().hasNext());
        assertEquals("1234", config.getRawValue("my.prop"));
    }
}
