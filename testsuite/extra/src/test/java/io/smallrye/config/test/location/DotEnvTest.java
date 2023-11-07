package io.smallrye.config.test.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class DotEnvTest {
    @Test
    void dotEnv(@TempDir Path tempDir) throws Exception {
        Properties dotEnv = new Properties();
        dotEnv.setProperty("FOO_BAR", "value");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env").toFile())) {
            dotEnv.store(out, null);
        }

        String previousUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new DotEnvConfigSourceProvider())
                .build();

        assertEquals("value", config.getRawValue("foo.bar"));

        System.setProperty("user.dir", previousUserDir);
    }

    @Test
    void dotEnvFolder(@TempDir Path tempDir) throws Exception {
        Path dotEnvFolder = tempDir.resolve(".env");
        Files.createDirectories(dotEnvFolder);
        Files.createDirectories(dotEnvFolder.resolve("foo"));

        String previousUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new DotEnvConfigSourceProvider())
                .build();

        assertNull(config.getRawValue("foo.bar"));

        System.setProperty("user.dir", previousUserDir);
    }
}
