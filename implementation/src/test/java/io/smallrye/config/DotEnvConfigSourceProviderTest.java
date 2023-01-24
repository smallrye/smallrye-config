package io.smallrye.config;

import static io.smallrye.config.DotEnvConfigSourceProvider.dotEnvSources;
import static io.smallrye.config.SecuritySupport.getContextClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvConfigSourceProviderTest {
    @Test
    void dotEnvSource(@TempDir Path tempDir) throws Exception {
        Properties envProperties = new Properties();
        envProperties.setProperty("MY_PROP", "1234");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env").toFile())) {
            envProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(dotEnvSources(tempDir.resolve(".env").toFile().toURI().toString(), getContextClassLoader()))
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("MY_PROP"));

        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().endsWith(".env]")) {
                assertEquals(295, configSource.getOrdinal());
            }
        }
    }

    @Test
    void dotEnvSourceProfiles(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("MY_PROP_MAIN", "main");
        mainProperties.setProperty("MY_PROP_COMMON", "main");
        mainProperties.setProperty("MY_PROP_PROFILE", "main");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env").toFile())) {
            mainProperties.store(out, null);
        }

        Properties commonProperties = new Properties();
        commonProperties.setProperty("MY_PROP_COMMON", "common");
        commonProperties.setProperty("MY_PROP_PROFILE", "common");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env-common").toFile())) {
            commonProperties.store(out, null);
        }

        Properties devProperties = new Properties();
        devProperties.setProperty("MY_PROP_DEV", "dev");
        devProperties.setProperty("MY_PROP_PROFILE", "dev");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env-dev").toFile())) {
            devProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withProfile("common,dev")
                .withSources(dotEnvSources(tempDir.resolve(".env").toFile().toURI().toString(), getContextClassLoader()))
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("main", config.getRawValue("MY_PROP_MAIN"));
        assertEquals("common", config.getRawValue("my.prop.common"));
        assertEquals("common", config.getRawValue("MY_PROP_COMMON"));
        assertEquals("dev", config.getRawValue("my.prop.profile"));
        assertEquals("dev", config.getRawValue("MY_PROP_PROFILE"));
    }

    @Test
    void dotEnvSourceConvertNames(@TempDir Path tempDir) throws Exception {
        Properties envProperties = new Properties();
        envProperties.setProperty("MY-PROP", "1234");
        envProperties.setProperty("FOO_BAR_BAZ", "1234");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve(".env").toFile())) {
            envProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(dotEnvSources(tempDir.resolve(".env").toFile().toURI().toString(), getContextClassLoader()))
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("1234", config.getRawValue("foo.bar.baz"));
    }
}
