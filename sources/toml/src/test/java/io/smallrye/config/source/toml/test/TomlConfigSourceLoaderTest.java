package io.smallrye.config.source.toml.test;

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
import io.smallrye.config.source.toml.TomlConfigSource;

class TomlConfigSourceLoaderTest {
    @Test
    void applicationToml(@TempDir Path tempDir) throws Exception {
        String toml = """
                [my]
                prop = 1234
                """;
        File file = tempDir.resolve("application.toml").toFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(toml.getBytes());
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(new URLClassLoader(new URL[] { tempDir.toUri().toURL() }))
                .addDiscoveredSources()
                .build();

        assertTrue(config.getConfigSources(TomlConfigSource.class).iterator().hasNext());
        assertEquals("1234", config.getConfigValue("my.prop").getValue());
    }

    @Test
    void microProfileConfigToml(@TempDir Path tempDir) throws Exception {
        String toml = """
                [my]
                prop = 1234
                """;
        File metaInf = tempDir.resolve("META-INF").toFile();
        metaInf.mkdirs();
        File file = new File(metaInf, "microprofile-config.toml");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(toml.getBytes());
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .forClassLoader(new URLClassLoader(new URL[] { tempDir.toUri().toURL() }))
                .addDiscoveredSources()
                .build();

        assertTrue(config.getConfigSources(TomlConfigSource.class).iterator().hasNext());
        assertEquals("1234", config.getConfigValue("my.prop").getValue());
    }
}
